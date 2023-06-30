package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.core.checkpoint.CPH;
import de.adito.aditoweb.core.checkpoint.exception.mechanics.AditoRuntimeException;
import de.adito.aditoweb.core.multilanguage.IStaticResources;
import de.adito.aditoweb.core.util.Utility;
import de.adito.aditoweb.database.general.metainfo.*;
import de.adito.aditoweb.filesystem.datamodelfs.modellookup.*;
import de.adito.aditoweb.nbm.aditonetbeansutil.notification.NotifyUtil;
import de.adito.aditoweb.nbm.designer.commonclasses.SystemDefinitionAliasConfigResolver;
import de.adito.aditoweb.nbm.designer.commonclasses.util.SaveUtil;
import de.adito.aditoweb.nbm.designer.commoninterface.services.editorcontext.IEditorContext;
import de.adito.aditoweb.nbm.designerdb.api.DatabaseAccessProvider;
import de.adito.aditoweb.nbm.entitydbeditor.dataobjects.EntityGroupDBDataObject;
import de.adito.aditoweb.nbm.entitydbeditor.sqlexport.SQLExporter;
import de.adito.aditoweb.nbm.entityeditorcommon.utility.IEntitySyncAction;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.datatypes.EDatabaseType;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.*;
import de.adito.propertly.core.spi.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.progress.*;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author t.tasior, 05.03.2018
 */
public class StructureToDBPerformer extends AbstractStructure implements IEntitySyncAction
{

  private final IEditorContext<SystemDataModel> context;

  private String aliasDbName = "";
  private String schemaName = "";

  public StructureToDBPerformer(EntityGroupDBDataObject pEntityGroupDataObject, IEditorContext<SystemDataModel> pContext)
  {
    super(pEntityGroupDataObject);
    context = pContext;
  }

  @Override
  public void sync()
  {
    if (context != null)
    {
      try
      {
        aliasDbName = _getDatabaseProductName(context);
        schemaName = _getDatabaseSchema(context);
        entityGroupDBDataObject.startSynchronizing(); //Mitteilen, dass nun synchronisiert wird - Listener deaktivieren
        _doHardWork(new SystemDefinitionAliasConfigResolver(context));
      }
      catch (Exception e)
      {
        NotifyUtil.console().error(e);
      }
      finally
      {
        entityGroupDBDataObject.finishSynchronizing();
        SaveUtil.saveUnsavedStates(null, true);
      }
    }
  }

  /**
   * Aktualisiert alle Tabellen die im Model sind
   *
   * @param pMetadata die Liste mit Metadaten für die Tabellen aus der Datenbank
   * @param pHandle   ProgressHandle für Fortschrittsmeldungen
   */
  private void _handleTablesInModel(List<ITableMetadata> pMetadata, ProgressHandle pHandle)
  {
    pHandle.switchToDeterminate(100);

    int counter = 1;
    for (EntityDBDataModel entity : entityGroup.getEntities())
    {
      pHandle.progress(ct.translate(IStaticResources.TITLE_PROGRESS_READ) + " " + entity.getName());
      int progress = (int) ((((double) counter) / pMetadata.size()) * 100);
      pHandle.progress(progress);

      ITableMetadata table = tableGroup.get(entity.getName());

      TableUpdateInfo tableUpdateInfo = _getTableUpdateInfoByName(entity.getName(), CONTAINER_TYPE.get(table));
      if (!tableUpdateInfo.getNew())
      {
        tableUpdateInfo.setTableProperties(_findTableUpddates(table, entity));

        HashMap<String, ColumnUpdateInfo> map = new HashMap<>();
        map.putAll(_findColumnsUpdatesPerTable(table, entity));
        map.putAll(_findColumnsToDelete(table, entity));
        tableUpdateInfo.setColumnUpdateInfos(map);
        tableUpdateInfos.put(table.getName(), tableUpdateInfo);
      }

      counter++;
    }
  }

