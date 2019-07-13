/*
 * Copyright (c) 2003, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/jrtom/jung/blob/master/LICENSE for a description.
 *
 */
package org.jungrapht.samples;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import org.jgrapht.Graph;
import org.jungrapht.samples.util.ControlHelpers;
import org.jungrapht.samples.util.DemoTreeSupplier;
import org.jungrapht.visualization.BaseVisualizationModel;
import org.jungrapht.visualization.GraphZoomScrollPane;
import org.jungrapht.visualization.MultiLayerTransformer.Layer;
import org.jungrapht.visualization.VisualizationModel;
import org.jungrapht.visualization.VisualizationServer;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.ModalLensGraphMouse;
import org.jungrapht.visualization.decorators.EdgeShape;
import org.jungrapht.visualization.decorators.PickableElementPaintFunction;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithmTransition;
import org.jungrapht.visualization.layout.algorithms.RadialTreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.TreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.layout.model.PolarPoint;
import org.jungrapht.visualization.selection.MutableSelectedState;
import org.jungrapht.visualization.transform.HyperbolicTransformer;
import org.jungrapht.visualization.transform.LayoutLensSupport;
import org.jungrapht.visualization.transform.Lens;
import org.jungrapht.visualization.transform.LensSupport;
import org.jungrapht.visualization.transform.shape.HyperbolicShapeTransformer;
import org.jungrapht.visualization.transform.shape.ViewLensSupport;

/**
 * Shows a RadialTreeLayout view of a Forest. A hyperbolic projection lens may also be applied to
 * the view
 *
 * @author Tom Nelson
 */
@SuppressWarnings("serial")
public class RadialTreeLensDemo extends JPanel {

  Graph<String, Integer> graph;

  VisualizationServer.Paintable rings;

  TreeLayoutAlgorithm<String> treeLayoutAlgorithm;

  RadialTreeLayoutAlgorithm<String> radialLayoutAlgorithm;

  /** the visual component and renderer for the graph */
  VisualizationViewer<String, Integer> vv;

  /** provides a Hyperbolic lens for the view */
  LensSupport hyperbolicViewSupport;

  LensSupport hyperbolicLayoutSupport;

  /** create an instance of a simple graph with controls to demo the zoomand hyperbolic features. */
  public RadialTreeLensDemo() {

    setLayout(new BorderLayout());
    // create a simple graph for the demo
    graph = DemoTreeSupplier.createTreeTwo();

    radialLayoutAlgorithm = RadialTreeLayoutAlgorithm.builder().build();
    treeLayoutAlgorithm = TreeLayoutAlgorithm.builder().build();

    Dimension preferredSize = new Dimension(600, 600);

    final VisualizationModel<String, Integer> visualizationModel =
        new BaseVisualizationModel<>(graph, radialLayoutAlgorithm, preferredSize);
    vv = new VisualizationViewer<>(visualizationModel, preferredSize);

    MutableSelectedState<String> ps = vv.getSelectedNodeState();
    MutableSelectedState<Integer> pes = vv.getSelectedEdgeState();
    vv.getRenderContext()
        .setNodeFillPaintFunction(new PickableElementPaintFunction<>(ps, Color.red, Color.yellow));
    vv.getRenderContext().setNodeLabelFunction(Object::toString);
    vv.getRenderContext()
        .setEdgeDrawPaintFunction(new PickableElementPaintFunction<>(pes, Color.black, Color.cyan));
    vv.setBackground(Color.white);

    vv.getRenderContext().setNodeLabelFunction(Object::toString);
    vv.getRenderContext().setEdgeShapeFunction(EdgeShape.line());

    // add a listener for ToolTips
    vv.setNodeToolTipFunction(Object::toString);

    GraphZoomScrollPane gzsp = new GraphZoomScrollPane(vv);
    add(gzsp);

    final DefaultModalGraphMouse<String, Integer> graphMouse = new DefaultModalGraphMouse<>();

    vv.setGraphMouse(graphMouse);
    vv.addKeyListener(graphMouse.getModeKeyListener());
    rings = new Rings(vv.getModel().getLayoutModel());
    vv.addPreRenderPaintable(rings);

    JToggleButton radial = new JToggleButton("Tree");
    final JRadioButton animateTransition = new JRadioButton("Animate Transition");

    radial.addItemListener(
        e -> {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            ((JToggleButton) e.getSource()).setText("Radial");
            if (animateTransition.isSelected()) {
              LayoutAlgorithmTransition.animate(vv, treeLayoutAlgorithm);
            } else {
              LayoutAlgorithmTransition.apply(vv, treeLayoutAlgorithm);
            }

            vv.getRenderContext()
                .getMultiLayerTransformer()
                .getTransformer(Layer.LAYOUT)
                .setToIdentity();
            vv.removePreRenderPaintable(rings);

          } else {
            ((JToggleButton) e.getSource()).setText("Tree");
            if (animateTransition.isSelected()) {
              LayoutAlgorithmTransition.animate(vv, radialLayoutAlgorithm);
            } else {
              LayoutAlgorithmTransition.apply(vv, radialLayoutAlgorithm);
            }

            vv.getRenderContext()
                .getMultiLayerTransformer()
                .getTransformer(Layer.LAYOUT)
                .setToIdentity();
            vv.addPreRenderPaintable(rings);
          }
          vv.repaint();
        });
    LayoutModel<String> layoutModel = vv.getModel().getLayoutModel();
    Dimension d = new Dimension(layoutModel.getWidth(), layoutModel.getHeight());

