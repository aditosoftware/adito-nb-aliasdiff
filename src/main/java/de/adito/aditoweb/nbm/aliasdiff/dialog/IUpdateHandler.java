package de.adito.aditoweb.nbm.aliasdiff.dialog;

import lombok.NonNull;

/**
 * Determines, if a child structure may be updated or not
 *
 * @author T.Tasior, 03.04.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public interface IUpdateHandler
{
  /**
   * Dummy Implementation, that always returns true
   */
  IUpdateHandler DEFAULT = (pDirection, pNode, isRemote) -> true;

  /**
   * Determines, if the given child on the given side can be updated
   *
   * @param pDirection Side on which the node is located
   * @param pNode      Node to check
   * @param isRemote   true, if the node is on a remote side
   * @return true, if it can be updated
   */
  boolean canUpdate(@NonNull EDirection pDirection, IDiffNode pNode, boolean isRemote);

}
