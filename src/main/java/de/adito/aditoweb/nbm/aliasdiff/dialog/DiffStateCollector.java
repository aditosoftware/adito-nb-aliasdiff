package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.EDiff;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.DiffIcon;

/**
 * Hilfsklasse zum sammeln der Diff Stati in den einzelnen Knoten.
 * Konfiguriert das Icon im Baum mit denn anzuzeigenden Farben.
 *
 * @author t.tasior, 09.03.2018
 * @see DiffIcon
 */
public class DiffStateCollector
{
  EDiff equal;
  EDiff different;
  EDiff missing;
  EDiff deleted;

  /**
   * Liefert true wenn einer der Stati EDiff.EQUAL
   * oder EDiff.DELETED gesetzt wurde.
   */
  public boolean canRestore()
  {
    return EDiff.EQUAL.equals(equal) | EDiff.DELETED.equals(deleted);
  }

  /**
   * Konfiguriert das Icon entsprechend der gesetzten Diff Stati.
   * @param pIcon für die Anzeige im Baum.
   */
  public void update(DiffIcon pIcon)
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
   * Bringt den Kollektur in den initialen Zustand damit erneut
   * Stati gesammelt werden können.
   * @return sich selbst im Ursprungszustand.
   */
  public DiffStateCollector reset()
  {
    equal = null;
    different = null;
    missing = null;
    deleted = null;

    return this;
  }


}
