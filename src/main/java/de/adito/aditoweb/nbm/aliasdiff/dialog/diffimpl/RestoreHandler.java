package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.EDirection;
import de.adito.propertly.core.spi.IProperty;
import org.jetbrains.annotations.NotNull;

/**
 * "Merkt" sich Datenmodelle oder Werte um sie für die restore Funktion
 * zur Verfügung zu stellen.
 * @author T.Tasior, 05.04.2018
 */
public class RestoreHandler
{
  private final EDirection direction;

  private final static Object NOT_SET = new String("NOT_SET");
  private Object propertly;
  private Object value;

  /**
   * Initialisierung mit der Seite die ggf. später wiederhergestellt werden soll.
   * @param pDirection markiert die Seite die restauriert werden soll.
   */
  public RestoreHandler(@NotNull EDirection pDirection)
  {
    direction = pDirection;

    propertly = NOT_SET;
    value = NOT_SET;
  }

  /**
   * Liefert die Seite die restauriert werden soll.
   * @return den Parameter des Konstruktors.
   */
  public EDirection getDirection()
  {
    return direction;
  }

  /**
   * Nimmt eine Datenmodell entgegen, oder null falls das der
   * Defaultzustand war.
   * 
   * @param pAny ein Datenmodell oder null.
   */
  public void setPropertly(Object pAny)
  {
    propertly = pAny;
  }

  /**
   * Liefert ein Datenmodell oder null
   * @return den "gemerkten" Zustand.
   */
  public Object getPropertly()
  {
    return propertly;
  }

  /**
   * Setzt den Wert eines Properties, z.B.: einen Integer, oder null.
   * @param pSomeValue den Wert der von einer IProperty Implementierung geliefert wird.
   * @see IProperty#getValue() 
   */
  public void setValue(Object pSomeValue)
  {
    value = pSomeValue;
  }

  /**
   * Liefert den "gemerkten" Wert eines Properties zurück.
   * @return einen beliebigen Wert, oder null.
   * @see IProperty#setValue(Object, Object...) ()
   */
  public Object getValue()
  {
    return value;
  }

  /**
   * Einfache Implementierung um festzustellen ob diesem Handler
   * der übergebene Wert gesetzt wurde.
   * 
   * @param pProband die Rückgabe aus getPropertly() oder getValue().
   * @return true, wenn sich dieser Handler das übergebene Objekt "merkt".
   */
  public final static boolean isSet(Object pProband)
  {
    return !(pProband == NOT_SET);
  }

}
