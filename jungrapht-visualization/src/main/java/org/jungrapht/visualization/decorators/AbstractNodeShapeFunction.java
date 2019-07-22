/*
 * Created on Jul 16, 2004
 *
 * Copyright (c) 2004, The JUNG Authors
 *
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * https://github.com/tomnelson/jungrapht-visualization/blob/master/LICENSE for a description.
 */
package org.jungrapht.visualization.decorators;

import java.util.function.Function;
import org.jungrapht.visualization.util.NodeShapeFactory;

/** @author Joshua O'Madadhain */
public abstract class AbstractNodeShapeFunction<N> implements SettableNodeShapeFunction<N> {
  protected Function<N, Integer> vsf;
  protected Function<N, Float> varf;
  protected NodeShapeFactory<N> factory;
  public static final int DEFAULT_SIZE = 8;
  public static final float DEFAULT_ASPECT_RATIO = 1.0f;

  public AbstractNodeShapeFunction(Function<N, Integer> vsf, Function<N, Float> varf) {
    this.vsf = vsf;
    this.varf = varf;
    factory = new NodeShapeFactory<>(vsf, varf);
  }

  public AbstractNodeShapeFunction() {
    this(n -> DEFAULT_SIZE, n -> DEFAULT_ASPECT_RATIO);
  }

  public void setSizeTransformer(Function<N, Integer> vsf) {
    this.vsf = vsf;
    factory = new NodeShapeFactory<>(vsf, varf);
  }

  public void setAspectRatioTransformer(Function<N, Float> varf) {
    this.varf = varf;
    factory = new NodeShapeFactory<>(vsf, varf);
  }
}
