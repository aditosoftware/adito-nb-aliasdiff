package de.adito.aditoweb.nbm.aliasdiff.dialog;

import lombok.NonNull;

import javax.swing.tree.MutableTreeNode;
import java.util.List;

/**
 * Filter for {@link MutableTreeNode} instances
 *
 * @author T.Tasior, 27.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public interface ITreeNodeFilter
{

  /**
   * Returns the passed node, or a replacement implementation.
   *
   * @param pNode will be replaced by an implementation
   * @return the filtered implementation, or the input value
   */
  @NonNull
  MutableTreeNode filterNode(@NonNull MutableTreeNode pNode);

  /**
   * Determines, if the given node should be separated into a list of multiple nodes
   *
   * @param pChild may be replaced by the return value
   * @return the filtered implementations
   */
  @NonNull
  List<MutableTreeNode> filterChild(@NonNull MutableTreeNode pChild);

}
