package edu.wlu.cs.levy.CG;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import java.util.Stack;

/**
 * KDTree is a class supporting KD-tree insertion, deletion, equality search,
 * range search, and nearest neighbor(s) using double-precision floating-point
 * keys. Splitting dimension is chosen naively, by depth modulo K. Semantics are
 * as follows:
 *
 * <UL>
 * <LI>Two different keys containing identical numbers should retrieve the same
 * value from a given KD-tree. Therefore keys are cloned when a node is
 * inserted. <BR>
 * <BR>
 * <LI>As with Hashtables, values inserted into a KD-tree are <I>not</I> cloned.
 * Modifying a value between insertion and retrieval will therefore modify the
 * value stored in the tree.
 * </UL>
 *
 * Implements the Nearest Neighbor algorithm (Table 6.4) of
 *
 * <PRE>
 * &*064;techreport{AndrewMooreNearestNeighbor,
 *   author  = {Andrew Moore},
 *   title   = {An introductory tutorial on kd-trees},
 *   institution = {Robotics Institute, Carnegie Mellon University},
 *   year    = {1991},
 *   number  = {Technical Report No. 209, Computer Laboratory, 
 *              University of Cambridge},
 *   address = {Pittsburgh, PA}
 * }
 * </PRE>
 * 
 * Copyright (C) Simon D. Levy and Bjoern Heckel 2014
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <http:*www.gnu.org/licenses/>. You should
 * also have received a copy of the Parrot Parrot AR.Drone Development License
 * and Parrot AR.Drone copyright notice and disclaimer and If not, see
 * <https:*projects.ardrone.org/attachments/277/ParrotLicense.txt> and
 * <https:*projects
 * .ardrone.org/attachments/278/ParrotCopyrightAndDisclaimer.txt>.
 */
public class KDTree<T> implements Serializable {
  // number of milliseconds
  final long m_timeout;

  // K = number of dimensions
  final private int m_K;

  // root of KD-tree
  private KDNode<T> m_root;

  // count of nodes
  private int m_count;

  /**
   * Creates a KD-tree with specified number of dimensions.
   *
   * @param k
   *          number of dimensions
   */

  public KDTree(final int k) {
    this(k, 0);
  }
  public KDTree(final int k, final long timeout) {
    this.m_timeout = timeout;
    m_K = k;
    m_root = null;
  }

  /**
   * Insert a node in a KD-tree. Uses algorithm translated from 352.ins.c of
   *
   * <PRE>
   *   &*064;Book{GonnetBaezaYates1991,                                   
   *     author =    {G.H. Gonnet and R. Baeza-Yates},
   *     title =     {Handbook of Algorithms and Data Structures},
   *     publisher = {Addison-Wesley},
   *     year =      {1991}
   *   }
   * </PRE>
   *
   * @param key
   *          key for KD-tree node
   * @param value
   *          value at that key
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   * @throws KeyDuplicateException
   *           if key already in tree
   */
  public void insert(final double[] key, final T value) throws KeySizeException, KeyDuplicateException {
    this.edit(key, new Editor.Inserter<T>(value));
  }

  /**
   * Edit a node in a KD-tree
   *
   * @param key
   *          key for KD-tree node
   * @param editor
   *          object to edit the value at that key
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   * @throws KeyDuplicateException
   *           if key already in tree
   */

  public void edit(final double[] key, final Editor<T> editor) throws KeySizeException, KeyDuplicateException {

    if (key.length != m_K) {
      throw new KeySizeException();
    }

    synchronized (this) {
      // the first insert has to be synchronized
      if (null == m_root) {
        m_root = KDNode.create(new HPoint(key), editor);
        m_count = m_root.deleted ? 0 : 1;
        return;
      }
    }

    m_count += KDNode.edit(new HPoint(key), editor, m_root, 0, m_K);
  }

  /**
   * Find KD-tree node whose key is identical to key. Uses algorithm translated
   * from 352.srch.c of Gonnet & Baeza-Yates.
   *
   * @param key
   *          key for KD-tree node
   *
   * @return object at key, or null if not found
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   */
  public T search(final double[] key) throws KeySizeException {

    if (key.length != m_K) {
      throw new KeySizeException();
    }

    final KDNode<T> kd = KDNode.srch(new HPoint(key), m_root, m_K);

    return (kd == null ? null : kd.v);
  }

  public void delete(final double[] key) throws KeySizeException, KeyMissingException {
    delete(key, false);
  }
  /**
   * Delete a node from a KD-tree. Instead of actually deleting node and
   * rebuilding tree, marks node as deleted. Hence, it is up to the caller to
   * rebuild the tree as needed for efficiency.
   *
   * @param key
   *          key for KD-tree node
   * @param optional
   *          if false and node not found, throw an exception
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   * @throws KeyMissingException
   *           if no node in tree has key
   */
  public void delete(final double[] key, final boolean optional) throws KeySizeException, KeyMissingException {

    if (key.length != m_K) {
      throw new KeySizeException();
    }
    final KDNode<T> t = KDNode.srch(new HPoint(key), m_root, m_K);
    if (t == null) {
      if (optional == false) {
        throw new KeyMissingException();
      }
    } else {
      if (KDNode.del(t)) {
        m_count--;
      }
    }
  }

