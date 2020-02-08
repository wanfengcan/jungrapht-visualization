package org.jungrapht.samples.sugiyama.test.algorithms;

import static org.jungrapht.visualization.VisualizationServer.PREFIX;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jungrapht.visualization.decorators.EdgeShape;
import org.jungrapht.visualization.layout.algorithms.eiglsperger.EiglspergerRunnable;
import org.jungrapht.visualization.layout.algorithms.eiglsperger.EiglspergerUtil;
import org.jungrapht.visualization.layout.algorithms.eiglsperger.SyntheticLV;
import org.jungrapht.visualization.layout.algorithms.eiglsperger.Synthetics;
import org.jungrapht.visualization.layout.algorithms.sugiyama.ArticulatedEdge;
import org.jungrapht.visualization.layout.algorithms.sugiyama.GraphLayers;
import org.jungrapht.visualization.layout.algorithms.sugiyama.GreedyCycleRemoval;
import org.jungrapht.visualization.layout.algorithms.sugiyama.LE;
import org.jungrapht.visualization.layout.algorithms.sugiyama.LV;
import org.jungrapht.visualization.layout.algorithms.sugiyama.TransformedGraphSupplier;
import org.jungrapht.visualization.layout.algorithms.sugiyama.Unaligned;
import org.jungrapht.visualization.layout.algorithms.util.Attributed;
import org.jungrapht.visualization.layout.model.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see "Methods for Visual Understanding Hierarchical System Structures. KOZO SUGIYAMA, MEMBER,
 *     IEEE, SHOJIRO TAGAWA, AND MITSUHIKO TODA, MEMBER, IEEE"
 * @see "An E log E Line Crossing Algorithm for Levelled Graphs. Vance Waddle and Ashok Malhotra IBM
 *     Thomas J. Watson Research Center"
 * @see "Simple and Efficient Bilayer Cross Counting. Wilhelm Barth, Petra Mutzel, Institut für
 *     Computergraphik und Algorithmen Technische Universität Wien, Michael Jünger, Institut für
 *     Informatik Universität zu Köln"
 * @see "Fast and Simple Horizontal Coordinate Assignment, Ulrik Brandes and Boris Köpf, Department
 *     of Computer & Information Science, University of Konstanz"
 * @see "An Efficient Implementation of Sugiyama's Algorithm for Layered Graph Drawing. Markus
 *     Eiglsperger, Martin Siebenhaller, Michael Kaufman"
 * @param <V> vertex type
 * @param <E> edge type
 */
