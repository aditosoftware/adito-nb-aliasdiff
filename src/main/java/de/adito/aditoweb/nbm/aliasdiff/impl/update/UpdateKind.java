package de.adito.aditoweb.nbm.aliasdiff.impl.update;

/**
 * Definiert den Änderungstyp für Struktur-nach-DB-Änderungen
 *
 * @author C.Stadler on 13.04.2017.
 */
enum UpdateKind
{
  NEW_OBJECT,
  DELETE_OBJECT,
  UPDATE_OBJECT,
  UNDEFINED;  // Noch nicht gesetzt
}
