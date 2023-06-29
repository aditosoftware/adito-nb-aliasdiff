package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.EDirection;
import org.jetbrains.annotations.NotNull;

/**
 * Unterstützt das Wiederherstellen von Werten auf Propertyebene.
 *
 * @author t.tasior, 14.03.2018
 */
class PropertyRestoreHandler
{
  private Boolean leftValueWasProvided = null;
  private Object leftValue = null;

  private Boolean rightValueWasProvided = null;
  private Object rightValue = null;

  /**
   * Speichert den Ursprungswert eines Properties.
   *
   * @param pDirection identifiziert das Property.
   * @param pValue     der Wert des Properties.
   */
  public void storeValue(@NotNull EDirection pDirection, Object pValue)
  {
    if (pDirection == EDirection.LEFT & leftValueWasProvided == null)
    {
      leftValue = pValue;
      leftValueWasProvided = true;
    }

    if (pDirection == EDirection.RIGHT & rightValueWasProvided == null)
    {
      rightValue = pValue;
      rightValueWasProvided = true;
    }
  }

  /**
   * Liefert den Ursprungswert wieder zurück.
   *
   * @param pDirection identifiziert die Seite die restauriert werden soll.
   * @return den ursprünglichen Wert.
   */
  public Object restoreValue(@NotNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT & leftValueWasProvided != null)
    {
      leftValueWasProvided = null;
      return leftValue;
    }

    if (pDirection == EDirection.RIGHT & rightValueWasProvided != null)
    {
      rightValueWasProvided = null;
      return rightValue;
    }

    return null;
  }

  /**
   * Liefert true, wenn ein Wert auf der angegebenen Seite überschrieben wurde.
   * @param pDirection die Seite die überschrieben wurde.
   * @return true wenn etwas wiederhergestellt werden kann.
   */
  public boolean wasValueProvided(@NotNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT & Boolean.TRUE.equals(leftValueWasProvided))
    {
      return true;
    }

    if (pDirection == EDirection.RIGHT & Boolean.TRUE.equals(rightValueWasProvided))
    {
      return true;
    }

    return false;
  }


}
