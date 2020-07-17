package org.jungrapht.visualization.layout.algorithms.util;

/** Used to cause a LayoutAlgorithm to call vv::scaleToLayout once it has completed */
public interface AfterRunnable {

  void setAfter(Runnable after);

  void runAfter();
}
