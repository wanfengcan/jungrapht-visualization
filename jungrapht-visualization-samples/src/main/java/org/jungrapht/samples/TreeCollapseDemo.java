package org.jungrapht.samples;
/*
 * Copyright (c) 2003, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/jrtom/jung/blob/master/LICENSE for a description.
 *
 */

import java.awt.*;
import java.util.Set;
import java.util.function.Function;
import javax.swing.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jungrapht.samples.util.ControlHelpers;
import org.jungrapht.samples.util.DemoTreeSupplier;
import org.jungrapht.samples.util.TreeLayoutSelector;
import org.jungrapht.visualization.GraphZoomScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.ModalGraphMouse;
import org.jungrapht.visualization.decorators.EdgeShape;
import org.jungrapht.visualization.decorators.EllipseNodeShapeFunction;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.subLayout.Collapsable;
import org.jungrapht.visualization.subLayout.TreeCollapser;

/**
 * Demonstrates "collapsing"/"expanding" of a tree's subtrees.
 *
 * @author Tom Nelson
 */
@SuppressWarnings("serial")
public class TreeCollapseDemo extends JPanel {

  /** the original graph */
  Graph<Collapsable<?>, Integer> graph;

  /** the visual component and renderer for the graph */
  VisualizationViewer<Collapsable<?>, Integer> vv;

  @SuppressWarnings("unchecked")
  public TreeCollapseDemo() {

    setLayout(new BorderLayout());
    // create a simple graph for the demo
    Graph<String, Integer> generatedGraph = DemoTreeSupplier.createTreeTwo();
    // make a pseudograph with Collapsable node types
    // the graph has to allow self loops and parallel edges in order to
    // be collapsed and expanded without losing edges
    this.graph =
        GraphTypeBuilder.<Collapsable<?>, Integer>forGraphType(
                DefaultGraphType.directedPseudograph())
            .buildGraph();
    // add nodes and edges to the new graph
    for (Integer edge : generatedGraph.edgeSet()) {
      Collapsable<?> source = Collapsable.of(generatedGraph.getEdgeSource(edge));
      Collapsable<?> target = Collapsable.of(generatedGraph.getEdgeTarget(edge));
      this.graph.addVertex(source);
      this.graph.addVertex(target);
      this.graph.addEdge(source, target, edge);
    }

    Dimension viewSize = new Dimension(1200, 600);
    Dimension layoutSize = new Dimension(600, 600);

    vv = new VisualizationViewer<>(graph, layoutSize, viewSize);
    vv.setBackground(Color.white);
    vv.getRenderContext().setEdgeShapeFunction(EdgeShape.line());
    vv.getRenderContext().setNodeLabelFunction(Object::toString);
    vv.getRenderContext().setNodeShapeFunction(new ClusterNodeShapeFunction());
    // add a listener for ToolTips
    vv.setNodeToolTipFunction(Object::toString);
    vv.getRenderContext().setArrowFillPaintFunction(n -> Color.lightGray);

    final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
    add(panel);

    final DefaultModalGraphMouse<String, Integer> graphMouse = new DefaultModalGraphMouse<>();
    vv.setGraphMouse(graphMouse);
    vv.addKeyListener(graphMouse.getModeKeyListener());

    JComboBox<?> modeBox = graphMouse.getModeComboBox();
    modeBox.addItemListener(graphMouse.getModeListener());
    graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);

    JButton collapse = new JButton("Collapse");
    collapse.addActionListener(
        e -> {
          Set<Collapsable<?>> picked = vv.getSelectedNodeState().getSelected();
          if (picked.size() == 1) {
            Collapsable<?> root = picked.iterator().next();
            Graph<Collapsable<?>, Integer> subTree = TreeCollapser.collapse(graph, root);
            LayoutModel<Collapsable<?>> objectLayoutModel = vv.getModel().getLayoutModel();
            objectLayoutModel.set(Collapsable.of(subTree), objectLayoutModel.apply(root));
            vv.getModel().setNetwork(graph, true);
            vv.getSelectedNodeState().clear();
            vv.repaint();
          }
        });

    JButton expand = new JButton("Expand");
    expand.addActionListener(
        e -> {
          for (Collapsable<?> v : vv.getSelectedNodeState().getSelected()) {
            if (v.get() instanceof Graph) {
              graph = TreeCollapser.expand(graph, (Collapsable<Graph>) v);
              LayoutModel<Collapsable<?>> objectLayoutModel = vv.getModel().getLayoutModel();
              objectLayoutModel.set(Collapsable.of(graph), objectLayoutModel.apply(v));
              vv.getModel().setNetwork(graph, true);
            }
            vv.getSelectedNodeState().clear();
            vv.repaint();
          }
        });

    JPanel controls = new JPanel();
    controls.add(new TreeLayoutSelector<>(vv, 0));

    controls.add(ControlHelpers.getZoomControls(vv, "Zoom"));
    controls.add(modeBox);
    controls.add(collapse);
    controls.add(expand);
    add(controls, BorderLayout.SOUTH);
  }

  /**
   * a demo class that will create a node shape that is either a polygon or star. The number of
   * sides corresponds to the number of nodes that were collapsed into the node represented by this
   * shape.
   */
  class ClusterNodeShapeFunction extends EllipseNodeShapeFunction<Collapsable<?>> {

    ClusterNodeShapeFunction() {
      setSizeTransformer(new ClusterNodeSizeFunction(20));
    }

    @Override
    public Shape apply(Collapsable<?> v) {
      if (v.get() instanceof Graph) {
        @SuppressWarnings("rawtypes")
        int size = ((Graph) v.get()).vertexSet().size();
        if (size < 8) {
          int sides = Math.max(size, 3);
          return factory.getRegularPolygon(v, sides);
        } else {
          return factory.getRegularStar(v, size);
        }
      }
      return super.apply(v);
    }
  }

  /**
   * A demo class that will make nodes larger if they represent a collapsed collection of original
   * nodes
   */
  class ClusterNodeSizeFunction implements Function<Collapsable<?>, Integer> {
    int size;

    public ClusterNodeSizeFunction(Integer size) {
      this.size = size;
    }

    public Integer apply(Collapsable<?> v) {
      if (v.get() instanceof Graph) {
        return 30;
      }
      return size;
    }
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    Container content = frame.getContentPane();
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    content.add(new TreeCollapseDemo());
    frame.pack();
    frame.setVisible(true);
  }
}