    Lens lens = new Lens(d);
    hyperbolicViewSupport =
        new ViewLensSupport<>(
            vv,
            new HyperbolicShapeTransformer(
                lens, vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW)),
            new ModalLensGraphMouse());
    hyperbolicLayoutSupport =
        new LayoutLensSupport<>(
            vv,
            new HyperbolicTransformer(
                lens,
                vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT)),
            new ModalLensGraphMouse());

    final JRadioButton hyperView = new JRadioButton("Hyperbolic View");
    hyperView.addItemListener(
        e -> hyperbolicViewSupport.activate(e.getStateChange() == ItemEvent.SELECTED));
    final JRadioButton hyperLayout = new JRadioButton("Hyperbolic Layout");
    hyperLayout.addItemListener(
        e -> hyperbolicLayoutSupport.activate(e.getStateChange() == ItemEvent.SELECTED));
    final JRadioButton noLens = new JRadioButton("No Lens");
    noLens.setSelected(true);

    ButtonGroup radio = new ButtonGroup();
    radio.add(hyperView);
    radio.add(hyperLayout);
    radio.add(noLens);

    graphMouse.addItemListener(hyperbolicViewSupport.getGraphMouse().getModeListener());
    graphMouse.addItemListener(hyperbolicLayoutSupport.getGraphMouse().getModeListener());

    JMenuBar menubar = new JMenuBar();
    menubar.add(graphMouse.getModeMenu());
    gzsp.setCorner(menubar);

    JPanel controls = new JPanel(new GridLayout(1, 0));
    JPanel hyperControls = new JPanel(new GridLayout(3, 2));
    hyperControls.setBorder(BorderFactory.createTitledBorder("Examiner Lens"));
    JPanel modeControls = new JPanel(new BorderLayout());
    modeControls.setBorder(BorderFactory.createTitledBorder("Mouse Mode"));
    modeControls.add(graphMouse.getModeComboBox());
    hyperControls.add(hyperView);
    hyperControls.add(hyperLayout);
    hyperControls.add(noLens);

    controls.add(ControlHelpers.getZoomControls(vv, "Zoom"));
    controls.add(hyperControls);
    controls.add(modeControls);
    JPanel layoutControls = new JPanel(new GridLayout(0, 1));
    layoutControls.setBorder(BorderFactory.createTitledBorder("Layouts"));
    JPanel radialPanel = new JPanel();
    radialPanel.add(radial);
    layoutControls.add(radialPanel);
    layoutControls.add(animateTransition);
    controls.add(layoutControls);
    add(controls, BorderLayout.SOUTH);
  }

  class Rings implements VisualizationServer.Paintable {

    Collection<Double> depths;
    LayoutModel<String> layoutModel;

    public Rings(LayoutModel<String> layoutModel) {
      this.layoutModel = layoutModel;
      depths = getDepths();
    }

    private Collection<Double> getDepths() {
      Set<Double> depths = new HashSet<>();
      Map<String, PolarPoint> polarLocations = radialLayoutAlgorithm.getPolarLocations();
      for (String v : graph.vertexSet()) {
        PolarPoint pp = polarLocations.get(v);
        depths.add(pp.radius);
      }
      return depths;
    }

    public void paint(Graphics g) {
      g.setColor(Color.gray);
      Graphics2D g2d = (Graphics2D) g;
      Point center = radialLayoutAlgorithm.getCenter(layoutModel);

      Ellipse2D ellipse = new Ellipse2D.Double();
      for (double d : depths) {
        ellipse.setFrameFromDiagonal(center.x - d, center.y - d, center.x + d, center.y + d);
        Shape shape = vv.getTransformSupport().transform(vv, ellipse, Layer.LAYOUT);
        g2d.draw(shape);
      }
    }

    public boolean useTransform() {
      return true;
    }
  }

  public static void main(String[] args) {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    f.getContentPane().add(new RadialTreeLensDemo());
    f.pack();
    f.setVisible(true);
  }
}
