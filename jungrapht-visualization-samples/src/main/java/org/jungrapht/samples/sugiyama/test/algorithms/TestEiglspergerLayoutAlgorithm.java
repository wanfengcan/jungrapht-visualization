package org.jungrapht.samples.sugiyama.test.algorithms;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.concurrent.CompletableFuture;
import org.jgrapht.Graph;
import org.jungrapht.visualization.layout.algorithms.EdgeAwareLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.EiglspergerLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test only, as this class is hard-coded for a specific test graph
 *
 * @param <V>
 * @param <E>
 */
public class TestEiglspergerLayoutAlgorithm<V, E> extends EiglspergerLayoutAlgorithm<V, E>
    implements LayoutAlgorithm<V> {

  private static final Logger log = LoggerFactory.getLogger(TestEiglspergerLayoutAlgorithm.class);

  private static final Shape IDENTITY_SHAPE = new Ellipse2D.Double();

  /**
   * a Builder to create a configured instance
   *
   * @param <V> the vertex type
   * @param <E> the edge type
   * @param <T> the type that is built
   * @param <B> the builder type
   */
  public static class Builder<
          V,
          E,
          T extends TestEiglspergerLayoutAlgorithm<V, E> & EdgeAwareLayoutAlgorithm<V, E>,
          B extends Builder<V, E, T, B>>
      extends EiglspergerLayoutAlgorithm.Builder<V, E, T, B>
      implements LayoutAlgorithm.Builder<V, T, B> {
    boolean doUpLeft = false;
    boolean doDownLeft = false;
    boolean doUpRight = false;
    boolean doDownRight = false;

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
      return (T) new TestEiglspergerLayoutAlgorithm<>(this);
    }
  }

  /**
   * @param <V> vertex type
   * @param <E> edge type
   * @return a Builder ready to configure
   */
  public static <V, E> Builder<V, E, ?, ?> edgeAwareBuilder() {
    return new Builder<>();
  }

  boolean doUpLeft;
  boolean doDownLeft;
  boolean doUpRight;
  boolean doDownRight;

  public TestEiglspergerLayoutAlgorithm() {
    this(TestEiglspergerLayoutAlgorithm.edgeAwareBuilder());
  }

  protected TestEiglspergerLayoutAlgorithm(Builder builder) {
    super(builder);
    this.doUpLeft = builder.doUpLeft;
    this.doUpRight = builder.doUpRight;
    this.doDownLeft = builder.doDownLeft;
    this.doDownRight = builder.doDownRight;
  }

  @Override
  public void visit(LayoutModel<V> layoutModel) {

    Graph<V, E> graph = layoutModel.getGraph();
    if (graph == null || graph.vertexSet().isEmpty()) {
      return;
    }
    TestEiglspergerRunnable runnable =
        TestEiglspergerRunnable.<V, E>builder()
            .layoutModel(layoutModel)
            .vertexShapeFunction(vertexShapeFunction)
            .straightenEdges(straightenEdges)
            .transpose(transpose)
            .postStraighten(postStraighten)
            .maxLevelCross(maxLevelCross)
            .layering(layering)
            .doUpLeft(doUpLeft)
            .doUpRight(doUpRight)
            .doDownLeft(doDownLeft)
            .doDownRight(doDownRight)
            .build();
    if (threaded) {

      theFuture =
          CompletableFuture.runAsync(runnable)
              .thenRun(
                  () -> {
                    log.trace("Eiglsperger layout done");
                    this.edgePointMap.putAll(runnable.getEdgePointMap());
                    this.run(); // run the after function
                    layoutModel.getViewChangeSupport().fireViewChanged();
                    // fire an event to say that the layout is done
                    layoutModel
                        .getLayoutStateChangeSupport()
                        .fireLayoutStateChanged(layoutModel, false);
                  });
    } else {
      runnable.run();
      this.edgePointMap.putAll(runnable.getEdgePointMap());
      after.run();
      layoutModel.getViewChangeSupport().fireViewChanged();
      // fire an event to say that the layout is done
      layoutModel.getLayoutStateChangeSupport().fireLayoutStateChanged(layoutModel, false);
    }
    edgeShapeConsumer.accept(edgeShape);
  }
}
