/*
 * Copyright (c) 2003, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/jrtom/jung/blob/master/LICENSE for a description.
 */
package org.jungrapht.samples;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.*;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jungrapht.samples.util.BalloonLayoutRings;
import org.jungrapht.samples.util.LayoutHelper;
import org.jungrapht.samples.util.RadialLayoutRings;
import org.jungrapht.samples.util.SpanningTreeAdapter;
import org.jungrapht.samples.util.TestGraphs;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.CrossoverScalingControl;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.ScalingControl;
import org.jungrapht.visualization.layout.algorithms.BalloonLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithmTransition;
import org.jungrapht.visualization.layout.algorithms.RadialTreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.StaticLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.TreeLayoutAlgorithm;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.LoadingCacheLayoutModel;

/**
 * Demonstrates several of the graph layout algorithms. Allows the user to interactively select one
 * of several graphs, and one of several layouts, and visualizes the combination.
 *
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * @author Tom Nelson - extensive modification
 */
@SuppressWarnings("serial")
public class ShowLayouts extends JPanel {

  protected static Graph[] g_array;
  protected static int graph_index;
  protected static String[] graph_names = {
    "Two component graph",
    "Random mixed-mode graph",
    "Miscellaneous multicomponent graph",
    "One component graph",
    "Chain+isolate graph",
    "Trivial (disconnected) graph",
    "Little Graph"
  };

  BalloonLayoutRings balloonLayoutRings;
  RadialLayoutRings radialLayoutRings;

