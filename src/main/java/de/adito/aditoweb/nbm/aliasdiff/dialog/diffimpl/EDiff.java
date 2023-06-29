package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.EDirection;

/**
 * Contains all states of a diff
 *
 * @author t.tasior, 13.02.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 * @see AbstractPair#typeOfDiff(EDirection)
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
