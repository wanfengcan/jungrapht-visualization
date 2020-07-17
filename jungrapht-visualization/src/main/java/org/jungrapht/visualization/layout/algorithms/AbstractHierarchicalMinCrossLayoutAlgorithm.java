package org.jungrapht.visualization.layout.algorithms;

import static org.jungrapht.visualization.VisualizationServer.PREFIX;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jgrapht.Graph;
import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.decorators.EdgeShape;
import org.jungrapht.visualization.layout.algorithms.sugiyama.Layering;
import org.jungrapht.visualization.layout.algorithms.util.AfterRunnable;
import org.jungrapht.visualization.layout.algorithms.util.ComponentGrouping;
import org.jungrapht.visualization.layout.algorithms.util.EdgeShapeFunctionSupplier;
import org.jungrapht.visualization.layout.algorithms.util.ExecutorConsumer;
import org.jungrapht.visualization.layout.algorithms.util.LayeredRunnable;
import org.jungrapht.visualization.layout.algorithms.util.Threaded;
import org.jungrapht.visualization.layout.algorithms.util.VertexShapeAware;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.layout.model.Rectangle;
import org.jungrapht.visualization.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Sugiyama Hierarchical Minimum-Cross layout algorithm
 *
 * @see "Methods for Visual Understanding Hierarchical System Structures. KOZO SUGIYAMA, MEMBER,
 *     IEEE, SHOJIRO TAGAWA, AND MITSUHIKO TODA, MEMBER, IEEE"
 * @see "An E log E Line Crossing Algorithm for Levelled Graphs. Vance Waddle and Ashok Malhotra IBM
 *     Thomas J. Watson Research Center"
 * @see "Simple and Efficient Bilayer Cross Counting. Wilhelm Barth, Petra Mutzel, Institut für
 *     Computergraphik und Algorithmen Technische Universität Wien, Michael Jünger, Institut für
 *     Informatik Universität zu Köln"
 * @see "Fast and Simple Horizontal Coordinate Assignment, Ulrik Brandes and Boris Köpf, Department
 *     of Computer & Information Science, University of Konstanz"
 * @param <V> vertex type
 * @param <E> edge type
 */
