package org.jungrapht.visualization.layout.algorithms.eiglsperger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jungrapht.visualization.layout.algorithms.sugiyama.LE;
import org.jungrapht.visualization.layout.algorithms.sugiyama.LV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * initialize for forward sweep (top to bottom)
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public class EiglspergerStepsForward<V, E> extends EiglspergerSteps<V, E> {

  private static final Logger log = LoggerFactory.getLogger(EiglspergerStepsForward.class);

  public EiglspergerStepsForward(
      Graph<LV<V>, LE<V, E>> svGraph, LV<V>[][] layersArray, boolean transpose) {
    super(
        svGraph,
        layersArray,
        PVertex.class::isInstance,
        QVertex.class::isInstance,
        svGraph::getEdgeSource,
        svGraph::getEdgeTarget,
        Graphs::predecessorListOf,
        e -> e,
        transpose);
  }

  public int sweep(LV<V>[][] layersArray) {

    if (log.isTraceEnabled())
      log.trace(">>>>>>>>>>>>>>>>>>>>>>>>> Forward!>>>>>>>>>>>>>>>>>>>>>>>>>");
    int crossCount = 0;

    List<LV<V>> layerEye = null;

    for (int i = 0; i < layersArray.length - 1; i++) {
      if (layerEye == null) {
        layerEye =
            EiglspergerUtil.scan(
                EiglspergerUtil.createListOfVertices(layersArray[i])); // first rank
      }

      stepOne(layerEye);
      // handled PVertices by merging them into containers
      if (log.isTraceEnabled()) {
        log.trace("stepOneOut:{}", layerEye);
      }

      List<LV<V>> currentLayer = layerEye;
      List<LV<V>> downstreamLayer = EiglspergerUtil.createListOfVertices(layersArray[i + 1]);
      stepTwo(currentLayer, downstreamLayer);
      if (log.isTraceEnabled()) {
        log.trace("stepTwoOut:{}", downstreamLayer);
      }

      stepThree(downstreamLayer);
      if (log.isTraceEnabled()) {
        log.trace("stepThreeOut:{}", downstreamLayer);
      }
      EiglspergerUtil.fixIndices(downstreamLayer);

      stepFour(downstreamLayer, i + 1);
      if (log.isTraceEnabled()) {
        log.trace("stepFourOut:{}", downstreamLayer);
      }

      if (transpose) {
        crossCount += stepFive(currentLayer, downstreamLayer, i, i + 1);
      }
      stepSix(downstreamLayer);
      if (log.isTraceEnabled()) {
        log.trace("stepSixOut:{}", downstreamLayer);
      }

      Arrays.sort(layersArray[i], Comparator.comparingInt(LV::getIndex));
      EiglspergerUtil.fixIndices(layersArray[i]);
      Arrays.sort(layersArray[i + 1], Comparator.comparingInt(LV::getIndex));
      EiglspergerUtil.fixIndices(layersArray[i + 1]);
      layerEye = downstreamLayer;
    }
    log.info("sweepForward crossCount:{}", crossCount);
    return crossCount;
  }
}