  /**
   * Find KD-tree node whose key is nearest neighbor to key.
   *
   * @param key
   *          key for KD-tree node
   *
   * @return object at node nearest to key, or null on failure
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   */
  public T nearest(final double[] key) throws KeySizeException {

    final List<T> nbrs = nearest(key, 1, null);
    return nbrs.get(0);
  }

  /**
   * Find KD-tree nodes whose keys are <i>n</i> nearest neighbors to key.
   *
   * @param key
   *          key for KD-tree node
   * @param n
   *          number of nodes to return
   *
   * @return objects at nodes nearest to key, or null on failure
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   */
  public List<T> nearest(final double[] key, final int n) throws KeySizeException, IllegalArgumentException {
    return nearest(key, n, null);
  }

  /**
   * Find KD-tree nodes whose keys are within a given Euclidean distance of a
   * given key.
   *
   * @param key
   *          key for KD-tree node
   * @param d
   *          Euclidean distance
   *
   * @return objects at nodes with distance of key, or null on failure
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   */
  public List<T> nearestEuclidean(final double[] key, final double dist) throws KeySizeException {
    return nearestDistance(key, dist, new EuclideanDistance());
  }

  /**
   * Find KD-tree nodes whose keys are within a given Hamming distance of a
   * given key.
   *
   * @param key
   *          key for KD-tree node
   * @param d
   *          Hamming distance
   *
   * @return objects at nodes with distance of key, or null on failure
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   */
  public List<T> nearestHamming(final double[] key, final double dist) throws KeySizeException {

    return nearestDistance(key, dist, new HammingDistance());
  }

  /**
   * Find KD-tree nodes whose keys are <I>n</I> nearest neighbors to key. Uses
   * algorithm above. Neighbors are returned in ascending order of distance to
   * key.
   *
   * @param key
   *          key for KD-tree node
   * @param n
   *          how many neighbors to find
   * @param checker
   *          an optional object to filter matches
   *
   * @return objects at node nearest to key, or null on failure
   *
   * @throws KeySizeException
   *           if key.length mismatches K
   * @throws IllegalArgumentException
   *           if <I>n</I> is negative or exceeds tree size
   */
  public List<T> nearest(final double[] key, int n, final Checker<T> checker) throws KeySizeException, IllegalArgumentException {

    if (n <= 0) {
      return new LinkedList<T>();
    }

    final NearestNeighborList<KDNode<T>> nnl = getnbrs(key, n, checker);

    n = nnl.getSize();
    final Stack<T> nbrs = new Stack<T>();

    for (int i = 0; i < n; ++i) {
      final KDNode<T> kd = nnl.removeHighest();
      nbrs.push(kd.v);
    }

    return nbrs;
  }

  /**
   * Range search in a KD-tree. Uses algorithm translated from 352.range.c of
   * Gonnet & Baeza-Yates.
   *
   * @param lowk
   *          lower-bounds for key
   * @param uppk
   *          upper-bounds for key
   *
   * @return array of Objects whose keys fall in range [lowk,uppk]
   *
   * @throws KeySizeException
   *           on mismatch among lowk.length, uppk.length, or K
   */
  public List<T> range(final double[] lowk, final double[] uppk) throws KeySizeException {

    if (lowk.length != uppk.length) {
      throw new KeySizeException();
    }

    else if (lowk.length != m_K) {
      throw new KeySizeException();
    }

    else {
      final List<KDNode<T>> found = new LinkedList<KDNode<T>>();
      KDNode.rsearch(new HPoint(lowk), new HPoint(uppk), m_root, 0, m_K, found);
      final List<T> o = new LinkedList<T>();
      for (final KDNode<T> node : found) {
        o.add(node.v);
      }
      return o;
    }
  }

  public int size() { /* added by MSL */
    return m_count;
  }

  public String toString() {
    return m_root != null ? m_root.toString(0) : "";
  }

  private NearestNeighborList<KDNode<T>> getnbrs(final double[] key) throws KeySizeException {
    return getnbrs(key, m_count, null);
  }

  private NearestNeighborList<KDNode<T>> getnbrs(final double[] key, final int n, final Checker<T> checker) throws KeySizeException {

    if (key.length != m_K) {
      throw new KeySizeException();
    }

    final NearestNeighborList<KDNode<T>> nnl = new NearestNeighborList<KDNode<T>>(n);

    // initial call is with infinite hyper-rectangle and max distance
    final HRect hr = HRect.infiniteHRect(key.length);
    final double max_dist_sqd = Double.MAX_VALUE;
    final HPoint keyp = new HPoint(key);

    if (m_count > 0) {
      final long timeout = (this.m_timeout > 0) ? (System.currentTimeMillis() + this.m_timeout) : 0;
      KDNode.nnbr(m_root, keyp, hr, max_dist_sqd, 0, m_K, nnl, checker, timeout);
    }

    return nnl;

  }

  private List<T> nearestDistance(final double[] key, final double dist, final DistanceMetric metric) throws KeySizeException {

    final NearestNeighborList<KDNode<T>> nnl = getnbrs(key);
    final int n = nnl.getSize();
    final Stack<T> nbrs = new Stack<T>();

    for (int i = 0; i < n; ++i) {
      final KDNode<T> kd = nnl.removeHighest();
      final HPoint p = kd.k;
      if (metric.distance(kd.k.coord, key) < dist) {
        nbrs.push(kd.v);
      }
    }

    return nbrs;
  }

}