  public ShowLayouts() {

    g_array = new Graph[graph_names.length];

    Supplier<Integer> nodeFactory =
        new Supplier<Integer>() {
          int count;

          public Integer get() {
            return count++;
          }
        };
    Supplier<Number> edgeFactory =
        new Supplier<Number>() {
          int count;

          public Number get() {
            return count++;
          }
        };

    g_array[0] = TestGraphs.createTestGraph(false);
    g_array[1] = TestGraphs.getGeneratedNetwork();
    g_array[2] = TestGraphs.getDemoGraph();
    g_array[3] = TestGraphs.getOneComponentGraph();
    g_array[4] = TestGraphs.createChainPlusIsolates(18, 5);
    g_array[5] = TestGraphs.createChainPlusIsolates(0, 20);
    Graph network =
        GraphTypeBuilder.forGraphType(DefaultGraphType.directedMultigraph()).buildGraph();

    network.addVertex("A");
    network.addVertex("B");
    network.addVertex("C");
    network.addEdge("A", "B", 1);
    network.addEdge("A", "C", 2);

    g_array[6] = network;

    Graph g = g_array[3]; // initial graph

    final VisualizationViewer vv = new VisualizationViewer<>(g, new Dimension(600, 600));

    vv.getRenderContext().setNodeLabelFunction(Object::toString);

    final DefaultModalGraphMouse<Integer, Number> graphMouse = new DefaultModalGraphMouse<>();
    vv.setGraphMouse(graphMouse);

    vv.setNodeToolTipFunction(
        node ->
            node.toString()
                + ". with neighbors:"
                + Graphs.neighborListOf(vv.getModel().getNetwork(), node));

    final ScalingControl scaler = new CrossoverScalingControl();

    JButton plus = new JButton("+");
    plus.addActionListener(e -> scaler.scale(vv, 1.1f, vv.getCenter()));
    JButton minus = new JButton("-");
    minus.addActionListener(e -> scaler.scale(vv, 1 / 1.1f, vv.getCenter()));

    JComboBox modeBox = graphMouse.getModeComboBox();
    modeBox.addItemListener(
        ((DefaultModalGraphMouse<Integer, Number>) vv.getGraphMouse()).getModeListener());

    vv.setBackground(Color.WHITE);

    setLayout(new BorderLayout());
    add(vv, BorderLayout.CENTER);
    LayoutHelper.Layouts[] combos = LayoutHelper.getCombos();
    final JRadioButton animateLayoutTransition = new JRadioButton("Animate Layout Transition");

    final JComboBox jcb = new JComboBox(combos);
    jcb.addActionListener(
        e ->
            SwingUtilities.invokeLater(
                () -> {
                  LayoutHelper.Layouts layoutType = (LayoutHelper.Layouts) jcb.getSelectedItem();
                  LayoutAlgorithm layoutAlgorithm = LayoutHelper.createLayout(layoutType);
                  vv.removePreRenderPaintable(balloonLayoutRings);
                  vv.removePreRenderPaintable(radialLayoutRings);
                  if ((layoutAlgorithm instanceof TreeLayoutAlgorithm
                          || layoutAlgorithm instanceof BalloonLayoutAlgorithm
                          || layoutAlgorithm instanceof RadialTreeLayoutAlgorithm)
                      && vv.getModel().getNetwork().getType().isUndirected()) {
                    Graph tree = SpanningTreeAdapter.getSpanningTree(vv.getModel().getNetwork());
                    LayoutModel positionModel =
                        this.getTreeLayoutPositions(
                            tree,
                            //                            SpanningTreeAdapter.getSpanningTree(vv.getModel().getNetwork()),
                            layoutAlgorithm);
                    vv.getModel().getLayoutModel().setInitializer(positionModel);
                    layoutAlgorithm = new StaticLayoutAlgorithm();
                  }
                  if (animateLayoutTransition.isSelected()) {
                    LayoutAlgorithmTransition.animate(vv, layoutAlgorithm);
                  } else {
                    LayoutAlgorithmTransition.apply(vv, layoutAlgorithm);
                  }
                  if (layoutAlgorithm instanceof BalloonLayoutAlgorithm) {
                    balloonLayoutRings =
                        new BalloonLayoutRings(vv, (BalloonLayoutAlgorithm) layoutAlgorithm);
                    vv.addPreRenderPaintable(balloonLayoutRings);
                  }
                  if (layoutAlgorithm instanceof RadialTreeLayoutAlgorithm) {
                    radialLayoutRings =
                        new RadialLayoutRings(vv, (RadialTreeLayoutAlgorithm) layoutAlgorithm);
                    vv.addPreRenderPaintable(radialLayoutRings);
                  }
                }));

    jcb.setSelectedItem(LayoutHelper.Layouts.FR);

    JPanel control_panel = new JPanel(new GridLayout(2, 1));
    JPanel topControls = new JPanel();
    JPanel bottomControls = new JPanel();
    control_panel.add(topControls);
    control_panel.add(bottomControls);
    add(control_panel, BorderLayout.NORTH);

    final JComboBox graph_chooser = new JComboBox(graph_names);
    // do this before adding the listener so there is no event fired
    graph_chooser.setSelectedIndex(2);

    graph_chooser.addActionListener(
        e ->
            SwingUtilities.invokeLater(
                () -> {
                  graph_index = graph_chooser.getSelectedIndex();
                  vv.getNodeSpatial().clear();
                  vv.getEdgeSpatial().clear();
                  vv.getModel().setNetwork(g_array[graph_index]);
                }));

    JButton showRTree = new JButton("Show RTree");
    showRTree.addActionListener(e -> RTreeVisualization.showRTree(vv));

    topControls.add(jcb);
    topControls.add(graph_chooser);
    bottomControls.add(animateLayoutTransition);
    bottomControls.add(plus);
    bottomControls.add(minus);
    bottomControls.add(modeBox);
    bottomControls.add(showRTree);
  }

  LayoutModel getTreeLayoutPositions(Graph tree, LayoutAlgorithm treeLayout) {
    LayoutModel model = LoadingCacheLayoutModel.builder().size(600, 600).graph(tree).build();
    model.accept(treeLayout);
    return model;
  }

  private Collection getRoots(Graph graph) {
    Set roots = new HashSet<>();
    for (Object v : graph.vertexSet()) {
      if (Graphs.predecessorListOf(graph, v).isEmpty()) {
        roots.add(v);
      }
    }
    return roots;
  }

  public static void main(String[] args) {
    JPanel jp = new ShowLayouts();

    JFrame jf = new JFrame();
    jf.getContentPane().add(jp);
    jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    jf.pack();
    jf.setVisible(true);
  }
}
