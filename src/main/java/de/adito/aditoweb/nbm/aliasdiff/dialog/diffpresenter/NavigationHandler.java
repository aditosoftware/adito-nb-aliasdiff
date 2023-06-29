package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.EnumSet;

/**
 * Ermittelt das nächste / vorherige Element im Baum mit einer Differenz.
 *
 * @author T.Tasior, 12.04.2018
 */
class NavigationHandler
{
  private final EnumSet<EDiff> diffStates;
  private final IDiffNode root;
  private final _Memory mem;

  /**
   * Initialisierung mit der Wurzel des Baumes.
   *
   * @param pRoot der Baum durch den navigiert werden soll.
   */
  public NavigationHandler(IDiffNode pRoot)
  {
    root = pRoot;
    diffStates = EnumSet.of(EDiff.DIFFERENT, EDiff.MISSING, EDiff.DELETED);
    mem = new _Memory();
  }

  /**
   * Ermittelt einen Node mit einer Differenz in der Baumstruktur aufwärts.
   *
   * @param pNode falls gesetzt, wird von diesem Node die Berechnung durchgeführt.
   * @return einen Node mit Differenz, oder null wenn es keine Differenzen gibt.
   */
  public TreeNode previous(@Nullable IDiffNode pNode)
  {
    if (root.countDifferences() == 0)
      return null;

    TreeNode node = pNode;

    if (node != null)
    {
      mem.reset();
      if (node.isLeaf() && _isDifferent(((IDiffNode) node).getPair()))
      {
        mem.set(node, _indexOf(node));
        return _findPrevoiusIn(node);
      }
    }

    if (node == null)
      node = mem.difference;

    if (node == null)
      node = _findLastNode();

    return _findPrevoiusIn(node);
  }

  /**
   * Ermittelt einen Node mit einer Differenz in der Baumstruktur abwärts.
   *
   * @param pNode falls gesetzt, wird von diesem Node die Berechnung durchgeführt.
   * @return einen Node mit Differenz, oder null wenn es keine Differenzen gibt.
   */
  public TreeNode next(IDiffNode pNode)
  {
    if (root.countDifferences() == 0)
      return null;

    TreeNode node = pNode;

    if (node != null)
    {
      mem.reset();
      if (node.isLeaf() && _isDifferent(((IDiffNode) node).getPair()))
      {
        mem.set(node, _indexOf(node));
        return _findNextIn(node);
      }
    }

    if (node == null)
      node = mem.difference;

    if (node == null)
      node = _findFirstNode();

    return _findNextIn(node);
  }

  private TreeNode _findNextIn(TreeNode pNode)
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
        if (_isDifferent(((IDiffNode) child).getPair()))
        {
          mem.set(child, index);
          return child;
        }
      }
      else
        return _findNextIn(child);
    }

    mem.reset();
    TreeNode parent = _findNextParent(node);

    if (parent != null)
      return _findNextIn(parent);

    return _findNextIn(root);
  }

  private TreeNode _findPrevoiusIn(TreeNode pNode)
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
        if (_isDifferent(((IDiffNode) child).getPair()))
        {
          mem.set(child, index);
          return child;
        }
      }
      else
        return _findPrevoiusIn(child);
    }

    mem.reset();
    TreeNode parent = _findPrevoiusParent(node);
    if (parent != null)
      return _findPrevoiusIn(parent);

    return _findPrevoiusIn(root);
  }

  private int _indexOf(TreeNode pChild)
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

  private TreeNode _findNextParent(TreeNode pChild)
  {
    TreeNode p = pChild.getParent();

    if (p == null)
    {
      return root;
    }

    int index = _indexOf(pChild) + 1;

    if (index < p.getChildCount())
      return p.getChildAt(index);

    return _findNextParent(p);
  }

  private TreeNode _findPrevoiusParent(TreeNode pChild)
  {
    TreeNode p = pChild.getParent();

    if (p == null)
    {
      return root;
    }

    int index = _indexOf(pChild) - 1;

    if (index >= 0)
      return p.getChildAt(index);


    return _findPrevoiusParent(p);
  }

  private IDiffNode _findFirstNode()
  {
    if (root.getChildCount() > 0)
      return (IDiffNode) root.getChildAt(0);

    return null;
  }

  private IDiffNode _findLastNode()
  {
    int childCount = root.getChildCount();
    if (childCount > 0)
      return (IDiffNode) root.getChildAt(childCount - 1);

    return null;
  }

  private boolean _isDifferent(AbstractPair pPair)
  {
    if (pPair != null)
      return diffStates.contains(pPair.typeOfDiff(EDirection.LEFT)) | diffStates.contains(pPair.typeOfDiff(EDirection.RIGHT));

    return false;
  }

  /**
   * Hilfskonstrukt, merkt sich den zuletzt gelieferten Node mit Differenz,
   * sowie dessen index in seinem Parent.
   */
  private static class _Memory
  {
    public TreeNode difference;
    public int indexInParent = -1;

    public void set(TreeNode pNode, int pIndexInParent)
    {
      difference = pNode;
      indexInParent = pIndexInParent;
    }

    public void reset()
    {
      difference = null;
      indexInParent = -1;
    }
  }

}
