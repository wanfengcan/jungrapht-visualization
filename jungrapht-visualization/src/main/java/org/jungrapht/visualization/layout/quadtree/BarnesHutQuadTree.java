package org.jungrapht.visualization.layout.quadtree;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A QuadTree that can gather combined forces from visited nodes. Inspired by
 * http://arborjs.org/docs/barnes-hut
 * http://www.cs.princeton.edu/courses/archive/fall03/cs126/assignments/barnes-hut.html
 * https://github.com/chindesaurus/BarnesHut-N-Body
 *
 * @author Tom Nelson
 */
public class BarnesHutQuadTree<T> {

  private static final Logger log = LoggerFactory.getLogger(BarnesHutQuadTree.class);

  /** the root node of the quad tree */
  private Node<T> root;

  public static class Builder<T> {
    protected double theta = Node.DEFAULT_THETA;
    protected Rectangle bounds;

    public BarnesHutQuadTree.Builder bounds(Rectangle bounds) {
      this.bounds = bounds;
      return this;
    }

    public BarnesHutQuadTree.Builder bounds(double x, double y, double width, double height) {
      bounds(new Rectangle(x, y, width, height));
      return this;
    }

    public BarnesHutQuadTree.Builder bounds(double width, double height) {
      bounds(new Rectangle(0, 0, width, height));
      return this;
    }

    public BarnesHutQuadTree.Builder theta(double theta) {
      this.theta = theta;
      return this;
    }

    public BarnesHutQuadTree<T> build() {
      return new BarnesHutQuadTree(this);
    }
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }
  /**
   * the bounds of this quad tree
   *
   * @return the bounds of the entire quadtree
   */
  public Rectangle getBounds() {
    return root.getBounds();
  }

  /** @return the root {@code Node} of this tree */
  public Node<T> getRoot() {
    return root;
  }

  private final Object lock = new Object();

  private BarnesHutQuadTree(Builder<T> builder) {
    this.root = Node.<T>builder().withArea(builder.bounds).withTheta(builder.theta).build();
  }

  /*
   * Clears the quadtree
   */
  public void clear() {
    root.clear();
  }

  /**
   * @param visitor passed {@code ForceObject} will visit nodes in the quad tree and accumulate
   *     their forces
   */
  public void applyForcesTo(ForceObject<T> visitor) {
    Preconditions.checkArgument(visitor != null, "Cannot apply forces to a null ForceObject");
    if (root != null && root.forceObject != visitor) {
      root.applyForcesTo(visitor);
    }
  }

  /*
   * Insert the object into the quadtree. If the node exceeds the capacity, it
   * will split and add all objects to their corresponding nodes.
   * @param node the {@code ForceObject} to insert
   */
  protected void insert(ForceObject node) {
    synchronized (lock) {
      root.insert(node);
    }
  }

  /**
   * rebuild the quad tree with the nodes and location mappings of the passed LayoutModel
   *
   * @param locations - mapping of elements to locations
   */
  public void rebuild(Map<T, Point> locations) {
    clear();
    synchronized (lock) {
      for (Map.Entry<T, Point> entry : locations.entrySet()) {
        ForceObject<T> forceObject = new ForceObject<T>(entry.getKey(), entry.getValue());
        insert(forceObject);
      }
    }
  }

  @Override
  public String toString() {
    return "Tree:" + root;
  }
}