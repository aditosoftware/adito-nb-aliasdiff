package de.adito.aditoweb.nbm.aliasdiff.dialog;

import org.jetbrains.annotations.NotNull;

/**
 * Entscheidet ob eine Datenstruktur aktualisiert werden darf, oder nicht.
 *
 * @author T.Tasior, 03.04.2018
 */
public interface IUpdateHandler
{
  boolean canUpdate(@NotNull EDirection pDirection, IDiffNode pNode, boolean isRemote);

  final class DefaultHandler implements IUpdateHandler
  {
    @Override
    public boolean canUpdate(@NotNull EDirection pDirection, IDiffNode pNode, boolean isRemote)
    {
      return true;
    }
  }
}
