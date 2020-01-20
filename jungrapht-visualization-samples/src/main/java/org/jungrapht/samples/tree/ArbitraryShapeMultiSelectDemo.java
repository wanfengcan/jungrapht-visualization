/*
 * Copyright (c) 2003, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/tomnelson/jungrapht-visualization/blob/master/LICENSE for a description.
 *
 */
package org.jungrapht.samples.tree;

import java.awt.*;
import javax.swing.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.DefaultGraphMouse;
import org.jungrapht.visualization.control.MultiSelectionStrategy;
import org.jungrapht.visualization.decorators.EdgeShape;
import org.jungrapht.visualization.layout.algorithms.StaticLayoutAlgorithm;
import org.jungrapht.visualization.renderers.Renderer;
import org.jungrapht.visualization.util.helpers.ControlHelpers;
import org.jungrapht.visualization.util.helpers.TreeLayoutSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates mulit-selection with arbitrary containing shape instead of a rectangle.<br>
 * CTRL-click and drag to trace a shape containing vertices to select.
 *
 * @author Tom Nelson
 */
public class ArbitraryShapeMultiSelectDemo extends JPanel {

  private static final Logger log = LoggerFactory.getLogger(ArbitraryShapeMultiSelectDemo.class);

  public ArbitraryShapeMultiSelectDemo() {

    setLayout(new BorderLayout());

    Graph<String, Integer> graph = createDAG();

    final DefaultGraphMouse<String, Integer> graphMouse = new DefaultGraphMouse<>();
    graphMouse.setMultiSelectionStrategy(MultiSelectionStrategy.arbitrary());

    VisualizationViewer<String, Integer> vv =
        VisualizationViewer.builder(graph)
            .layoutAlgorithm(new StaticLayoutAlgorithm<>())
            .viewSize(new Dimension(600, 600))
            .graphMouse(graphMouse)
            .build();

    vv.getRenderContext().setEdgeShapeFunction(EdgeShape.line());
    vv.getRenderContext().setVertexLabelFunction(Object::toString);

    vv.setVertexToolTipFunction(Object::toString);
    vv.getRenderContext().setArrowFillPaintFunction(n -> Color.lightGray);

    vv.getRenderContext().setVertexLabelPosition(Renderer.VertexLabel.Position.CNTR);
    vv.getRenderContext().setVertexLabelDrawPaintFunction(c -> Color.white);

    final VisualizationScrollPane panel = new VisualizationScrollPane(vv);
    add(panel);

    JPanel layoutPanel = new JPanel(new GridLayout(0, 1));
    layoutPanel.add(
        ControlHelpers.getCenteredContainer(
            "Layouts", TreeLayoutSelector.builder(vv).after(vv::scaleToLayout).build()));
    Box controls = Box.createHorizontalBox();
    controls.add(layoutPanel);
    controls.add(ControlHelpers.getCenteredContainer("Zoom", ControlHelpers.getZoomControls(vv)));
    add(controls, BorderLayout.SOUTH);
  }

  private Graph<String, Integer> createDAG() {
    Graph<String, Integer> graph =
        GraphTypeBuilder.<String, Integer>forGraphType(DefaultGraphType.dag()).buildGraph();
    Integer i = 0;
    // roots
    graph.addVertex("R1");
    graph.addVertex("R2");
    graph.addVertex("R3");
    graph.addVertex("R4");

    graph.addVertex("A1");
    graph.addVertex("A2");
    graph.addVertex("A3");
    graph.addVertex("A4");
    graph.addVertex("A5");
    graph.addVertex("A6");

    graph.addEdge("R1", "A1", i++);
    graph.addEdge("R1", "A2", i++);
    graph.addEdge("A1", "A3", i++);
    graph.addEdge("A1", "A4", i++);

    graph.addEdge("A4", "A3", i++);
    graph.addEdge("A3", "A4", i++);

    graph.addEdge("R2", "A5", i++);
    graph.addEdge("R3", "A5", i++);
    graph.addEdge("A5", "A6", i++);
    //    graph.addEdge("R1","A1", i++);
    return graph;
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    Container content = frame.getContentPane();
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    content.add(new ArbitraryShapeMultiSelectDemo());
    frame.pack();
    frame.setVisible(true);
  }
}