public abstract class AbstractHierarchicalMinCrossLayoutAlgorithm<V, E>
    implements LayoutAlgorithm<V>,
        VertexShapeAware<V>,
        EdgeShapeFunctionSupplier<V, E>,
        Layered,
        AfterRunnable,
        Threaded,
        ExecutorConsumer,
        Future {

  private static final Logger log =
      LoggerFactory.getLogger(AbstractHierarchicalMinCrossLayoutAlgorithm.class);

  protected static final Shape IDENTITY_SHAPE = new Ellipse2D.Double();
  protected static final String MINCROSS_STRAIGHTEN_EDGES = PREFIX + "mincross.straightenEdges";
  protected static final String MINCROSS_POST_STRAIGHTEN = PREFIX + "mincross.postStraighten";
  protected static final String MINCROSS_THREADED = PREFIX + "mincross.threaded";
  protected static final String TRANSPOSE_LIMIT = PREFIX + "mincross.transposeLimit";
  protected static final String MAX_LEVEL_CROSS = PREFIX + "mincross.maxLevelCross";

  /**
   * a Builder to create a configured instance
   *
   * @param <V> the vertex type
   * @param <E> the edge type
   * @param <T> the type that is built
   * @param <B> the builder type
   */
  public abstract static class Builder<
          V,
          E,
          T extends
              AbstractHierarchicalMinCrossLayoutAlgorithm<V, E> & EdgeAwareLayoutAlgorithm<V, E>,
          B extends Builder<V, E, T, B>>
      implements LayoutAlgorithm.Builder<V, T, B> {
    protected Executor executor;
    protected Function<V, Shape> vertexShapeFunction = v -> IDENTITY_SHAPE;
    protected Consumer<Function<Context<Graph<V, E>, E>, Shape>> edgeShapeFunctionConsumer =
        i -> {};
    protected boolean straightenEdges =
        Boolean.parseBoolean(System.getProperty(MINCROSS_STRAIGHTEN_EDGES, "true"));
    protected boolean postStraighten =
        Boolean.parseBoolean(System.getProperty(MINCROSS_POST_STRAIGHTEN, "true"));;
    protected boolean transpose = true;
    //    protected int transposeLimit = Integer.getInteger(TRANSPOSE_LIMIT, 6); ///
    protected int maxLevelCross = Integer.getInteger(MAX_LEVEL_CROSS, 23);
    protected boolean expandLayout = true;
    protected Layering layering = Layering.TOP_DOWN;
    protected Runnable after = () -> {};
    protected boolean threaded =
        Boolean.parseBoolean(System.getProperty(MINCROSS_THREADED, "true"));
    protected boolean separateComponents = true;

    /** {@inheritDoc} */
    protected B self() {
      return (B) this;
    }

    public B vertexShapeFunction(Function<V, Shape> vertexShapeFunction) {
      this.vertexShapeFunction = vertexShapeFunction;
      return self();
    }

    public B edgeShapeFunctionConsumer(
        Consumer<Function<Context<Graph<V, E>, E>, Shape>> edgeShapeFunctionConsumer) {
      this.edgeShapeFunctionConsumer = edgeShapeFunctionConsumer;
      return self();
    }

    public B straightenEdges(boolean straightenEdges) {
      this.straightenEdges = straightenEdges;
      return self();
    }

    public B postStraighten(boolean postStraighten) {
      this.postStraighten = postStraighten;
      return self();
    }

    public B transpose(boolean transpose) {
      this.transpose = transpose;
      return self();
    }

    //    public B transposeLimit(int transposeLimit) {
    //      this.transposeLimit = transposeLimit;
    //      return self();
    //    }

    public B maxLevelCross(int maxLevelCross) {
      this.maxLevelCross = maxLevelCross;
      return self();
    }

    /** {@inheritDoc} */
    public B expandLayout(boolean expandLayout) {
      this.expandLayout = expandLayout;
      return self();
    }

    public B layering(Layering layering) {
      this.layering = layering;
      return self();
    }

    public B threaded(boolean threaded) {
      this.threaded = threaded;
      return self();
    }

    public B executor(Executor executor) {
      this.executor = executor;
      return self();
    }

    public B after(Runnable after) {
      this.after = after;
      return self();
    }

    public B separateComponents(boolean separateComponents) {
      this.separateComponents = separateComponents;
      return self();
    }

    //    /** {@inheritDoc} */
    //    public T build() {
    //      return (T) new BaseHierarchicalMinCrossLayoutAlgorithm<>(this);
    //    }
  }

  /**
   * @param <V> vertex type
   * @param <E> edge type
   * @return a Builder ready to configure
   */
  //  public static <V, E> Builder<V, E, ?, ?> edgeAwareBuilder() {
  //    return new Builder<>();
  //  }

  protected Rectangle bounds = Rectangle.IDENTITY;

  protected List<V> roots;

  protected Function<V, Shape> vertexShapeFunction;
  protected Consumer<Function<Context<Graph<V, E>, E>, Shape>> edgeShapeFunctionConsumer;
  protected boolean straightenEdges;
  protected boolean postStraighten;
  protected boolean transpose;
  //  protected int transposeLimit; /// sugiyama
  protected int maxLevelCross;
  protected boolean expandLayout;
  protected RenderContext<V, E> renderContext;
  protected boolean threaded;
  protected Layering layering;
  protected Executor executor;
  protected CompletableFuture theFuture;
  protected Runnable after;
  protected boolean separateComponents;
  protected Map<E, List<Point>> edgePointMap = new HashMap<>();
  protected EdgeShape.ArticulatedLine<V, E> edgeShape = new EdgeShape.ArticulatedLine<>();
  protected int completionCounter = 0;

  //  protected BaseHierarchicalMinCrossLayoutAlgorithm() {
  //    this(BaseHierarchicalMinCrossLayoutAlgorithm.edgeAwareBuilder());
  //  }

  protected AbstractHierarchicalMinCrossLayoutAlgorithm(Builder builder) {
    this(
        builder.vertexShapeFunction,
        builder.edgeShapeFunctionConsumer,
        builder.straightenEdges,
        builder.postStraighten,
        builder.transpose,
        //        builder.transposeLimit,
        builder.maxLevelCross,
        builder.expandLayout,
        builder.layering,
        builder.threaded,
        builder.executor,
        builder.separateComponents,
        builder.after);
  }

  protected AbstractHierarchicalMinCrossLayoutAlgorithm(
      Function<V, Shape> vertexShapeFunction,
      Consumer<Function<Context<Graph<V, E>, E>, Shape>> edgeShapeFunctionConsumer,
      boolean straightenEdges,
      boolean postStraighten,
      boolean transpose,
      //      int transposeLimit,
      int maxLevelCross,
      boolean expandLayout,
      Layering layering,
      boolean threaded,
      Executor executor,
      boolean separateComponents,
      Runnable after) {
    this.vertexShapeFunction = vertexShapeFunction;
    this.edgeShapeFunctionConsumer = edgeShapeFunctionConsumer;
    this.straightenEdges = straightenEdges;
    this.postStraighten = postStraighten;
    this.transpose = transpose;
    //    this.transposeLimit = transposeLimit;
    this.maxLevelCross = maxLevelCross;
    this.expandLayout = expandLayout;
    this.layering = layering;
    this.threaded = threaded;
    this.executor = executor;
    this.separateComponents = separateComponents;
    this.after = after;
    this.edgeShape.setEdgeArticulationFunction(
        e -> edgePointMap.getOrDefault(e, Collections.emptyList()));
  }

  @Override
  public void setVertexShapeFunction(Function<V, Shape> vertexShapeFunction) {
    this.vertexShapeFunction = vertexShapeFunction;
  }

  @Override
  public void setEdgeShapeFunctionConsumer(
      Consumer<Function<Context<Graph<V, E>, E>, Shape>> edgeShapeFunctionConsumer) {
    this.edgeShapeFunctionConsumer = edgeShapeFunctionConsumer;
  }

  @Override
  public void setLayering(Layering layering) {
    this.edgePointMap.clear();
    this.layering = layering;
  }

  @Override
  public boolean isThreaded() {
    return this.threaded;
  }

  @Override
  public void setThreaded(boolean threaded) {
    this.threaded = threaded;
  }

  protected boolean isComplete(int expected) {
    boolean isComplete = ++completionCounter >= expected;
    log.trace(
        " completionCounter:{}, expected: {} isComplete:{}",
        completionCounter,
        expected,
        isComplete);
    return isComplete;
  }

  @Override
  public void setExecutor(Executor executor) {
    this.executor = executor;
  }

  @Override
  public Executor getExecutor() {
    return this.executor;
  }

  protected abstract LayeredRunnable<E> getRunnable(
      int componentCount, LayoutModel<V> componentLayoutModel);

  @Override
  public void visit(LayoutModel<V> layoutModel) {
    this.edgePointMap.clear();
    this.completionCounter = 0;

    Graph<V, E> graph = layoutModel.getGraph();
    if (graph == null || graph.vertexSet().isEmpty()) {
      return;
    }
    List<Graph<V, E>> graphs;
    List<LayoutModel<V>> layoutModels = new ArrayList<>();

    if (separateComponents) {
      // if this is a multicomponent graph, discover components and create a temp
      // LayoutModel for each to visit. Afterwards, append all the layoutModels
      // to the one visited above.
      graphs = ComponentGrouping.getComponentGraphs(graph);

      for (int i = 0; i < graphs.size(); i++) {
        LayoutModel<V> componentLayoutModel =
            LayoutModel.<V>builder()
                .graph(graphs.get(i))
                .width(layoutModel.getWidth())
                .height(layoutModel.getHeight())
                .build();
        layoutModels.add(componentLayoutModel);
      }
    } else {
      graphs = Collections.singletonList(graph);
      layoutModels.add(layoutModel);
    }

    for (LayoutModel<V> componentLayoutModel : layoutModels) {

      LayeredRunnable<E> runnable = getRunnable(graphs.size(), componentLayoutModel);
      if (threaded) {
        if (executor != null) {
          theFuture =
              CompletableFuture.runAsync(runnable, executor)
                  .thenRun(
                      () -> {
                        log.trace("Sugiyama layout done");
                        this.edgePointMap.putAll(runnable.getEdgePointMap());
                        layoutModel.appendLayoutModel(componentLayoutModel);
                        if (isComplete(graphs.size())) {
                          this.run(); // run the after function
                          layoutModel.getViewChangeSupport().fireViewChanged();
                          // fire an event to say that the layout is done
                          layoutModel
                              .getLayoutStateChangeSupport()
                              .fireLayoutStateChanged(layoutModel, false);
                        }
                      });
        } else {
          theFuture =
              CompletableFuture.runAsync(runnable)
                  .thenRun(
                      () -> {
                        log.trace("Sugiyama layout done");
                        this.edgePointMap.putAll(runnable.getEdgePointMap());
                        layoutModel.appendLayoutModel(componentLayoutModel);
                        if (isComplete(graphs.size())) {
                          this.run(); // run the after function
                          layoutModel.getViewChangeSupport().fireViewChanged();
                          // fire an event to say that the layout is done
                          layoutModel
                              .getLayoutStateChangeSupport()
                              .fireLayoutStateChanged(layoutModel, false);
                        }
                      });
        }
      } else {
        runnable.run();
        this.edgePointMap.putAll(runnable.getEdgePointMap());
        layoutModel.appendLayoutModel(componentLayoutModel);
        if (isComplete(graphs.size())) {
          after.run();
          layoutModel.getViewChangeSupport().fireViewChanged();
          // fire an event to say that the layout is done
          layoutModel.getLayoutStateChangeSupport().fireLayoutStateChanged(layoutModel, false);
        }
      }
      edgeShapeFunctionConsumer.accept(edgeShape);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (theFuture != null) {
      return theFuture.cancel(mayInterruptIfRunning);
    }
    return false;
  }

  @Override
  public boolean isCancelled() {
    if (theFuture != null) {
      return theFuture.isCancelled();
    }
    return false;
  }

  @Override
  public boolean isDone() {
    if (theFuture != null) {
      return theFuture.isDone();
    }
    return false;
  }

  @Override
  public Object get() throws InterruptedException, ExecutionException {
    if (theFuture != null) {
      return theFuture.get();
    }
    return null;
  }

  @Override
  public Object get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    if (theFuture != null) {
      return theFuture.get(timeout, unit);
    }
    return null;
  }

  @Override
  public void run() {
    after.run();
  }

  @Override
  public void setAfter(Runnable after) {
    this.after = after;
  }
}
