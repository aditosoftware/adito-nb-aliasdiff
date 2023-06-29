package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.EDiff;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.DiffIcon;
import lombok.NonNull;

/**
 * Helper class for collecting the diff statuses in each node.
 * Configures the icon in the tree with the colors to be displayed.
 *
 * @author t.tasior, 09.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 * @see DiffIcon
 */
public class DiffStateCollector
{
  EDiff equal;
  EDiff different;
  EDiff missing;
  EDiff deleted;

  /**
   * Returns true, if one of the statuses i EQUAL or DELETED
   */
  public boolean canRestore()
  {
    return EDiff.EQUAL.equals(equal) || EDiff.DELETED.equals(deleted);
  }

  /**
   * Updates the icon based on the current statuses
   *
   * @param pIcon the icon inside a tree
   */
  public void update(@NonNull DiffIcon pIcon)
  {
    pIcon.reset();
    if (equal != null)
      pIcon.setEqualColor();

    if (different != null)
      pIcon.setDifferentColor();

    if (missing != null)
      pIcon.setMissingColor();

    if (deleted != null)
      pIcon.setDeletedColor();
  }

  /**
   * Resets every status inside, so it seems untouched afterwards
   */
  public void reset()
  {
    equal = null;
    different = null;
    missing = null;
    deleted = null;
  }


}
