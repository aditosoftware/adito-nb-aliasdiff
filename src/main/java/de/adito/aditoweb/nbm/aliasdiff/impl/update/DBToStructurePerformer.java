package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.core.checkpoint.CPH;
import de.adito.aditoweb.core.multilanguage.*;
import de.adito.aditoweb.core.util.Utility;
import de.adito.aditoweb.database.IDBConstants;
import de.adito.aditoweb.database.general.metainfo.*;
import de.adito.aditoweb.database.general.sqlstatement.DBIDGenerator;
import de.adito.aditoweb.nbm.aditonetbeansutil.notification.NotifyUtil;
import de.adito.aditoweb.nbm.designer.commonclasses.SystemDefinitionAliasConfigResolver;
import de.adito.aditoweb.nbm.designer.commonclasses.util.SaveUtil;
import de.adito.aditoweb.nbm.designer.commongui.components.systemselection.ServerSystemSelectionPanel;
import de.adito.aditoweb.nbm.designer.commoninterface.services.editorcontext.IEditorContext;
import de.adito.aditoweb.nbm.designerdb.impl.metadata.online.NBViewMetadata;
import de.adito.aditoweb.nbm.entitydbeditor.dataobjects.EntityGroupDBDataObject;
import de.adito.aditoweb.nbm.entityeditorcommon.utility.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.SystemDataModel;
import de.adito.propertly.core.spi.*;
import org.netbeans.api.progress.*;
import org.netbeans.api.project.Project;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * @author t.tasior, 05.03.2018
 */
public class DBToStructurePerformer extends AbstractStructure//todo ist nicht finale Zustand
{

  private final Project project;
  private IEditorContext<SystemDataModel> aliasEditorContext;

  public DBToStructurePerformer(EntityGroupDBDataObject pEntityGroupDataObject, Project pProject)
  {
    super(pEntityGroupDataObject);
    project = pProject;
  }

  public IEditorContext<SystemDataModel> perform()
  {
    final AtomicReference<SystemDefinitionAliasConfigResolver> resolverHolder = new AtomicReference<>();
    final AtomicReference<Object> optionHolder = new AtomicReference<>();
    final AtomicBoolean resolved = new AtomicBoolean();

    //System-Config auswählen
    final ServerSystemSelectionPanel panel = new ServerSystemSelectionPanel(project);
    panel.showInDialog(NbBundle.getMessage(getClass(), "ACTION_EntityDBAddAction"), e ->
    {
      optionHolder.set(e.getSource());
      if (e.getSource() == DialogDescriptor.OK_OPTION)
      {
        try
        {
          aliasEditorContext = panel.getAliasField().getSelectedItem();
          resolverHolder.set(new SystemDefinitionAliasConfigResolver(aliasEditorContext));
          resolved.set(true);
        }
        catch (Exception e1)
        {
          resolved.set(false);
          CPH.checkPoint(e1, 20, 638, CPH.OK_DIALOG);
        }
      }
    });

    if (optionHolder.get() == DialogDescriptor.OK_OPTION && resolved.get())
    {
      try
      {
        entityGroupDBDataObject.startSynchronizing(); //Mitteilen, dass nun synchronisiert wird - Listener deaktivieren
        boolean successful = _doHardWork(resolverHolder.get());
        if (!successful)
          aliasEditorContext = null;
      }
      catch (Exception e)
      {
        aliasEditorContext = null;
        NotifyUtil.balloon().error(e);
      }
      finally
      {
        entityGroupDBDataObject.finishSynchronizing();
        SaveUtil.saveUnsavedStates(null, true);
      }
    }
    return aliasEditorContext;
  }

