/*
 * Copyright (c) 2005, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/tomnelson/jungrapht-visualization/blob/master/LICENSE for a description.
 *
 * Created on Aug 15, 2005
 */

package org.jungrapht.visualization.control;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import org.jungrapht.visualization.MultiLayerTransformer;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.spatial.Spatial;
import org.jungrapht.visualization.transform.MutableAffineTransformer;
import org.jungrapht.visualization.transform.shape.GraphicsDecorator;
import org.jungrapht.visualization.transform.shape.ShapeTransformer;

/**
 * A VisualizationViewer that can act as a satellite view for another (master) VisualizationViewer.
 * In this view, the full graph is always visible and all mouse actions affect the graph in the
 * master view.
 *
 * <p>A rectangular shape in the satellite view shows the visible bounds of the master view.
 *
 * @author Tom Nelson
 */
@SuppressWarnings("serial")
public class SatelliteVisualizationViewer<V, E> extends VisualizationViewer<V, E> {

  public static class Builder<
          V, E, T extends SatelliteVisualizationViewer<V, E>, B extends Builder<V, E, T, B>>
      extends VisualizationViewer.Builder<V, E, T, B> {

    protected VisualizationViewer<V, E> master;

    protected Builder(VisualizationViewer<V, E> master) {
      super(master.getModel());
      this.master = master;
    }

    public T build() {
      super.build();
      return (T) new SatelliteVisualizationViewer<>(master, viewSize);
    }
  }

  public static <V, E> Builder<V, E, ?, ?> builder(VisualizationViewer<V, E> master) {
    return new Builder(master);
  }

  //  protected SatelliteVisualizationViewer(Builder<V, E, ?, ?> builder) {
  //    this(builder.master, builder.viewSize);
  //  }

  /** the master VisualizationViewer that this is a satellite view for */
  protected VisualizationViewer<V, E> master;

  /**
   * @param master the master VisualizationViewer for which this is a satellite view
   * @param preferredSize the specified layoutSize of the component
   */
  protected SatelliteVisualizationViewer(
      VisualizationViewer<V, E> master, Dimension preferredSize) {
    super(master.getModel(), preferredSize);
    this.master = master;

    // create a graph mouse with custom plugins to affect the master view
    ModalGraphMouse gm = new ModalSatelliteGraphMouse();
    setGraphMouse(gm);

    // this adds the Lens to the satellite view
    addPreRenderPaintable(new ViewLens<>(this, master));

    // get a copy of the current layout transform
    // it may have been scaled to fit the graph
    AffineTransform modelLayoutTransform =
        new AffineTransform(
            master
                .getRenderContext()
                .getMultiLayerTransformer()
                .getTransformer(MultiLayerTransformer.Layer.LAYOUT)
                .getTransform());

    // I want no layout transformations in the satellite view
    // this resets the auto-scaling that occurs in the super constructor
    getRenderContext()
        .getMultiLayerTransformer()
        .setTransformer(
            MultiLayerTransformer.Layer.LAYOUT, new MutableAffineTransformer(modelLayoutTransform));

    // make sure the satellite listens for changes in the master
    master.addChangeListener(this);

    // share the picked state of the master
    setSelectedVertexState(master.getSelectedVertexState());
    setSelectedEdgeState(master.getSelectedEdgeState());
    setVertexSpatial(new Spatial.NoOp.Vertex(model.getLayoutModel()));
    setEdgeSpatial(new Spatial.NoOp.Edge(model));
  }

  /**
   * override to not use the spatial data structure, as this view will always show the entire graph
   *
   * @param g2d
   */
  @Override
  protected void renderGraph(Graphics2D g2d) {
    if (renderContext.getGraphicsContext() == null) {
      renderContext.setGraphicsContext(new GraphicsDecorator(g2d));
    } else {
      renderContext.getGraphicsContext().setDelegate(g2d);
    }
    renderContext.setScreenDevice(this);
    LayoutModel<V> layoutModel = getModel().getLayoutModel();

    g2d.setRenderingHints(renderingHints);

    // the layoutSize of the VisualizationViewer
    Dimension d = getSize();

    // clear the offscreen image
    g2d.setColor(getBackground());
    g2d.fillRect(0, 0, d.width, d.height);

    AffineTransform oldXform = g2d.getTransform();
    AffineTransform newXform = new AffineTransform(oldXform);
    newXform.concatenate(
        renderContext
            .getMultiLayerTransformer()
            .getTransformer(MultiLayerTransformer.Layer.VIEW)
            .getTransform());

    g2d.setTransform(newXform);

    // if there are  preRenderers set, paint them
    for (Paintable paintable : preRenderers) {

      if (paintable.useTransform()) {
        paintable.paint(g2d);
      } else {
        g2d.setTransform(oldXform);
        paintable.paint(g2d);
        g2d.setTransform(newXform);
      }
    }

    renderer.render(renderContext, model);

    // if there are postRenderers set, do it
    for (Paintable paintable : postRenderers) {

      if (paintable.useTransform()) {
        paintable.paint(g2d);
      } else {
        g2d.setTransform(oldXform);
        paintable.paint(g2d);
        g2d.setTransform(newXform);
      }
    }
    g2d.setTransform(oldXform);
  }

  /** @return Returns the master. */
  public VisualizationViewer<V, E> getMaster() {
    return master;
  }

  /**
   * A four-sided shape that represents the visible part of the master view and is drawn in the
   * satellite view
   *
   * @author Tom Nelson
   */
  static class ViewLens<V, E> implements Paintable {

    VisualizationViewer<V, E> master;
    VisualizationViewer<V, E> vv;

    public ViewLens(VisualizationViewer<V, E> vv, VisualizationViewer<V, E> master) {
      this.vv = vv;
      this.master = master;
    }

    public void paint(Graphics g) {
      ShapeTransformer masterViewTransformer =
          master
              .getRenderContext()
              .getMultiLayerTransformer()
              .getTransformer(MultiLayerTransformer.Layer.VIEW);
      ShapeTransformer masterLayoutTransformer =
          master
              .getRenderContext()
              .getMultiLayerTransformer()
              .getTransformer(MultiLayerTransformer.Layer.LAYOUT);
      ShapeTransformer vvLayoutTransformer =
          vv.getRenderContext()
              .getMultiLayerTransformer()
              .getTransformer(MultiLayerTransformer.Layer.LAYOUT);

      Shape lens = master.getBounds();
      lens = masterViewTransformer.inverseTransform(lens);
      lens = masterLayoutTransformer.inverseTransform(lens);
      lens = vvLayoutTransformer.transform(lens);
      Graphics2D g2d = (Graphics2D) g;
      Color old = g.getColor();
      Color lensColor = master.getBackground();
      Color darker =
          new Color(
              Math.max((int) (lensColor.getRed() * .95), 0),
              Math.max((int) (lensColor.getGreen() * .95), 0),
              Math.max((int) (lensColor.getBlue() * .95), 0),
              lensColor.getAlpha());
      vv.setBackground(darker);
      g.setColor(lensColor);
      g2d.fill(lens);
      g.setColor(Color.gray);
      g2d.draw(lens);
      g.setColor(old);
    }

    public boolean useTransform() {
      return true;
    }
  }
}
