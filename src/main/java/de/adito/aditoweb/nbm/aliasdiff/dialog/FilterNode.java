package de.adito.aditoweb.nbm.aliasdiff.dialog;

import org.jetbrains.annotations.NotNull;

import javax.swing.tree.*;
import java.util.ArrayList;

/**
 * Implementierung für die Darstellung im JTree
 * mit der Möglichkeit zur Filterung.
 * @author T.Tasior, 27.03.2018
 */
public class FilterNode extends DefaultMutableTreeNode
{
  /**
   * Speichert den übergebenen Node und lässt mithilfe des Filters
   * desen Kinder filtern.
   * @param pNode wird als Userobjekt gehalten.
   * @param pFilter zum filtern der Kinder von pNode.
   */
  public FilterNode(@NotNull MutableTreeNode pNode,@NotNull ITreeNodeFilter pFilter)
  {
    setUserObject(pNode);

    ArrayList<MutableTreeNode> collector = new ArrayList<>();
    for (int i = 0; i < pNode.getChildCount(); i++)
    {
      MutableTreeNode child = (MutableTreeNode) pNode.getChildAt(i);
      ArrayList<MutableTreeNode> children = pFilter.filterChild(child);
      collector.addAll(children);
    }

    for (MutableTreeNode child : collector)
    {
      add(child);
    }
  }
}
