package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.EnumSet;

/**
 * Determines the next / previous element in the tree, containing a difference
 *
 * @author T.Tasior, 12.04.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
class NavigationHandler
{
  private final EnumSet<EDiff> diffStates;
  private final IDiffNode root;
  private final Memory mem;

  /**
   * @param pRoot the tree that should be navigated
   */
  public NavigationHandler(IDiffNode pRoot)
  {
    root = pRoot;
    diffStates = EnumSet.of(EDiff.DIFFERENT, EDiff.MISSING, EDiff.DELETED);
    mem = new Memory();
  }

  /**
   * Finds a node with a difference in the tree structure upwards.
   *
   * @param pNode if set, the calculation is performed by this node.
   * @return a node with difference, or null if there are no differences.
   */
  @Nullable
  public TreeNode previous(@Nullable IDiffNode pNode)
  {
    if (root.countDifferences() == 0)
      return null;

    TreeNode node = pNode;

    if (node != null)
    {
      mem.reset();
      if (node.isLeaf() && isDifferent(((IDiffNode) node).getPair()))
      {
        mem.set(node, indexOf(node));
        return findPreviousIn(node);
      }
    }

    if (node == null)
      node = mem.difference;

    if (node == null)
      node = findLastNode();

    return node == null ? null : findPreviousIn(node);
  }

  /**
   * Finds a node with a difference in the tree structure downwards.
   *
   * @param pNode if set, the calculation is performed by this node.
   * @return a node with difference, or null if there are no differences.
   */
  @Nullable
  public TreeNode next(@Nullable IDiffNode pNode)
  {
    if (root.countDifferences() == 0)
      return null;

    TreeNode node = pNode;

    if (node != null)
    {
      mem.reset();
      if (node.isLeaf() && isDifferent(((IDiffNode) node).getPair()))
      {
        mem.set(node, indexOf(node));
        return findNextIn(node);
      }
    }

    if (node == null)
      node = mem.difference;

    if (node == null)
      node = findFirstNode();

    return node == null ? null : findNextIn(node);
  }

  /**
   * Searches the next difference in the given node.
   * Will be recursive.
   *
   * @param pNode Node to search in
   * @return the next difference or null, if nothing found
   */
  @Nullable
  private TreeNode findNextIn(@NonNull TreeNode pNode)
  {
    TreeNode node;
    int startIndex;
    if (mem.difference == null)
    {
      startIndex = 0;
      node = pNode;
    }
    else
    {
      startIndex = mem.indexInParent + 1;
      node = mem.difference.getParent();
    }

    for (int index = startIndex; index < node.getChildCount(); index++)
    {
      TreeNode child = node.getChildAt(index);

      if (child.isLeaf())
      {
        if (isDifferent(((IDiffNode) child).getPair()))
        {
          mem.set(child, index);
          return child;
        }
      }
      else
        return findNextIn(child);
    }

    mem.reset();
    TreeNode parent = findNextParent(node);

    if (parent != null)
      return findNextIn(parent);

    return findNextIn(root);
  }


  /**
   * Searches the previous difference in the given node.
   * Will be recursive.
   *
   * @param pNode Node to search in
   * @return the previous difference or null, if nothing found
   */
  @Nullable
  private TreeNode findPreviousIn(@NonNull TreeNode pNode)
  {
    TreeNode node;
    int startIndex;
    if (mem.difference == null)
    {
      node = pNode;
      startIndex = node.getChildCount() - 1;
    }
    else
    {
      startIndex = mem.indexInParent - 1;
      node = mem.difference.getParent();
    }

    for (int index = startIndex; index >= 0; index--)
    {
      TreeNode child = node.getChildAt(index);

      if (child.isLeaf())
      {
        if (isDifferent(((IDiffNode) child).getPair()))
        {
          mem.set(child, index);
          return child;
        }
      }
      else
        return findPreviousIn(child);
    }

    mem.reset();
    TreeNode parent = findPreviousParent(node);
    if (parent != null)
      return findPreviousIn(parent);

    return findPreviousIn(root);
  }

  /**
   * Searches the index of the given child node.
   *
   * @param pChild Child to search for
   * @return the index, or -1 if not found
   */
  private int indexOf(@NonNull TreeNode pChild)
  {
    int index = -1;
    TreeNode p = pChild.getParent();
    for (int i = 0; i < p.getChildCount(); i++)
    {
      if (p.getChildAt(i) == pChild)
      {
        index = i;
        break;
      }
    }
    return index;
  }

  /**
   * Searches the next node in parent
   *
   * @param pChild Child to indicate the current position
   * @return the next node or the root node
   */
  @Nullable
  private TreeNode findNextParent(@NonNull TreeNode pChild)
  {
    TreeNode p = pChild.getParent();

    if (p == null)
      return root;

    int index = indexOf(pChild) + 1;
    if (index < p.getChildCount())
      return p.getChildAt(index);

    return findNextParent(p);
  }

  /**
   * Searches the previous node in parent
   *
   * @param pChild Child to indicate the current position
   * @return the previous node or the root node
   */
  @Nullable
  private TreeNode findPreviousParent(@NonNull TreeNode pChild)
  {
    TreeNode p = pChild.getParent();

    if (p == null)
      return root;

    int index = indexOf(pChild) - 1;
    if (index >= 0)
      return p.getChildAt(index);

    return findPreviousParent(p);
  }

  /**
   * @return the first node of the root node, or null if the root has no children
   */
  @Nullable
  private IDiffNode findFirstNode()
  {
    if (root.getChildCount() > 0)
      return (IDiffNode) root.getChildAt(0);

    return null;
  }

  /**
   * @return the last node of the root node, or null if the root has no children
   */
  @Nullable
  private IDiffNode findLastNode()
  {
    int childCount = root.getChildCount();
    if (childCount > 0)
      return (IDiffNode) root.getChildAt(childCount - 1);
    return null;
  }

  /**
   * Determines, if the given pair is in a "different" state
   *
   * @param pPair Pair to check
   * @return true, if it is in a "different" state
   */
  private boolean isDifferent(@Nullable AbstractPair pPair)
  {
    if (pPair != null)
      return diffStates.contains(pPair.typeOfDiff(EDirection.LEFT)) ||
          diffStates.contains(pPair.typeOfDiff(EDirection.RIGHT));

    return false;
  }

  /**
   * Helper, remembers the last delivered node with differences,
   * as well as its index in its parent.
   */
  private static class Memory
  {
    private TreeNode difference;
    private int indexInParent = -1;

    /**
     * Sets the given node and the appropriate index
     *
     * @param pNode          Node to set
     * @param pIndexInParent Index to set, -1 if no index available
     */
    public void set(@Nullable TreeNode pNode, int pIndexInParent)
    {
      difference = pNode;
      indexInParent = pIndexInParent;
    }

    /**
     * Resets everything inside
     */
    public void reset()
    {
      difference = null;
      indexInParent = -1;
    }
  }

}