public class TestEiglspergerRunnable<V, E> extends EiglspergerRunnable<V, E> implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(TestEiglspergerRunnable.class);

  /**
   * a Builder to create a configured instance
   *
   * @param <V> the vertex type
   * @param <E> the edge type
   * @param <T> the type that is built
   * @param <B> the builder type
   */
  public static class Builder<
          V, E, T extends TestEiglspergerRunnable<V, E>, B extends Builder<V, E, T, B>>
      extends EiglspergerRunnable.Builder<V, E, T, B> {
    boolean doUpLeft;
    boolean doDownLeft;
    boolean doUpRight;
    boolean doDownRight;

    public B doUpLeft(boolean doUpLeft) {
      this.doUpLeft = doUpLeft;
      return self();
    }

    public B doUpRight(boolean doUpRight) {
      this.doUpRight = doUpRight;
      return self();
    }

    public B doDownLeft(boolean doDownLeft) {
      this.doDownLeft = doDownLeft;
      return self();
    }

    public B doDownRight(boolean doDownRight) {
      this.doDownRight = doDownRight;
      return self();
    }

    /** {@inheritDoc} */
    public T build() {
      return (T) new TestEiglspergerRunnable<>(this);
    }
  }

  /**
   * @param <V> vertex type
   * @param <E> edge type
   * @return a Builder ready to configure
   */
  public static <V, E> Builder<V, E, ?, ?> builder() {
    return new Builder<>();
  }

  //  final LayoutModel<V> layoutModel;
  //  final RenderContext<V, E> renderContext;
  //  Graph<V, E> graph;
  //  Graph<LV<V>, LE<V, E>> svGraph;
  //  boolean stopit = false;
  //  protected Predicate<V> vertexPredicate;
  //  protected Predicate<E> edgePredicate;
  //  protected Comparator<V> vertexComparator;
  //  protected Comparator<E> edgeComparator;
  //  protected boolean straightenEdges;
  //  protected boolean postStraighten;
  //  protected int maxLevelCross;
  //  protected boolean useLongestPathLayering;
  boolean doUpLeft;
  boolean doDownLeft;
  boolean doUpRight;
  boolean doDownRight;

  protected TestEiglspergerRunnable(Builder<V, E, ?, ?> builder) {
    super(builder);
    this.doUpLeft = builder.doUpLeft;
    this.doDownLeft = builder.doDownLeft;
    this.doUpRight = builder.doUpRight;
    this.doDownRight = builder.doDownRight;
  }

  //  private TestEiglspergerRunnable(
  //      LayoutModel<V> layoutModel,
  //      RenderContext<V, E> renderContext,
  //      boolean straightenEdges,
  //      boolean postStraighten,
  //      int maxLevelCross,
  //      boolean useLongestPathLayering) {
  //    this.layoutModel = layoutModel;
  //    this.renderContext = renderContext;
  //    this.straightenEdges = straightenEdges;
  //    this.postStraighten = postStraighten;
  //    this.maxLevelCross = maxLevelCross;
  //    this.useLongestPathLayering = useLongestPathLayering;
  //  }
  //
  //  protected boolean checkStopped() {
  //    try {
  //      Thread.sleep(1);
  //      if (stopit) {
  //        return true;
  //      }
  //    } catch (InterruptedException ex) {
  //    }
  //    return false;
  //  }

  @Override
  public void run() {
    this.graph = layoutModel.getGraph();

    long startTime = System.currentTimeMillis();
    TransformedGraphSupplier<V, E> transformedGraphSupplier = new TransformedGraphSupplier(graph);
    this.svGraph = transformedGraphSupplier.get();
    long transformTime = System.currentTimeMillis();
    log.trace("transform Graph took {}", (transformTime - startTime));

    if (checkStopped()) {
      return;
    }
    GreedyCycleRemoval<LV<V>, LE<V, E>> greedyCycleRemoval = new GreedyCycleRemoval(svGraph);
    Collection<LE<V, E>> feedbackArcs = greedyCycleRemoval.getFeedbackArcs();

    // reverse the direction of feedback arcs so that they no longer introduce cycles in the graph
    // the feedback arcs will be processed later to draw with the correct direction and correct articulation points
    for (LE<V, E> se : feedbackArcs) {
      svGraph.removeEdge(se);
      LE<V, E> newEdge = LE.of(se.getEdge(), se.getTarget(), se.getSource());
      svGraph.addEdge(newEdge.getSource(), newEdge.getTarget(), newEdge);
    }
    long cycles = System.currentTimeMillis();
    log.trace("remove cycles took {}", (cycles - transformTime));

    List<List<LV<V>>> layers;
    if (useLongestPathLayering) {
      layers = GraphLayers.longestPath(svGraph);
    } else {
      layers = GraphLayers.assign(svGraph);
    }
    long assignLayersTime = System.currentTimeMillis();
    log.trace("assign layers took {} ", (assignLayersTime - cycles));
    if (log.isTraceEnabled()) {
      GraphLayers.checkLayers(layers);
    }

    if (checkStopped()) {
      return;
    }

    Synthetics<V, E> synthetics = new Synthetics<>(svGraph);
    List<LE<V, E>> edges = new ArrayList<>(svGraph.edgeSet());
    LV<V>[][] layersArray = synthetics.createVirtualVerticesAndEdges(edges, layers);

    if (log.isTraceEnabled()) {
      GraphLayers.checkLayers(layersArray);
    }

    if (checkStopped()) {
      return;
    }

    // save off a map of edge lists keyed on the source vertex
    Map<Integer, List<LE<V, E>>> edgesKeyedOnSource = new LinkedHashMap<>();
    edges.forEach(
        e -> {
          int sourceRank = e.getSource().getRank();
          if (edgesKeyedOnSource.containsKey(sourceRank)) {
            edgesKeyedOnSource.get(sourceRank).add(e);
          } else {
            ArrayList<LE<V, E>> list = new ArrayList<>();
            list.add(e);
            edgesKeyedOnSource.put(sourceRank, list);
          }
        });

    // save off a map of edge lists keyed on the target vertex
    Map<Integer, List<LE<V, E>>> edgesKeyedOnTarget = new LinkedHashMap<>();
    svGraph
        .edgeSet()
        .forEach(
            e -> {
              int targetRank = e.getTarget().getRank();
              if (edgesKeyedOnTarget.containsKey(targetRank)) {
                edgesKeyedOnTarget.get(targetRank).add(e);
              } else {
                ArrayList<LE<V, E>> list = new ArrayList<>();
                list.add(e);
                edgesKeyedOnTarget.put(targetRank, list);
              }
            });

    long syntheticsTime = System.currentTimeMillis();
    log.trace("synthetics took {}", (syntheticsTime - assignLayersTime));

    int bestCrossCount = Integer.MAX_VALUE;
    int edgeCount = svGraph.edgeSet().size();
    if (edgeCount > 200) {
      maxLevelCross = 1;
    }
    for (int i = 0; i < maxLevelCross; i++) {
      int forwardCrossCount = 0;
      int reverseCrossCount = 0;
      if (i % 2 == 0) {
        int count = sweepForward(svGraph, layersArray, edgesKeyedOnSource);
        forwardCrossCount = count;
        EiglspergerUtil.check(layersArray);
      } else {
        int count = sweepBackwards(svGraph, layersArray, edgesKeyedOnTarget);
        reverseCrossCount = count;
        EiglspergerUtil.check(layersArray);
      }
      int twoWayCrossCount = forwardCrossCount + reverseCrossCount;
      if (twoWayCrossCount < bestCrossCount) {
        bestCrossCount = twoWayCrossCount;
      } else {
        log.trace("the bext cross count was {}", bestCrossCount);
        break;
      }
    }

    // done optimizing for edge crossing
    LV<V>[][] best = layersArray;

    // figure out the avg size of rendered vertex
    Rectangle avgVertexBounds = maxVertexBounds(best, renderContext.getVertexShapeFunction());

    int horizontalOffset =
        Math.max(
            avgVertexBounds.width, Integer.getInteger(PREFIX + "mincross.horizontalOffset", 50));
    int verticalOffset =
        Math.max(
            avgVertexBounds.height, Integer.getInteger(PREFIX + "mincross.verticalOffset", 50));
    GraphLayers.checkLayers(best);
    Map<LV<V>, Point> vertexPointMap = new HashMap<>();

    // update the indices of the all layers
    for (int i = 0; i < best.length; i++) {
      for (int j = 0; j < best[i].length; j++) {
        best[i][j].setIndex(j);
      }
    }
    if (straightenEdges) {
      SelectiveEiglspergerHorizontalCoordinateAssignment<V, E> horizontalCoordinateAssignment =
          new SelectiveEiglspergerHorizontalCoordinateAssignment(
              layersArray,
              svGraph,
              new HashSet<>(),
              50,
              50,
              doUpLeft,
              doUpRight,
              doDownLeft,
              doDownRight);
      horizontalCoordinateAssignment.horizontalCoordinateAssignment();

      //      HorizontalCoordinateAssignment<V, E> horizontalCoordinateAssignment =
      //      new HorizontalCoordinateAssignment<>(
      //          best, svGraph, new HashSet<>(), horizontalOffset, verticalOffset);
      //      horizontalCoordinateAssignment.horizontalCoordinateAssignment();

      GraphLayers.checkLayers(best);

      for (int i = 0; i < best.length; i++) {
        for (int j = 0; j < best[i].length; j++) {
          LV<V> EiglspergerVertex = best[i][j];
          vertexPointMap.put(EiglspergerVertex, EiglspergerVertex.getPoint());
        }
      }

    } else {
      Unaligned.centerPoints(
          best,
          renderContext.getVertexShapeFunction(),
          horizontalOffset,
          verticalOffset,
          vertexPointMap);
    }

    Map<Integer, Integer> rowWidthMap = new HashMap<>(); // all the row widths
    Map<Integer, Integer> rowMaxHeightMap = new HashMap<>(); // all the row heights
    int layerIndex = 0;
    Function<V, Shape> vertexShapeFunction = renderContext.getVertexShapeFunction();
    int totalHeight = 0;
    int totalWidth = 0;

    for (int i = 0; i < best.length; i++) {

      int width = horizontalOffset;
      int maxHeight = 0;
      for (int j = 0; j < best[i].length; j++) {
        LV<V> v = best[i][j];
        if (!(v instanceof SyntheticLV)) {
          Rectangle bounds = vertexShapeFunction.apply(v.getVertex()).getBounds();
          width += bounds.width + horizontalOffset;
          maxHeight = Math.max(maxHeight, bounds.height);
        } else {
          width += horizontalOffset;
        }
      }
      rowWidthMap.put(layerIndex, width);
      rowMaxHeightMap.put(layerIndex, maxHeight);
      layerIndex++;
    }

    int widestRowWidth = rowWidthMap.values().stream().mapToInt(v -> v).max().getAsInt();
    int x = horizontalOffset;
    int y = verticalOffset;
    layerIndex = 0;
    if (log.isTraceEnabled()) {
      log.trace("layerMaxHeights {}", rowMaxHeightMap);
    }
    for (int i = 0; i < best.length; i++) {
      int previousVertexWidth = 0;
      // offset against widest row
      x += (widestRowWidth - rowWidthMap.get(layerIndex)) / 2;

      y += rowMaxHeightMap.get(layerIndex) / 2;
      if (layerIndex > 0) {
        y += rowMaxHeightMap.get(layerIndex - 1) / 2;
      }

      int rowWidth = 0;
      for (int j = 0; j < best[i].length; j++) {
        LV<V> EiglspergerVertex = best[i][j];
        int vertexWidth = 0;
        if (!(EiglspergerVertex instanceof SyntheticLV)) {
          vertexWidth = vertexShapeFunction.apply(EiglspergerVertex.getVertex()).getBounds().width;
        }

        x += previousVertexWidth / 2 + vertexWidth / 2 + horizontalOffset;

        rowWidth = x + vertexWidth / 2;
        log.trace("layerIndex {} y is {}", layerIndex, y);
        previousVertexWidth = vertexWidth;
      }
      totalWidth = Math.max(totalWidth, rowWidth);

      x = horizontalOffset;
      y += verticalOffset;
      totalHeight = y + rowMaxHeightMap.get(layerIndex) / 2;
      layerIndex++;
    }

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = -1;
    int maxY = -1;
    for (Point p : vertexPointMap.values()) {
      minX = Math.min((int) p.x, minX);
      maxX = Math.max((int) p.x, maxX);
      minY = Math.min((int) p.y, minY);
      maxY = Math.max((int) p.y, maxY);
    }
    maxX += horizontalOffset;
    maxY += verticalOffset;
    int pointRangeWidth = maxX - minX;
    int pointRangeHeight = maxY - minY;
    int offsetX = 0;
    int offsetY = 0;
    if (minX < 0) {
      offsetX += -minX + horizontalOffset;
    }
    if (minY < 0) {
      offsetY += -minY + verticalOffset;
    }
    pointRangeWidth *= 1.1;
    pointRangeHeight *= 1.1;

    int maxDimension = Math.max(totalWidth, totalHeight);

    layoutModel.setSize(
        Math.max(maxDimension, layoutModel.getWidth()),
        Math.max(maxDimension, layoutModel.getHeight()));
    long pointsSetTime = System.currentTimeMillis();
    double scalex = (double) layoutModel.getWidth() / pointRangeWidth;
    double scaley = (double) layoutModel.getHeight() / pointRangeHeight;

    for (Map.Entry<LV<V>, Point> entry : vertexPointMap.entrySet()) {
      Point p = entry.getValue();
      Point q = Point.of((offsetX + p.x) * scalex, (offsetY + p.y) * scaley);
      entry.setValue(q);
    }

    // now all the vertices in layers (best) have points associated with them
    // every vertex in vertexMap has a point value
    svGraph.vertexSet().forEach(v -> v.setPoint(vertexPointMap.get(v)));

    if (postStraighten) {
      synthetics.alignArticulatedEdges();
    }
    List<ArticulatedEdge<V, E>> articulatedEdges = synthetics.makeArticulatedEdges();

    Set<E> feedbackEdges = new HashSet<>();
    feedbackArcs.forEach(a -> feedbackEdges.add(a.getEdge()));
    articulatedEdges
        .stream()
        .filter(ae -> feedbackEdges.contains(ae.getEdge()))
        .forEach(
            ae -> {
              svGraph.removeEdge(ae);
              LE<V, E> reversed = ae.reversed();
              svGraph.addEdge(reversed.getSource(), reversed.getTarget(), reversed);
            });

    Map<E, List<Point>> edgePointMap = new HashMap<>();
    for (ArticulatedEdge<V, E> ae : articulatedEdges) {
      List<Point> points = new ArrayList<>();
      if (feedbackEdges.contains(ae.getEdge())) {
        points.add(ae.getTarget().getPoint());
        points.addAll(ae.reversed().getIntermediatePoints());
        points.add(ae.getSource().getPoint());
      } else {
        points.add(ae.getSource().getPoint());
        points.addAll(ae.getIntermediatePoints());
        points.add(ae.getTarget().getPoint());
      }

      edgePointMap.put(ae.edge, points);
    }
    EdgeShape.ArticulatedLine<V, E> edgeShape = new EdgeShape.ArticulatedLine<>();
    edgeShape.setEdgeArticulationFunction(
        e -> edgePointMap.getOrDefault(e, Collections.emptyList()));

    renderContext.setEdgeShapeFunction(edgeShape);

    long articulatedEdgeTime = System.currentTimeMillis();
    log.trace("articulated edges took {}", (articulatedEdgeTime - pointsSetTime));

    svGraph.vertexSet().forEach(v -> layoutModel.set(v.getVertex(), v.getPoint()));
    for (LV<V> v : svGraph.vertexSet()) {
      if (v.getVertex() instanceof Attributed) {
        Attributed<V> va = (Attributed<V>) v.getVertex();
        va.set("pos", "" + v.getPos());
        va.set("idx", "" + v.getIndex());
        va.set("rank", "" + v.getRank());
      }
    }
  }

  //  public static <V, E> int sweepForward(
  //      Graph<LV<V>, LE<V, E>> svGraph,
  //      LV<V>[][] layersArray,
  //      Map<Integer, List<LE<V, E>>> edgesKeyedOnSource) {
  //
  //    int crossCount = 0;
  //    if (log.isTraceEnabled()) log.trace("sweepForward");
  //
  //    List<LV<V>> layerEye = null;
  //
  //    for (int i = 0; i < layersArray.length - 1; i++) {
  //      List<LE<V, E>> edges = new ArrayList<>(svGraph.edgeSet());
  //      if (layerEye == null) {
  //        layerEye =
  //            EiglspergerUtil.scan(
  //                EiglspergerUtil.createListOfVertices(layersArray[i])); // first rank
  //      }
  //
  //      BiLayer<V, E> biLayer =
  //          BiLayer.of(
  //              i,
  //              i + 1,
  //              layerEye,
  //              EiglspergerUtil.createListOfVertices(layersArray[i + 1]),
  //              layersArray[i + 1],
  //              PVertex.class::isInstance,
  //              QVertex.class::isInstance,
  //              Graphs::predecessorListOf);
  //
  //      EiglspergerSteps.stepOne(biLayer);
  //      // handled PVertices by merging them into containers
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepOneOut:{}", biLayer.currentLayer);
  //      }
  //
  //      List<VirtualEdge<V, E>> virtualEdges = new ArrayList<>();
  //
  //      EiglspergerSteps.stepTwo(biLayer, virtualEdges, svGraph);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepTwoOut:{}", biLayer.downstreamLayer);
  //      }
  //
  //      EiglspergerSteps.stepThree(biLayer);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepThreeOut:{}", biLayer.downstreamLayer);
  //      }
  //      EiglspergerUtil.fixIndices(biLayer.downstreamLayer);
  //
  //      EiglspergerSteps.stepFour(biLayer, virtualEdges);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepFourOut:{}", biLayer.downstreamLayer);
  //      }
  //
  //      // i want the edges keyed on this rank, plus any virtual edges
  //      List<LE<V, E>> reducedEdges = new ArrayList<>();
  //      reducedEdges.addAll(edgesKeyedOnSource.getOrDefault(i + 1, Collections.emptyList()));
  //      reducedEdges.addAll(virtualEdges);
  //
  //      crossCount += EiglspergerSteps.stepFive(svGraph, true, biLayer, virtualEdges);
  //      log.trace("forward stepFive crossCount:{}", crossCount);
  //
  //      EiglspergerSteps.stepSix(biLayer);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepSixOut:{}", biLayer.downstreamLayer);
  //      }
  //
  //      Arrays.sort(layersArray[i], Comparator.comparingInt(LV::getIndex));
  //      EiglspergerUtil.fixIndices(layersArray[i]);
  //      Arrays.sort(layersArray[i + 1], Comparator.comparingInt(LV::getIndex));
  //      EiglspergerUtil.fixIndices(layersArray[i + 1]);
  //      layerEye = biLayer.downstreamLayer;
  //    }
  //    return crossCount;
  //  }
  //
  //  public static <V, E> int sweepBackwards(
  //      Graph<LV<V>, LE<V, E>> svGraph,
  //      LV<V>[][] layersArray,
  //      Map<Integer, List<LE<V, E>>> edgesKeyedOnSource) {
  //
  //    int crossCount = 0;
  //    if (log.isTraceEnabled()) log.trace("sweepBackwards");
  //    List<LV<V>> layerEye = null;
  //    //    EiglspergerUtil.check(layersArray);
  //
  //    for (int i = layersArray.length - 1; i > 0; i--) {
  //      List<LE<V, E>> edges = new ArrayList<>(svGraph.edgeSet());
  //      if (layerEye == null) {
  //        layerEye =
  //            EiglspergerUtil.scan(EiglspergerUtil.createListOfVertices(layersArray[i])); // last rank
  //      }
  //
  //      BiLayer<V, E> biLayer =
  //          BiLayer.of(
  //              i,
  //              i - 1,
  //              layerEye,
  //              EiglspergerUtil.createListOfVertices(layersArray[i - 1]),
  //              layersArray[i - 1],
  //              QVertex.class::isInstance,
  //              PVertex.class::isInstance,
  //              Graphs::successorListOf);
  //      //      EiglspergerUtil.check(layersArray);
  //
  //      EiglspergerSteps.stepOne(biLayer);
  //      // handled PVertices by merging them into containers
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepOneOut:{}", biLayer.currentLayer);
  //      }
  //
  //      List<VirtualEdge<V, E>> virtualEdges = new ArrayList<>();
  //
  //      EiglspergerSteps.stepTwo(biLayer, virtualEdges, svGraph);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepTwoOut:{}", biLayer.downstreamLayer);
  //      }
  //
  //      EiglspergerSteps.stepThree(biLayer);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepThreeOut:{}", biLayer.downstreamLayer);
  //      }
  //      EiglspergerUtil.fixIndices(biLayer.downstreamLayer);
  //
  //      EiglspergerSteps.stepFour(biLayer, virtualEdges);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepFourOut:{}", biLayer.downstreamLayer);
  //      }
  //
  //      // i want the edges keyed on this rank, plus any virtual edges
  //      List<LE<V, E>> reducedEdges = new ArrayList<>();
  //      reducedEdges.addAll(edgesKeyedOnSource.getOrDefault(i + 1, Collections.emptyList()));
  //      reducedEdges.addAll(virtualEdges);
  //
  //      crossCount += EiglspergerSteps.stepFive(svGraph, false, biLayer, virtualEdges);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepFiveOut:{}", biLayer.downstreamLayer);
  //        log.trace("backwards stepFive crossCount:{}", crossCount);
  //      }
  //      EiglspergerSteps.stepSix(biLayer);
  //      if (log.isTraceEnabled()) {
  //        log.trace("stepSixOut:{}", biLayer.downstreamLayer);
  //      }
  //
  //      Arrays.sort(layersArray[i], Comparator.comparingInt(LV::getIndex));
  //      EiglspergerUtil.fixIndices(layersArray[i]);
  //      Arrays.sort(layersArray[i - 1], Comparator.comparingInt(LV::getIndex));
  //      EiglspergerUtil.fixIndices(layersArray[i - 1]);
  //      layerEye = biLayer.downstreamLayer;
  //    }
  //    return crossCount;
  //  }

  private static <V> Rectangle maxVertexBounds(
      LV<V>[][] layers, Function<V, Shape> vertexShapeFunction) {
    // figure out the largest rendered vertex
    Rectangle maxVertexBounds = new Rectangle();

    for (int i = 0; i < layers.length; i++) {
      for (int j = 0; j < layers[i].length; j++) {
        if (!(layers[i][j] instanceof SyntheticLV)) {
          Rectangle bounds = vertexShapeFunction.apply(layers[i][j].getVertex()).getBounds();
          int width = Math.max(bounds.width, maxVertexBounds.width);
          int height = Math.max(bounds.height, maxVertexBounds.height);
          maxVertexBounds = new Rectangle(width, height);
        }
      }
    }
    return maxVertexBounds;
  }

  private static <V> Rectangle avgVertexBounds(
      LV<V>[][] layers, Function<V, Shape> vertexShapeFunction) {

    LongSummaryStatistics w = new LongSummaryStatistics();
    LongSummaryStatistics h = new LongSummaryStatistics();
    for (int i = 0; i < layers.length; i++) {
      for (int j = 0; j < layers[i].length; j++) {
        if (!(layers[i][j] instanceof SyntheticLV)) {
          Rectangle bounds = vertexShapeFunction.apply(layers[i][j].getVertex()).getBounds();
          w.accept(bounds.width);
          h.accept(bounds.height);
        }
      }
    }
    return new Rectangle((int) w.getAverage(), (int) h.getAverage());
  }
}