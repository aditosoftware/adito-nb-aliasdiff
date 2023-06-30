package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.database.general.metainfo.*;

import java.util.List;

/**
 * Hilfsmethoden, welche die Datenbank-Metadaten betreffen
 *
 * @author s.danner, 28.09.2016
 */
public final class DBMetadataUtil
{
  private DBMetadataUtil()
  {
  }

  /**
   * Liefert die ID-Spalte einer Tabelle
   * Entweder der einzige Primärschlüssel oder ein einspaltiger, einzigartiger Index, welcher null nicht erlaubt
   *
   * @param pTable die Metadaten zur Tabelle
   * @return die ID Spalte oder Leerstring, wenn keine ID-Spalte existiert
   */
  public static String getIDColumn(ITableMetadata pTable)
  {
    List<IColumnMetadata> keys = pTable.getPrimaryKeyColumns();
    if (keys.size() == 1)//nur dann liefern wenn der Primärschlüssel aus einer einzigen Spalte besteht.
      return keys.get(0).getName();

    List<IIndexMetadata> indexes = pTable.getIndexes();
    for (IIndexMetadata index : indexes)
    {
      List<IColumnMetadata> indexColumns = index.getColumns();
      if (index.isUnique() && indexColumns.size() == 1)
      {
        IColumnMetadata indexColumn = indexColumns.get(0);
        if (!indexColumn.isNullAllowed())
          return indexColumn.getName();
      }
    }
    return "";
  }
}
