package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import com.google.common.base.Strings;
import de.adito.aditoweb.database.IAliasConfigInfo;
import de.adito.aditoweb.database.general.metainfo.*;
import de.adito.aditoweb.filesystem.datamodelfs.misc.IContextualAliasConfigResolver;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.designer.commoninterface.dataobjects.IDesignerDataObject;
import de.adito.aditoweb.nbm.designerdb.api.*;
import de.adito.aditoweb.nbm.designerdb.impl.metadata.online.NBViewMetadata;
import de.adito.aditoweb.nbm.entitydbeditor.dataobjects.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.aliasdefsubs.AliasDefDBDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.datatypes.EDatabaseType;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasConfigDataModel;
import de.adito.notification.INotificationFacade;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.progress.*;
import org.openide.util.NbBundle;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

/**
 * Modifies the database, that it matches a single alias definition
 *
 * @author t.tasior, 05.03.2018
 * @author w.glanzer, 04.07.2023 (refactored, translated)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StructureToDBPerformer
{

  /**
   * Modified the database, that the given alias definition references, so that the database
   * has the same structure as the given alias definition afterwards.
   *
   * @param pAliasConfigResolver Resolver, to get alias configs from
   * @param pLocalAlias          Alias to perform the action
   * @throws Exception if something failed during update
   */
  public static void perform(@NonNull IContextualAliasConfigResolver pAliasConfigResolver, @NonNull EntityGroupDBDataObject pLocalAlias) throws Exception
  {
    try
    {
      // Fire, that the alias definition is about to synchronize
      pLocalAlias.startSynchronizing();

      AliasConfigDataModel aliasConfig = pAliasConfigResolver.getConfigForDefinitionName(getDefinitionName(pLocalAlias));
      EntityGroupDBDataModel entityGroup = pLocalAlias.getPropertyPitProvider();
      if (entityGroup != null)
      {
        // Read structure from database and create a map of update information
        Map<String, TableUpdateInfo> tableUpdateInfos =
            BaseProgressUtils.showProgressDialogAndRunLater(new FindOutStructure(entityGroup, aliasConfig),
                                                            ProgressHandle.createHandle(NbBundle.getMessage(StructureToDBPerformer.class,
                                                                                                            "TEXT_StructureToDBPerformer_FindOut")),
                                                            true).get();

        // Execute update information
        BaseProgressUtils.showProgressDialogAndRunLater(new ExecChangesRunnable(entityGroup, aliasConfig, tableUpdateInfos),
                                                        ProgressHandle.createHandle(NbBundle.getMessage(StructureToDBPerformer.class,
                                                                                                        "TEXT_StructureToDBPerformer_Exec")),
                                                        true).get();

        // show the result
        printErrorIfNecessary(tableUpdateInfos);
      }
    }
    finally
    {
      // Fire, that the alias definition has completed synchronization
      pLocalAlias.finishSynchronizing();
    }
  }

  /**
   * Returns the name, that should be used during database compare.
   * Because of modularization, we need to look for the "targetAliasName" property.
   *
   * @param pDataObject DataObject, that should be read
   * @return the name of the alias or null, if it can't be read
   */
  @Nullable
  private static String getDefinitionName(@NonNull EntityGroupDBDataObject pDataObject)
  {
    // Search for the "targetAliasName" property
    return Optional.ofNullable(pDataObject.getPropertyPitProvider())
        .map(pGroupModel -> pGroupModel.getPit().getParent())
        .filter(AliasDefDBDataModel.class::isInstance)
        .map(pDefDBModel -> ((AliasDefDBDataModel) pDefDBModel).getPit().getValue(AliasDefDBDataModel.targetAliasName))
        .map(Strings::emptyToNull)

        // Read with the common "old" way
        .or(() -> Optional.ofNullable(pDataObject.getParent())
            .map(IDesignerDataObject::getParent)
            .map(IDesignerDataObject::getName))

        // Parent or property is not available -> we do not know, how to handel this anymore..
        .orElse(null);
  }

  /**
   * Shows a balloon that notifies about something went wrong during update execution
   *
   * @param pTableUpdateInfos information, that were executed
   */
  private static void printErrorIfNecessary(@NonNull Map<String, TableUpdateInfo> pTableUpdateInfos)
  {
    pTableUpdateInfos.values().stream()
        .flatMap(pTableUpdateInfo -> Stream.concat(
            Stream.ofNullable(pTableUpdateInfo.getErrorMessage()),
            pTableUpdateInfo.getColumnUpdateInfos().values().stream()
                .map(AbstractUpdateInfo::getErrorMessage)))
        .findFirst()
        .ifPresent(pFirstError -> INotificationFacade.INSTANCE.error(
            new IOException("Some updates could not be applied to database. See IDE-Log for more information.")));
  }

  /**
   * Runnable to read the structure from the database and compare it with the given alias model
   */
  @RequiredArgsConstructor
  private static class FindOutStructure implements ProgressRunnable<Map<String, TableUpdateInfo>>
  {
    /**
     * Alias to compare the structure of the database to
     */
    @NonNull
    private final EntityGroupDBDataModel localAliasModel;

    /**
     * Alias to read the metadata from
     */
    @NonNull
    private final IAliasConfigInfo aliasConfig;

    @Override
    public Map<String, TableUpdateInfo> run(ProgressHandle pHandle)
    {
      Map<String, TableUpdateInfo> tableUpdateInfos = new HashMap<>();

      try
      {
        Map<String, ITableMetadata> metadata = DatabaseAccessProvider.getInstance().getMetadataProvider().getOnline()
            .getTableMetaData(aliasConfig, true)
            .stream()
            .collect(Collectors.toMap(ITableMetadata::getName, pMeta -> pMeta));

        findTablesToCreateInDB(metadata, tableUpdateInfos);
        findTablesToDeleteInDB(metadata, tableUpdateInfos);
        handleTablesInModel(metadata, tableUpdateInfos);
      }
      catch (Exception e)
      {
        INotificationFacade.INSTANCE.error(e);
      }

      return tableUpdateInfos;
    }

    /**
     * Searches all tables, that are not available in the given metdata.
     * This means, that those tables have to be created in the database afterwards
     *
     * @param pMetadata          Metadata of the current database to diff
     * @param pUpdateInfoResults Result to append the tables to add to
     */
    private void findTablesToCreateInDB(@NonNull Map<String, ITableMetadata> pMetadata, @NonNull Map<String, TableUpdateInfo> pUpdateInfoResults)
    {
      localAliasModel.getEntities().stream()
          .filter(entity -> !pMetadata.containsKey(entity.getName()))
          .forEach(pModel -> {
            TableUpdateInfo tableUpdateInfo = getTableUpdateInfoByName(pModel.getName(), null, pUpdateInfoResults);
            tableUpdateInfo.setNew();
            pUpdateInfoResults.put(pModel.getName(), tableUpdateInfo);
          });
    }

    /**
     * Searches all tables, that are available in the given metdata, but not in the current alias
     * This means, that those tables have to be deleted from the database afterwards
     *
     * @param pMetadata          Metadata of the current database to diff
     * @param pUpdateInfoResults Result to append the tables to remove to
     */
    private void findTablesToDeleteInDB(@NonNull Map<String, ITableMetadata> pMetadata, @NonNull Map<String, TableUpdateInfo> pUpdateInfoResults)
    {
      pMetadata.values().forEach(pTableMeta -> {
        String tableName = pTableMeta.getName();
        if (localAliasModel.getEntities().stream().noneMatch(pModel -> pModel.getName().equalsIgnoreCase(tableName)))
          pUpdateInfoResults.put(tableName, getTableUpdateInfoByName(tableName, pTableMeta, pUpdateInfoResults));
      });
    }

    /**
     * Handles the columns, so that those changes will be applied in the database afterwards too.
     *
     * @param pMetadata          Metadata of the current database to diff
     * @param pUpdateInfoResults Result to append the changes to
     */
    private void handleTablesInModel(@NonNull Map<String, ITableMetadata> pMetadata, @NonNull Map<String, TableUpdateInfo> pUpdateInfoResults)
    {
      for (EntityDBDataModel entity : localAliasModel.getEntities())
      {
        ITableMetadata table = pMetadata.get(entity.getName());
        TableUpdateInfo tableUpdateInfo = getTableUpdateInfoByName(entity.getName(), table, pUpdateInfoResults);
        if (!tableUpdateInfo.isNew())
        {
          Map<String, ColumnUpdateInfo> map = new HashMap<>();
          findColumnsToCreateInDB(table, entity, map);
          findColumnToDeleteInDB(table, entity, map);
          tableUpdateInfo.setColumnUpdateInfos(map);
          pUpdateInfoResults.put(table.getName(), tableUpdateInfo);
        }
      }
    }

    /**
     * Searches columns, that are available in the local model but are not available in the db
     *
     * @param pTableMeta         Metadata in database
     * @param pTable             Table in local alias
     * @param pUpdateInfoResults Result to append the columns to add to
     */
    private void findColumnsToCreateInDB(@NonNull ITableMetadata pTableMeta, @NonNull IEntityDataModel<?, ?> pTable,
                                         @NonNull Map<String, ColumnUpdateInfo> pUpdateInfoResults)
    {
      pTable.getEntityFields().forEach(pColumn -> {
        String columnName = pColumn.getName();
        if (pTableMeta.getColumns().stream().noneMatch(pColumnMeta -> columnName.equalsIgnoreCase(pColumnMeta.getName())))
          pUpdateInfoResults.put(columnName, new ColumnUpdateInfo(columnName, AbstractUpdateInfo.UpdateKind.NEW_OBJECT));
      });
    }

    /**
     * Searches columns, that are available in the db, but should be deleted
     * because they are not available in the local alias
     *
     * @param pTableMeta         Metadata in database
     * @param pTable             Table in local alias
     * @param pUpdateInfoResults Result to append the columns to delete to
     */
    private void findColumnToDeleteInDB(@NonNull ITableMetadata pTableMeta, @NonNull IEntityDataModel<?, ?> pTable,
                                        @NonNull Map<String, ColumnUpdateInfo> pUpdateInfoResults)
    {
      pTableMeta.getColumns().forEach(pColumnMeta -> {
        String columnName = pColumnMeta.getName();
        if (pTable.getEntityFields().stream().noneMatch(pColumn -> columnName.equalsIgnoreCase(pColumn.getName())))
          pUpdateInfoResults.put(columnName, new ColumnUpdateInfo(columnName, AbstractUpdateInfo.UpdateKind.DELETE_OBJECT));
      });
    }

    /**
     * Returns the update info, that belongs to a table / view with the given name.
     * If it is not created yet, then a new update info will be created
     *
     * @param pName              Name to search
     * @param pTableMeta         Metadata to create the update info for.
     *                           Null, if a update info should be created, that does not already exist in db
     * @param pUpdateInfoResults Result to get the current info from
     * @return the already / newly created info
     */
    @NonNull
    private TableUpdateInfo getTableUpdateInfoByName(@NonNull String pName, @Nullable ITableMetadata pTableMeta,
                                                     @NonNull Map<String, TableUpdateInfo> pUpdateInfoResults)
    {
      // Search, if already something in result set
      TableUpdateInfo tableUpdateInfo = pUpdateInfoResults.get(pName);
      if (tableUpdateInfo == null)
      {
        // if it is a view, then we should create a view update info
        if (pTableMeta instanceof NBViewMetadata)
          tableUpdateInfo = new ViewUpdateInfo(pName);
        else
          // if it is not a view, than it may be null or a table
          tableUpdateInfo = new TableUpdateInfo(pName);
      }

      return tableUpdateInfo;
    }
  }

  /**
   * Runnable that executes the given changes on the given alias
   */
  @RequiredArgsConstructor
  private static class ExecChangesRunnable implements ProgressRunnable<Void>
  {

    /**
     * Local alias to write the changes to
     */
    @NonNull
    private final EntityGroupDBDataModel localAliasModel;

    /**
     * Alias to write the changes to
     */
    @NonNull
    private final IAliasConfigInfo aliasConfig;

    /**
     * Changes to execute
     */
    @NonNull
    private final Map<String, TableUpdateInfo> tableUpdateInfos;

    @Override
    public Void run(ProgressHandle pHandle)
    {
      try
      {
        // Set progress to the correct value, so the user can track the current progress amount
        pHandle.switchToDeterminate(tableUpdateInfos.size());

        // Execute changes
        DatabaseAccessProvider.getInstance().getConnectionManagement().withJDBCConnection(aliasConfig, pCon -> {
          // do nothing, if the connection is null
          if (pCon == null)
            return;

          String aliasDbName = EDatabaseType.getFromDbType(aliasConfig.getDatabaseType()).getDatabaseProductName();
          String schemaName = aliasConfig.getAliasConfigProperties().getOrDefault("schema", "");

          AtomicInteger progressCounter = new AtomicInteger(0);
          for (Map.Entry<String, TableUpdateInfo> tableUpdateEntry : tableUpdateInfos.entrySet())
          {
            String tableName = tableUpdateEntry.getKey();
            TableUpdateInfo tableUpdateInfo = tableUpdateEntry.getValue();

            // Update Progress to display the tablename
            pHandle.progress(NbBundle.getMessage(ExecChangesRunnable.class, "TEXT_ExecChangesRunnable_Progress", tableName),
                             progressCounter.incrementAndGet());

            // execute table changes
            execChangesInTable(aliasDbName, schemaName, pCon, tableName, tableUpdateInfo);
          }
        });
      }
      catch (Exception e)
      {
        INotificationFacade.INSTANCE.error(e);
      }

      // no result, just return
      return null;
    }

    /**
     * Executes the given update infos on the table with the given name
     *
     * @param pDbName          Name of the DBMS to execute the changes in
     * @param pSchemaName      Name of the schema to execute the changes in
     * @param pCon             Connection to execute the changes
     * @param pTableName       Name of the table to execute the changes
     * @param pTableUpdateInfo Changes to execute
     * @throws DatabaseException if something during the execution failed
     */
    private void execChangesInTable(@NonNull String pDbName, @NonNull String pSchemaName, @NonNull Connection pCon,
                                    @NonNull String pTableName, @NonNull TableUpdateInfo pTableUpdateInfo)
        throws DatabaseException
    {
      // Update table columns
      for (Map.Entry<String, ColumnUpdateInfo> columnUpdateEntry : pTableUpdateInfo.getColumnUpdateInfos().entrySet())
      {
        String columnName = columnUpdateEntry.getKey();
        ColumnUpdateInfo columnUpdateInfo = columnUpdateEntry.getValue();
        if (columnUpdateInfo.isNew())
        {
          EntityFieldDBDataModel column = findEntityFieldByName(localAliasModel, pTableName, columnName);
          if (column != null)
            runSQL(columnUpdateInfo, pCon, generateColumnSQL(pDbName, pSchemaName, pTableName, column));
        }
      }

      // Update whole table
      if (pTableUpdateInfo.isNew())
        runSQL(pTableUpdateInfo, pCon, generateTableSQL(pDbName, pSchemaName, pTableName,
                                                        findEntityByName(localAliasModel, pTableName)));
    }

    /**
     * Searches an {@link EntityDBDataModel} in the given alias, identified by the given table name
     *
     * @param pAlias     Alias to search in
     * @param pTableName Name of the table to search for
     * @return the table or null, if not found
     */
    @Nullable
    private EntityDBDataModel findEntityByName(@NonNull EntityGroupDBDataModel pAlias, @NonNull String pTableName)
    {
      return pAlias.getEntities().stream()
          .filter(pEntity -> pEntity.getName().equalsIgnoreCase(pTableName))
          .findFirst()
          .orElse(null);
    }

    /**
     * Searches an {@link EntityFieldDBDataModel} in the given alias, identified by the given table and column name
     *
     * @param pAlias      Alias to search in
     * @param pTableName  Name of the table to search for
     * @param pColumnName Name of the column to search for
     * @return the entity field or null, if not found
     */
    @Nullable
    private EntityFieldDBDataModel findEntityFieldByName(@NonNull EntityGroupDBDataModel pAlias, @NonNull String pTableName, @NonNull String pColumnName)
    {
      EntityDBDataModel entityDBDataModel = findEntityByName(pAlias, pTableName);
      if (entityDBDataModel != null)
        return entityDBDataModel.getEntityFields().stream()
            .filter(EntityFieldDBDataModel.class::isInstance)
            .filter(pField -> pField.getName().equalsIgnoreCase(pColumnName))
            .map(EntityFieldDBDataModel.class::cast)
            .findFirst()
            .orElse(null);
      return null;
    }

    /**
     * Generates a SQL, that will create the given table in the given schema.
     * If the given entity structure is not null, then the columns will be created too
     *
     * @param pDbName     Name of the DBMS to generate the SQL for
     * @param pSchemaName Name of the schema to create the table in
     * @param pTableName  Name of the table to create
     * @param pEntity     Structure to create inside, null if nothing additional should be created (like indices, primary keys, ...)
     * @return the SQL to execute
     * @throws DatabaseException if the creation of the SQL failed
     */
    @NonNull
    private String generateTableSQL(@NonNull String pDbName, @NonNull String pSchemaName, @NonNull String pTableName,
                                    @Nullable EntityDBDataModel pEntity)
        throws DatabaseException
    {
      List<IColumnMetadata> columns = new LinkedList<>();
      List<IColumnMetadata> pkColumns = new LinkedList<>();
      List<IColumnMetadata> idxColumns = new LinkedList<>();

      if (pEntity != null)
      {
        for (IEntityFieldDataModel<?> entityField : pEntity.getEntityFields())
        {
          EntityFieldDBDataModel fieldModel = (EntityFieldDBDataModel) entityField;
          EntityFieldDBDataObject fieldObj = (EntityFieldDBDataObject) DataObjectUtil.get(entityField);
          IColumnMetadata metadata = fieldObj.getColumnMetadata();
          columns.add(metadata);
          if (Boolean.TRUE == fieldModel.getPrimaryKey())
            pkColumns.add(metadata);
          if (Boolean.TRUE == fieldModel.getIndex())
            idxColumns.add(metadata);
        }
      }

      return "-- table " + pTableName + "\n" + DatabaseAccessProvider.getInstance().getDDLBuilder()
          .getCreateTableDDL(pDbName, pSchemaName, pTableName, columns, pkColumns, idxColumns);
    }

    /**
     * Generates a SQL, that will insert the given column metadata into the given table
     *
     * @param pDbName      Name of the DBMS to generate the SQL for
     * @param pSchemaName  Name of the schema to retrieve the table from
     * @param pTableName   Name of the table to create the column in
     * @param pEntityField Metadata of the column to create
     * @return the SQL to execute
     * @throws DatabaseException if the creation of the SQL failed
     */
    @NonNull
    private String generateColumnSQL(@NonNull String pDbName, @NonNull String pSchemaName, @NonNull String pTableName,
                                     @NonNull EntityFieldDBDataModel pEntityField)
        throws DatabaseException
    {
      IColumnMetadata metadata = ((EntityFieldDBDataObject) DataObjectUtil.get(pEntityField)).getColumnMetadata();
      if (metadata == null)
        return ";";

      return DatabaseAccessProvider.getInstance().getDDLBuilder().getCreateColumnDDL(pDbName, pSchemaName, pTableName, metadata) + ";";
    }

    /**
     * Executes the given SQL on the given connection.
     * If something failed, then the error message will be written to the given update info
     *
     * @param pSource Source of the SQL, that receives the error message
     * @param pConn   Connection to execute the SQL
     * @param pSql    SQL to execute
     */
    private void runSQL(@NonNull AbstractUpdateInfo pSource, @NonNull Connection pConn, @NonNull String pSql)
    {
      String msg = null;
      if (!pSql.trim().isEmpty())
      {
        try
        {
          StringTokenizer str = new StringTokenizer(pSql, ";", false);
          while (str.hasMoreElements())
          {
            String s = str.nextToken();
            s = s.replace("\n\n", "\n");
            if (!s.isEmpty() && !s.equals("\n"))
            {
              msg = s + "\n";
              try (PreparedStatement statement = pConn.prepareStatement(s))
              {
                statement.execute();

                if (!pConn.getAutoCommit())
                  pConn.commit();
              }
              return;
            }
          }
        }
        catch (Exception e)
        {
          INotificationFacade.INSTANCE.error(e);
          msg += e.getMessage();
        }
      }

      pSource.setErrorMessage(msg);
    }
  }
}