  /**
   * Führt die eigentliche Arbeit aus
   *
   * @param pResolver SystemDefinitionAliasConfigResolver
   * @return ProgressHandle
   * @throws Exception wenn es einen Fehler gab
   */
  private boolean _doHardWork(SystemDefinitionAliasConfigResolver pResolver) throws Exception
  {
    //Auslesen starten (im Hintergrund mit Progress)
    _FindOutStructure findOutStructure = new _FindOutStructure(pResolver);
    String progressTitel = new ConvenienceTranslator(20).translate(IStaticResources.TITLE_IMPORT_DATABASE_STRUCTURE);
    ProgressHandle handle = ProgressHandle.createHandle(progressTitel);
    boolean findOutWasOK = BaseProgressUtils.showProgressDialogAndRunLater(findOutStructure, handle, true).get();
    if (findOutWasOK)
    {
      _ExecChanges execChanges = new _ExecChanges();
      progressTitel = new ConvenienceTranslator(20).translate(IStaticResources.TITLE_EXEC_DB_TO_STRUCTURE);
      handle = ProgressHandle.createHandle(progressTitel);
      BaseProgressUtils.showProgressDialogAndRunLater(execChanges, handle, true).get();
    }

    return findOutWasOK;
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

    private void _findNonExistingTablesInDB(List<ITableMetadata> pMetadata)
    {
      List<String> namesFromDB = pMetadata.stream().map(ITableMetadata::getName).collect(Collectors.toList());
      List<EntityDBDataModel> toRemove = entityGroup.getEntities().stream()
          .filter(entity -> !namesFromDB.contains(entity.getName()))
          .collect(Collectors.toList());

      for (EntityDBDataModel entityDBDataModel : toRemove)
      {
        TableUpdateInfo tableUpdateInfo = _getTableUpdateInfoByName(entityDBDataModel.getName(), CONTAINER_TYPE.TABLE);
        tableUpdateInfo.setDelete();
        tableUpdateInfos.put(entityDBDataModel.getName(), tableUpdateInfo);
      }
    }

    /**
     * Liefert den Generator-Typ anhand von pTable und pIDColumn.
     *
     * @param pTable    Eine Tabelle aus der Datenbank.
     * @param pIDColumn der Wert der idColumn.
     * @return den Generator-Typ.
     */
    private Integer _getIDGeneratorType(ITableMetadata pTable, String pIDColumn)
    {


      if (!Utility.isEmptyString(pIDColumn))
      {
        List<IColumnMetadata> columns = pTable.getColumns();
        for (IColumnMetadata column : columns)
        {
          if (column.getName().equalsIgnoreCase(pIDColumn))
            return DBIDGenerator.getDefaultIDStrategy(column.getDatatype());
        }
      }

      return IDBConstants.ID_PER_SEQUENZ; //Sequenz
    }

    private List<PropertyValue> _findTableUpddates(ITableMetadata pTable, EntityDBDataModel pEntity)
    {
      List<PropertyValue> tableUpdates = new ArrayList<>();

      tableUpdates.add(_checkIfUpdateNeeded(EntityDBDataModel.idColumn, pEntity, DBMetadataUtil.getIDColumn(pTable)));
      tableUpdates.add(_checkIfUpdateNeeded(EntityDBDataModel.idGeneratorType, pEntity, _getIDGeneratorType(pTable, DBMetadataUtil.getIDColumn(pTable))));
      tableUpdates.add(_checkIfUpdateNeeded(EntityDBDataModel.idGeneratorInterval, pEntity, 1));

      return tableUpdates;
    }

    private PropertyValue _checkIfUpdateNeeded(IPropertyDescription<?, ?> pProperty, IPropertyPitProvider<?, ?, ?> pModel, Object pNewValue)
    {
      if (pModel == null)
        return new PropertyValue(pProperty, pNewValue);

      //noinspection unchecked,rawtypes
      Object oldValue = pModel.getPit().getProperty((IPropertyDescription) pProperty).getValue();

      if (oldValue == null && pNewValue == null)
        return null; // Nix zu tun

      if (oldValue == null || pNewValue == null)
      {
        return new PropertyValue(pProperty, pNewValue); // Vergleichen gibt wenig Sinn wenn NULL
      }

      if (!oldValue.equals(pNewValue))
        return new PropertyValue(pProperty, pNewValue); // Nur updaten wenn sich was getan hat

      return null;
    }

    private ColumnUpdateInfo _getColumnUpdateInfoByName(String pColumnName, TableUpdateInfo pTableUpdateInfo)
    {
      ColumnUpdateInfo columnUpdateInfo = pTableUpdateInfo.getColumnUpdateInfos().get(pColumnName);
      if (columnUpdateInfo == null)
      {
        columnUpdateInfo = pTableUpdateInfo.getColumnInfo(pColumnName, pTableUpdateInfo.getObjectName());
      }

      return columnUpdateInfo;
    }

    private HashMap<String, ColumnUpdateInfo> _findNonExistingColumnsInTable(List<IColumnMetadata> pMetadata, EntityDBDataModel pTableEntity)
    {
      List<String> namesFromTable = pMetadata.stream().map(IColumnMetadata::getName).collect(Collectors.toList());
      HashMap<String, ColumnUpdateInfo> cUI = new HashMap<>();
      if (pTableEntity != null)
      {

        List<IEntityFieldDataModel<?>> toRemove = pTableEntity.getEntityFields()
            .stream()
            .filter(entityField -> !namesFromTable.contains(entityField.getName()))
            .collect(Collectors.toList());


        for (IEntityFieldDataModel<?> aToRemove : toRemove)
        {
          EntityFieldDBDataModel entityFieldDataModel = (EntityFieldDBDataModel) aToRemove;
          ColumnUpdateInfo columnUpdateInfo = _getColumnUpdateInfoByName(entityFieldDataModel.getName(),
                                                                         _getTableUpdateInfoByName(pTableEntity.getName(), CONTAINER_TYPE.TABLE));
          columnUpdateInfo.setDelete();
          cUI.put(entityFieldDataModel.getName(), columnUpdateInfo);
        }
      }

      return cUI;
    }

    /**
     * Liefert eine Liste von Spalten-Namen die unique sind. (abhängig von Indicies)
     *
     * @param pIndexes die Indicies einer Tabelle
     * @return die Liste mit Spalten-Namen
     */
    private List<String> _getUniqueColumns(List<IIndexMetadata> pIndexes)
    {
      List<String> unique = new ArrayList<>();

      for (IIndexMetadata index : pIndexes)
      {
        List<IColumnMetadata> columns = index.getColumns();
        if (columns.size() == 1 && index.isUnique())
          unique.add(columns.get(0).getName());
      }

      return unique;
    }

    /**
     * Liefert eine Liste von Spalten-Namen die indiziert sind.
     * Berücksichtigt nur Indizes auf genau einer Spalte !!
     * PrimaryKeys werden ignoriert.
     *
     * @param pIndexes die Indizes einer Tabelle
     * @return die Liste mit Spalten-Namen
     */
    private List<String> _getIndexedColumns(List<IIndexMetadata> pIndexes)
    {
      List<String> indexed = new ArrayList<>();

      for (IIndexMetadata index : pIndexes)
      {
        List<IColumnMetadata> columns = index.getColumns();
        if (columns.size() == 1)
          indexed.add(columns.get(0).getName());
      }

      return indexed;
    }

    private List<PropertyValue> _findColumnUpdates(IColumnMetadata pColumn, EntityFieldDBDataModel pField, ITableMetadata pMetadata)
    {
      List<PropertyValue> columnUpdates = new ArrayList<>();

      columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.columnType, pField, pColumn.getDatatype()));
      columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.size, pField, pColumn.getSize()));
      columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.scale, pField, (int) pColumn.getScale()));
      columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.isUnique, pField, _getUniqueColumns(pMetadata.getIndexes()).contains(pColumn.getName())));
      columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.notNull, pField, !pColumn.isNullAllowed()));
      columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.primaryKey, pField, pMetadata.getPrimaryKeyColumns().contains(pColumn)));
      columnUpdates.add(_checkIfUpdateNeeded(EntityFieldDBDataModel.index, pField, _getIndexedColumns(pMetadata.getIndexes()).contains(pColumn.getName())));

      return columnUpdates;
    }

    private HashMap<String, ColumnUpdateInfo> _findColumnUpdatesPerTable(EntityDBDataModel pEntity, ITableMetadata pMetadata)
    {
      List<IEntityFieldDataModel<?>> fields = pEntity != null ? pEntity.getEntityFields() : Collections.emptyList();

      HashMap<String, ColumnUpdateInfo> columnUpdateInfos = new HashMap<>(_findNonExistingColumnsInTable(pMetadata.getColumns(), pEntity));

      for (IColumnMetadata column : pMetadata.getColumns())
      {
        if (!columnUpdateInfos.containsKey(column.getName()))
        {
          ColumnUpdateInfo columnUpdateInfo;
          if (pMetadata instanceof NBViewMetadata)
          {
            columnUpdateInfo = new ViewColumnUpdateInfo(column.getName(), column.getTableName());
          }
          else
          {
            columnUpdateInfo = new ColumnUpdateInfo(column.getName(), column.getTableName());
          }
          EntityFieldDBDataModel field = (EntityFieldDBDataModel) EntityUtil.getEntityFieldByName(fields, column.getName());

          // Spalteneigenschaften
          columnUpdateInfo.setColumnProperties(_findColumnUpdates(column, field, pMetadata));
          columnUpdateInfos.put(column.getName(), columnUpdateInfo);
        }
      }
      return columnUpdateInfos;
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
      Map<String, EntityDBDataModel> map = new HashMap<>();
      for (IEntityDataModel<?, ?> entity : entityGroup.getEntities())
        map.put(entity.getName(), (EntityDBDataModel) entity);

      int counter = 1;
      for (ITableMetadata table : pMetadata)
      {
        pHandle.progress("Read: " + table.getName());
        int progress = (int) ((((double) counter) / pMetadata.size()) * 100);
        pHandle.progress(progress);

        EntityDBDataModel entity = map.get(table.getName());

        TableUpdateInfo tableUpdateInfo = _getTableUpdateInfoByName(table.getName(), CONTAINER_TYPE.get(table));
        tableUpdateInfo.setTableProperties(_findTableUpddates(table, entity));
        tableUpdateInfo.setColumnUpdateInfos(_findColumnUpdatesPerTable(entity, table));
        tableUpdateInfos.put(table.getName(), tableUpdateInfo);

        counter++;
      }
    }

    private void _cleanUpTableUpdateInfo()
    {
      tableUpdateInfos.entrySet()
          .removeIf(pO -> !pO.getValue().isSomethingToUpdate());
    }

    @Override
    public Boolean run(ProgressHandle pHandle)
    {
      try
      {
        List<ITableMetadata> metadata = collectTableMetadata(systemDefinitionAliasConfigResolver, entityGroup);

        tableUpdateInfos = new HashMap<>();
        _findNonExistingTablesInDB(metadata);

        // Tabellen aktualisieren
        _handleTablesInModel(metadata, pHandle);
        _cleanUpTableUpdateInfo();

        return true;
      }
      catch (Exception pE)
      {
        NotifyUtil.balloon().error(pE);
        return false;
      }
    }
  }

  /**
   * Runnable für das umsetzen der DB-Änderungen
   */
  private class _ExecChanges implements ProgressRunnable<Boolean>
  {

    @Override
    public Boolean run(ProgressHandle pHandle)
    {
      try
      {
        pHandle.switchToDeterminate(100);
        int counter = 1;
        EntityGroupDBDataObject groupDataObject = entityGroupDBDataObject;
        EntitySyncOutputter output = new EntitySyncOutputter(groupDataObject.getNodeDelegate().getDisplayName());

        for (String tableName : tableUpdateInfos.keySet())
        {
          pHandle.progress("Update: " + tableName);
          int progress = (int) ((((double) counter) / tableUpdateInfos.keySet().size()) * 100);
          pHandle.progress(progress);
          _updateTable(tableName, output);
          counter++;
        }

        return true;
      }
      catch (Exception e)
      {
        NotifyUtil.balloon().error(e); //Fehler werden sonst verschluckt
        return false;
      }
    }

    private void _updateTable(String pTableName, EntitySyncOutputter pOutput)
    {
      TableUpdateInfo tableUpdateInfo = tableUpdateInfos.get(pTableName);
      EntityDBDataModel entityDBDataModel = _findEntityDBDataModel(pTableName, tableUpdateInfo);

      // Tabellenspalten updaten
      for (String columnName : tableUpdateInfo.getColumnUpdateInfos().keySet())
        _updateColumn(columnName, tableUpdateInfo.getColumnUpdateInfos().get(columnName), entityDBDataModel, pOutput);

      // Tabelle löschen
      if (tableUpdateInfo.getDelete())
      {
        entityGroup.removeEntity(entityDBDataModel);
      }
      else
      {
        // TabellenProperties setzen
        List<PropertyValue> tableProperties = tableUpdateInfo.getTableProperties();
        for (PropertyValue pv : tableProperties)
          if (pv != null)
            //noinspection unchecked
            EntitySyncOutputter.updateValueWithLog(entityDBDataModel.getPit().getProperty(pv.getProperty()), pv.getNewValue(), pOutput);
      }
    }

    private void _updateColumn(String pColumnName, ColumnUpdateInfo pColumnUpdateInfo, EntityDBDataModel pEntity, EntitySyncOutputter pOutput)
    {
      EntityFieldDBDataModel entityField = null;
      try
      {
        entityField = (EntityFieldDBDataModel) EntityUtil.getEntityFieldByName(pEntity.getEntityFields(), pColumnName);
      }
      catch (NoSuchElementException ignored)
      {
      }
      if (entityField == null)
      {
        entityField = EntityUtil.createEntityDBFieldModel(pEntity, pColumnUpdateInfo instanceof ViewColumnUpdateInfo ? EntityFieldViewDBDataModel.class : EntityFieldDBDataModel.class, pColumnName);
        pOutput.putAdded(pEntity.getName(), entityField.getName());
      }

      // Spalte löschen
      if (pColumnUpdateInfo.getDelete())
      {
        pEntity.removeField(entityField);

      }
      else
      {
        // Spaltenproperties setzen
        List<PropertyValue> columnProperties = pColumnUpdateInfo.getColumnProperties();

        for (PropertyValue pv : columnProperties)
          if (pv != null)
            //noinspection unchecked
            EntitySyncOutputter.updateValueWithLog(entityField.getPit().getProperty(pv.getProperty()), pv.getNewValue(), pOutput);
      }
    }
  }
}