  private HashMap<String, ColumnUpdateInfo> _findColumnsUpdatesPerTable(ITableMetadata pTable, IEntityDataModel<?, ?> pEntity)
  {
    HashMap<String, ColumnUpdateInfo> columnUpdateInfos = new HashMap<>();

    for (IEntityFieldDataModel<?> field : pEntity.getEntityFields())
    {
      boolean existing = false;
      for (IColumnMetadata columnMetadata : pTable.getColumns())
      {
        if (field.getName().equalsIgnoreCase(columnMetadata.getName()))
        {
          // Spalte existiert, updates abklappern
          columnUpdateInfos.put(columnMetadata.getName(), _findColumnUpdates(columnMetadata, field, pTable));
          existing = true;
        }
      }
      if (!existing)
      {
        // Spalte existiert nicht, Neuanlage
        columnUpdateInfos.put(field.getName(), new ColumnUpdateInfo(new ArrayList<>(), field.getName(), field.getEntity().getName(),
                                                                    UpdateKind.NEW_OBJECT));
      }
    }

    return columnUpdateInfos;
  }

  private ColumnUpdateInfo _findColumnUpdates(IColumnMetadata pColumn, IEntityFieldDataModel<?> pField, ITableMetadata pMetadata)
  {
    List<PropertyValue> columnUpdates = new ArrayList<>();

    columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.columnType, pField, pColumn.getDatatype()));
    columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.size, pField, pColumn.getSize()));
    columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.scale, pField, pColumn.getScale()));
    columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.notNull, pField, !pColumn.isNullAllowed()));
    columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.primaryKey, pField, pMetadata.getPrimaryKeyColumns().contains(pColumn)));
    columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.index, pField, _isColumnIdexed(pMetadata, pColumn.getName(), false)));
    columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.isUnique, pField, _isColumnIdexed(pMetadata, pColumn.getName(), true)));

    return new ColumnUpdateInfo(columnUpdates, pField.getName(), pField.getEntity().getName(), UpdateKind.UNDEFINED);
  }

  private boolean _isColumnIdexed(ITableMetadata pMetadata, String pColumnName, boolean pUniqueNeeded)
  {
    for (IIndexMetadata indexMetadata : pMetadata.getIndexes())
    {
      for (IColumnMetadata indexColumnMetadata : indexMetadata.getColumns())
      {
        if (indexColumnMetadata.getName().equalsIgnoreCase(pColumnName))
        {
          return !pUniqueNeeded || indexMetadata.isUnique();
        }
      }
    }
    return false;
  }

  private PropertyValue _checkIfUpdateNeeded(IPropertyDescription<?, ?> pProperty, IPropertyPitProvider<?, ?, ?> pModel, Object pNewValue)
  {
    if (pModel == null)
      return new PropertyValue(pProperty, pNewValue);

    //noinspection unchecked,rawtypes
    Object oldValue = ((IPropertyPit) pModel.getPit()).getProperty(pProperty).getValue();

    if (oldValue == null && pNewValue == null)
      return null; // Nix zu tun

    if (oldValue == null || pNewValue == null)
    {
      return new PropertyValue(pProperty, oldValue); // Vergleichen gibt wenig Sinn wenn NULL
    }

    if (!oldValue.equals(pNewValue))
      return new PropertyValue(pProperty, oldValue); // Nur updaten wenn sich was getan hat

    return null;
  }

  private List<PropertyValue> _findTableUpddates(ITableMetadata pTable, IEntityDataModel<?, EntityDBDataModel> pEntity)
  {
    List<PropertyValue> tableUpdates = new ArrayList<>();

    String idColumn = pEntity.getPit().getProperty(EntityDBDataModel.idColumn).getValue();
    if (!Utility.isNullOrEmptyTrimmedString(idColumn))
    {
      boolean pkExists = false;
      for (IColumnMetadata pk : pTable.getPrimaryKeyColumns())
      {
        if (pk.getName().equalsIgnoreCase(idColumn))
          pkExists = true;
      }
      if (!pkExists)
        tableUpdates.add(new PropertyValue(EntityDBDataModel.idColumn, idColumn));
    }
    return tableUpdates;
  }

  private HashMap<String, ColumnUpdateInfo> _findColumnsToDelete(ITableMetadata pTable, EntityDBDataModel pEntity)
  {
    HashMap<String, ColumnUpdateInfo> infoHashMap = new HashMap<>();

    for (IColumnMetadata columnMetadata : pTable.getColumns())
    {
      boolean exists = false;
      for (IEntityFieldDataModel<?> entityField : pEntity.getEntityFields())
      {
        if (entityField.getName().equalsIgnoreCase(columnMetadata.getName()))
          exists = true;
      }
      if (!exists)
        infoHashMap.put(columnMetadata.getName(), new ColumnUpdateInfo(new ArrayList<>(), columnMetadata.getName(), pTable.getName(), UpdateKind.DELETE_OBJECT));
    }

    return infoHashMap;
  }

  private void _findTablesToDelete(List<ITableMetadata> pMetadata)
  {
    for (ITableMetadata meta : pMetadata)
    {
      String tableName = meta.getName();
      boolean exists = false;
      for (EntityDBDataModel entityDBDataModel : entityGroup.getEntities())
      {
        if (entityDBDataModel.getName().equalsIgnoreCase(tableName))
          exists = true;
      }
      if (!exists)
      {
        TableUpdateInfo tableUpdateInfo = _getTableUpdateInfoByName(tableName, CONTAINER_TYPE.get(meta));
        tableUpdateInfo.setDelete();
        tableUpdateInfos.put(tableName, tableUpdateInfo);
      }
    }
  }


  private String _getDatabaseSchema(IEditorContext<SystemDataModel> pAliasEditorContext) throws ServerIdResolveException, AliasConfigNotFoundException
  {
    String name = getDefinitionName(entityGroupDBDataObject);
    if (name != null)
    {
      return new SystemDefinitionAliasConfigResolver(pAliasEditorContext)
          .getConfigForDefinitionName(name)
          .getAliasConfigProperties()
          .getOrDefault("schema", "");
    }
    return "";
  }

  private String _getDatabaseProductName(IEditorContext<SystemDataModel> pAliasEditorContext) throws AliasConfigNotFoundException, ServerIdResolveException
  {
    SystemDefinitionAliasConfigResolver resolver = new SystemDefinitionAliasConfigResolver(pAliasEditorContext);
    String name = getDefinitionName(entityGroupDBDataObject);
    if (name != null)
    {
      AliasConfigDataModel cfg = resolver.getConfigForDefinitionName(name);
      int databaseType = cfg.getDatabaseType();
      return EDatabaseType.getFromDbType(databaseType).getDatabaseProductName();
    }
    return EDatabaseType.NO_DATABASE.getDatabaseProductName();
  }

  /**
   * Führt die eigentliche Arbeit aus
   *
   * @param pResolver SystemDefinitionAliasConfigResolver
   * @throws Exception wenn es einen Fehler gab
   */
  private void _doHardWork(SystemDefinitionAliasConfigResolver pResolver) throws Exception
  {
    // Auslesen starten (im Hintergrund mit Progress)
    _FindOutStructure findOutStructure = new _FindOutStructure(pResolver);
    String progressTitel = ct.translate(IStaticResources.TITLE_EXPORT_DATABASE_STRUCTURE);
    ProgressHandle handle = ProgressHandle.createHandle(progressTitel);
    BaseProgressUtils.showProgressDialogAndRunLater(findOutStructure, handle, true).get();

    // Änderungen umsetzen !
    _ExecChanges execChanges = new _ExecChanges(pResolver);
    progressTitel = ct.translate(IStaticResources.TITLE_EXEC_STRUCTURE_TO_DB);
    handle = ProgressHandle.createHandle(progressTitel);
    BaseProgressUtils.showProgressDialogAndRunLater(execChanges, handle, true).get();

    _showResult();
  }

  private void _showResult()
  {
    List<String> em = new ArrayList<>();

    for (String table : (new TreeSet<>(tableUpdateInfos.keySet())))
    {
      TableUpdateInfo tableUpdateInfo = tableUpdateInfos.get(table);
      String tableUpdateInfoErrorMessage = tableUpdateInfo.getErrorMessage();
      if (tableUpdateInfoErrorMessage != null && !tableUpdateInfoErrorMessage.isEmpty())
      {
        em.add(tableUpdateInfoErrorMessage);

        for (String column : (new TreeSet<>(tableUpdateInfo.getColumnUpdateInfos().keySet())))
        {
          ColumnUpdateInfo columnUpdateInfo = tableUpdateInfo.getColumnUpdateInfos().get(column);
          String columnUpdateInfoErrorMessage = columnUpdateInfo.getErrorMessage();
          if (columnUpdateInfoErrorMessage != null && !columnUpdateInfoErrorMessage.isEmpty())
            em.add(columnUpdateInfoErrorMessage);
        }
      }
    }

    if (!em.isEmpty())
    {
      CPH.checkPoint(20, 862, em.toArray(String[]::new), CPH.OK_DIALOG);
    }
  }

  /**
   * Runnable für das Auslesen aus der Datenbank
   */
  private class _FindOutStructure implements ProgressRunnable<Boolean>
  {
    private final SystemDefinitionAliasConfigResolver systemDefinitionAliasConfigResolver;

    public _FindOutStructure(SystemDefinitionAliasConfigResolver pSystemDefinitionAliasConfigResolver)
    {
      systemDefinitionAliasConfigResolver = pSystemDefinitionAliasConfigResolver;
    }

    @Override
    public Boolean run(ProgressHandle pHandle)
    {
      try
      {
        List<ITableMetadata> metadata = collectTableMetadata(systemDefinitionAliasConfigResolver, entityGroup);
        tableUpdateInfos = new HashMap<>();

        _findNewTablesForDB(metadata);
        _findTablesToDelete(metadata);
        _fillTableGroup(metadata);
        _handleTablesInModel(metadata, pHandle);

        return true;
      }
      catch (Exception pE)
      {
        NotifyUtil.console().error(pE);
        return false;
      }
    }

    private void _fillTableGroup(List<ITableMetadata> pMetadata)
    {
      //Map<String, ITableMetadata> map = new HashMap<>();
      for (ITableMetadata table : pMetadata)
        tableGroup.put(table.getName(), table);
    }

    private void _findNewTablesForDB(List<ITableMetadata> pMetadata)
    {
      List<String> namesFromDB = pMetadata.stream().map(ITableMetadata::getName).collect(Collectors.toList());
      List<EntityDBDataModel> tablesToAdd = entityGroup.getEntities().stream()
          .filter(entity -> !namesFromDB.contains(entity.getName()))
          .collect(Collectors.toList());

      for (EntityDBDataModel entityDBDataModel : tablesToAdd)
      {
        TableUpdateInfo tableUpdateInfo = _getTableUpdateInfoByName(entityDBDataModel.getName(), CONTAINER_TYPE.TABLE);
        tableUpdateInfo.setNew();
        tableUpdateInfos.put(entityDBDataModel.getName(), tableUpdateInfo);
      }
    }
  }

  /**
   * Runnable für das umsetzen der DB-Änderungen
   */
  private class _ExecChanges implements ProgressRunnable<Boolean>
  {
    private final SystemDefinitionAliasConfigResolver systemDefinitionAliasConfigResolver;

    public _ExecChanges(SystemDefinitionAliasConfigResolver pSystemDefinitionAliasConfigResolver)
    {
      systemDefinitionAliasConfigResolver = pSystemDefinitionAliasConfigResolver;
    }

    @Override
    public Boolean run(ProgressHandle pHandle)
    {
      try
      {
        // DB-Verbindung aufbauen
        //noinspection ConstantConditions wird im catch abgefangen
        AliasConfigDataModel aliasConfig = systemDefinitionAliasConfigResolver.getConfigForDefinitionName(getDefinitionName(entityGroupDBDataObject));

        pHandle.switchToDeterminate(100);

        return DatabaseAccessProvider.getInstance().getConnectionManagement().withJDBCConnection(aliasConfig, pCon -> {
          int counter = 1;

          for (String tableName : tableUpdateInfos.keySet())
          {
            pHandle.progress(ct.translate(IStaticResources.TITLE_PROGRESS_UPDATE) + " " + tableName);
            int progress = (int) ((((double) counter) / tableUpdateInfos.keySet().size()) * 100);
            pHandle.progress(progress);

            _updateTable(tableName, pCon);

            counter++;
          }

          return true;
        });
      }
      catch (Exception e)
      {
        NotifyUtil.console().error(e); //Fehler werden sonst verschluckt
        return false;
      }
    }

    private void _updateTable(String pTableName, Connection pConn)
    {
      TableUpdateInfo tableUpdateInfo = tableUpdateInfos.get(pTableName);
      EntityDBDataModel entityDBDataModel = _findEntityDBDataModel(pTableName, tableUpdateInfo);

      // Tabellenspalten updaten
      for (String columnName : tableUpdateInfo.getColumnUpdateInfos().keySet())
      {
        _updateColumn(pTableName, tableUpdateInfo, columnName, tableUpdateInfo.getColumnUpdateInfos().get(columnName), pConn);
      }
      tableUpdateInfo.setErrorMessage(_execUpdate(_createNewTable(pTableName, tableUpdateInfo, entityDBDataModel), pConn));
    }

    private void _updateColumn(@NotNull String pTableName, @NotNull TableUpdateInfo pTableUpdateInfo, @NotNull String pColumnName,
                               @NotNull ColumnUpdateInfo pColumnUpdateInfo, @NotNull Connection pConn)
    {
      if (pColumnUpdateInfo.getNew())
        pColumnUpdateInfo.setErrorMessage(_execUpdate(_createNewColumn(pTableName, pTableUpdateInfo, pColumnUpdateInfo, pColumnName) + ";", pConn));
      else if (pColumnUpdateInfo.isSomethingToUpdate())
      {
        pColumnUpdateInfo.setErrorMessage(_execUpdate(_modifyColumn(pTableName, pColumnUpdateInfo, pColumnName), pConn));
      }
    }

    private String _execUpdate(String pSql, Connection pConn)
    {
      String msg = null;
      if (!pSql.isEmpty())
      {
        try
        {
          StringTokenizer str = new StringTokenizer(pSql, ";", false);
          while (str.hasMoreElements())
          {
            String s = str.nextToken();
            s = s.replace("\n\n", "\n");
            if (!s.isEmpty() && (!s.equals("\n")))
            {
              msg = s + "\n";
              try (PreparedStatement statement = pConn.prepareStatement(s))
              {
                statement.execute();

                if (!pConn.getAutoCommit())
                  pConn.commit();
              }
              return null; // Bei Erfolg kein Fehler...
            }
          }
        }
        catch (Exception e)
        {
          NotifyUtil.console().error(e);
          msg += e.getMessage();
        }
      }
      return msg;
    }

    private String _createNewTable(String pTableName, TableUpdateInfo pTableUpdateInfo, EntityDBDataModel pEntityDBDataModel)
    {
      if (pTableUpdateInfo.getNew())
        return SQLExporter.generateTableSQL(pTableName, aliasDbName, pEntityDBDataModel.getEntityFields(), schemaName);

      return "";
    }

    private String _createNewColumn(@NotNull String pTableName, @NotNull TableUpdateInfo pTableUpdateInfo, @NotNull ColumnUpdateInfo pColumnUpdateInfo,
                                    @NotNull String pColumnName)
    {
      if (pColumnUpdateInfo.getNew())
        return SQLExporter.generateColumnSQL(pTableName, aliasDbName, _findEntityFieldDataModel(pTableName, pTableUpdateInfo, pColumnName), schemaName);
      return "";
    }

    private String _modifyColumn(String pTableName, ColumnUpdateInfo pColumnUpdateInfo, String pColumnName)
    {
      String sql = "";
      try
      {
        if (pColumnUpdateInfo.getIndex())
          sql += _createNewIndex(pTableName, pColumnName, pColumnUpdateInfo.getUnique());
      }
      catch (Exception e)
      {
        throw new AditoRuntimeException(e, 20, 855);
      }


      return sql;
    }

    private String _createNewIndex(String pTableName, String pColumnName, boolean pIsUnique)
    {
      String sql = "";
      try
      {
        String indexName = _getIndexNameForColumn(pTableName, pColumnName);
        if (indexName.isEmpty())
        {
          sql += DatabaseAccessProvider.getInstance().getDDLBuilder().getCreateIndexDDL(aliasDbName, schemaName, pTableName, pColumnName, pIsUnique);
          sql += ";\n";
        }
      }
      catch (Exception e)
      {
        throw new AditoRuntimeException(e, 20, 858);
      }
      return sql;
    }

    private String _getIndexNameForColumn(String pTableName, String pColumnName)
    {
      ITableMetadata tableMetadata = tableGroup.get(pTableName);
      List<IIndexMetadata> indexes = tableMetadata.getIndexes();
      String indexName = "";

      for (IIndexMetadata indexMetadata : indexes)
      {
        for (IColumnMetadata columnMetadata : indexMetadata.getColumns())
        {
          if (columnMetadata.getName().equalsIgnoreCase(pColumnName))
            indexName = indexMetadata.getName();
        }
      }
      return indexName;
    }
  }
}
