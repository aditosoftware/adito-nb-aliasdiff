package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.EDirection;

/**
 * Enthält die verschiedenen Diff Zustände.
 * @see AbstractPair#typeOfDiff(EDirection) 
 * @author t.tasior, 13.02.2018
 */
public enum EDiff
{
  EQUAL,
  DIFFERENT,
  MISSING,
  DELETED,
  NOT_EVALUATED,
  BOTH_PRESENT;
}
