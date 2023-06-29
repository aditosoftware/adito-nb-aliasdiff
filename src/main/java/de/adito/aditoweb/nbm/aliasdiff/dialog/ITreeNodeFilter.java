package de.adito.aditoweb.nbm.aliasdiff.dialog;

import org.jetbrains.annotations.NotNull;

import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;

/**
 * Filter für MutableTreeNode Implementierungen.
 * @author T.Tasior, 27.03.2018
 */
public interface ITreeNodeFilter
{
  /**
   * Liefert den übergebenen Node zurück, oder eine Ersatzimplementierung.
   * @param pNode wird evtl. gegen eine andere Implementierung ersetzt.
   * @return den Parameter, oder eine Ersatzimplementierung.
   */
  MutableTreeNode filterNode(@NotNull MutableTreeNode pNode);

  /**
   * Untersucht pCild und liefert diesen zurück, oder eine Liste anderer Nodes,
   * oder ein leeres Array.
   * @param pChild wird evtl. durch andere Nodes ersetzt.
   * @return niemals null.
   */
  ArrayList<MutableTreeNode> filterChild(@NotNull MutableTreeNode pChild);

  /**
   * Hilfsmethode zum iterieren über die Kinder des übergebenen Nodes.
   * @param pNode
   * @return
   */
  default ArrayList<MutableTreeNode> collectChildren(@NotNull MutableTreeNode pNode)
  {
    ArrayList<MutableTreeNode> list = new ArrayList<>();
    for (int i = 0; i < pNode.getChildCount(); i++)
    {
      list.add((MutableTreeNode) pNode.getChildAt(i));
    }
    return list;
  }
}
