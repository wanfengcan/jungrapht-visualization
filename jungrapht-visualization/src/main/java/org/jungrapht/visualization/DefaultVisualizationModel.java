/*
 * Copyright (c) 2003, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/tomnelson/jungrapht-visualization/blob/master/LICENSE for a description.
 *
 * Created on Jul 7, 2003
 *
 */
package org.jungrapht.visualization;

import com.google.common.base.Preconditions;
import java.awt.Dimension;
import java.util.function.Function;
import org.jgrapht.Graph;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.event.ModelChange;
import org.jungrapht.visualization.layout.event.ViewChange;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.LoadingCacheLayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.layout.util.RandomLocationTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Tom Nelson */
class DefaultVisualizationModel<V, E> implements VisualizationModel<V, E> {

  private static final Logger log = LoggerFactory.getLogger(DefaultVisualizationModel.class);

  protected DefaultVisualizationModel(Builder<V, E, ?, ?> builder) {
    this(
        builder.graph,
        builder.layoutAlgorithm,
        builder.layoutModel,
        builder.layoutSize,
        builder.initializer);
  }

  private DefaultVisualizationModel(
      Graph<V, E> graph,
      LayoutAlgorithm<V> layoutAlgorithm,
      LayoutModel<V> layoutModel,
      Dimension layoutSize,
      Function<V, Point> initializer) {
    if (layoutModel == null) {
      // I must have both a graph and a layoutSize to create a layoutModel
      Preconditions.checkNotNull(graph);
      Preconditions.checkNotNull(layoutSize);
      Preconditions.checkArgument(layoutSize.width > 0, "width must be > 0");
      Preconditions.checkArgument(layoutSize.height > 0, "height must be > 0");
      if (initializer == null) {
        initializer =
            new RandomLocationTransformer<>(
                layoutSize.width, layoutSize.height, System.currentTimeMillis());
      }
      layoutModel =
          LoadingCacheLayoutModel.<V>builder()
              .graph(graph)
              .size(layoutSize.width, layoutSize.height)
              .initializer(initializer)
              .build();
    }
    setLayoutModel(layoutModel); // will hook up events
    this.layoutModel.accept(layoutAlgorithm);
    this.layoutAlgorithm = layoutAlgorithm;
  }

  /**
   * copy constructor
   *
   * @param other the {@code DefaultVisualizationModel} to copy
   */
  protected DefaultVisualizationModel(VisualizationModel<V, E> other) {
    this(
        other.getGraph(),
        other.getLayoutAlgorithm(),
        other.getLayoutModel(),
        other.getLayoutSize(),
        null);
  }

  protected LayoutModel<V> layoutModel;

  protected LayoutAlgorithm<V> layoutAlgorithm;

  protected ModelChange.Support modelChangeSupport = ModelChange.Support.create();

  protected ViewChange.Support viewChangeSupport = ViewChange.Support.create();

  @Override
  public LayoutModel<V> getLayoutModel() {
    log.trace("getting a layourModel " + layoutModel);
    return layoutModel;
  }

  @Override
  public void setLayoutModel(LayoutModel<V> layoutModel) {
    // stop any Relaxer threads before abandoning the previous LayoutModel
    if (this.layoutModel != null) {
      this.layoutModel.stopRelaxer();
      this.layoutModel.getModelChangeSupport().getModelChangeListeners().remove(this);
      this.layoutModel.getViewChangeSupport().getViewChangeListeners().remove(this);
    }
    this.layoutModel = layoutModel;
    this.layoutModel.getModelChangeSupport().addModelChangeListener(this);
    this.layoutModel.getViewChangeSupport().addViewChangeListener(this);
    if (layoutAlgorithm != null) {
      layoutModel.accept(layoutAlgorithm);
    }
  }

  @Override
  public void setLayoutAlgorithm(LayoutAlgorithm<V> layoutAlgorithm) {
    this.layoutAlgorithm = layoutAlgorithm;
    log.trace("setLayoutAlgorithm to " + layoutAlgorithm);
    layoutModel.accept(layoutAlgorithm);
  }

  /**
   * Returns the current layoutSize of the visualization space, accoring to the last call to
   * resize().
   *
   * @return the current layoutSize of the screen
   */
  @Override
  public Dimension getLayoutSize() {
    return new Dimension(layoutModel.getWidth(), layoutModel.getHeight());
  }

  @Override
  public void setGraph(Graph<V, E> graph) {
    this.setGraph(graph, true);
  }

  @Override
  public void setGraph(Graph<V, E> graph, boolean forceUpdate) {
    log.info("setGraph to n:{} e:{}", graph.vertexSet(), graph.edgeSet());
    this.layoutModel.setGraph(graph);
    if (forceUpdate && this.layoutAlgorithm != null) {
      log.trace("will accept {}", layoutAlgorithm);
      layoutModel.accept(this.layoutAlgorithm);
      log.trace("will fire fireModelChanged");
      modelChangeSupport.fireModelChanged();
      log.trace("fired fireModelChanged");
    }
  }

  @Override
  public LayoutAlgorithm<V> getLayoutAlgorithm() {
    return layoutAlgorithm;
  }

  @Override
  public Graph<V, E> getGraph() {
    return this.layoutModel.getGraph();
  }

  @Override
  public ModelChange.Support getModelChangeSupport() {
    return this.modelChangeSupport;
  }

  @Override
  public ViewChange.Support getViewChangeSupport() {
    return this.viewChangeSupport;
  }

  @Override
  public void viewChanged() {
    getViewChangeSupport().fireViewChanged();
  }

  @Override
  public void modelChanged() {
    getModelChangeSupport().fireModelChanged();
  }
}
