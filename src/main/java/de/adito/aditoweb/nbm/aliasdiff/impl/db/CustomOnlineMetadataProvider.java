package de.adito.aditoweb.nbm.aliasdiff.impl.db;

import de.adito.aditoweb.common.IDBTypes;
import de.adito.aditoweb.core.util.Utility;
import de.adito.aditoweb.database.IAliasConfigInfo;
import de.adito.aditoweb.database.general.metainfo.ITableMetadata;
import de.adito.aditoweb.nbm.designerdb.api.*;
import de.adito.aditoweb.nbm.designerdb.impl.metadata.online.OnlineMetaDataProvider;
import lombok.*;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.db.explorer.metadata.MetadataModelManager;
import org.netbeans.modules.db.metadata.model.api.*;
import org.openide.util.NbBundle;

import java.lang.reflect.Constructor;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * Contains the ability to get table meta data for a bunch of tables too.
 * Should be included into the ADITO Designer sources as soon as possible.
 *
 * @author w.glanzer, 28.06.2023
 */
@Log
public class CustomOnlineMetadataProvider
{
  private static final String SCHEMA_NOT_FOUND_STRING = "Failed to retrieve database schema of ";
  private static Constructor<?> NBTABLEMETA_CONSTR;
  private static Constructor<?> NBVIEWMETA_CONSTR;

  @NonNull
  public List<ITableMetadata> getTableMetaData(@NonNull IAliasConfigInfo pAlias, @NonNull List<String> pTables, @Nullable String pSchema,
                                               boolean pIncludeViews) throws DatabaseException
  {
    Set<String> uppercasedNames = pTables.stream()
        .map(pName -> pName.toUpperCase(Locale.ROOT))
        .collect(Collectors.toSet());

    return readTableMeta(pAlias, pTables, pSchema, pIncludeViews).stream()
        .filter(pMetadata -> pMetadata == null || uppercasedNames.contains(pMetadata.getName().toUpperCase(Locale.ROOT)))
        .collect(Collectors.toList());
  }

  /**
   * Extracts metadata of the given table names
   *
   * @param pAliasConfigInfo information about the db connection
   * @param pTables          Name of the tables or null, if all tables should be read
   * @param pSchema          Name of the schema, or null if the default schema is to be used (or any if a default schema does not exist)
   * @return list of table metadata, may contain null-values if some tables won't exist
   * @throws DatabaseException if something failed
   */
  @NonNull
  private List<ITableMetadata> readTableMeta(@NonNull IAliasConfigInfo pAliasConfigInfo, @Nullable List<String> pTables,
                                             @Nullable String pSchema, boolean pIncludeViews) throws DatabaseException
  {
    DatabaseConnection connection = DatabaseAccessProvider.getInstance().getConnectionManagement().getConnection(pAliasConfigInfo);
    if (connection == null)
      throw new DatabaseException("Failed to initiate database connection");

    AtomicReference<List<ITableMetadata>> result = new AtomicReference<>();

    try
    {
      MetadataModel metadataModel = MetadataModelManager.get(connection);
      metadataModel.runReadAction(pMetadata -> {
        // Refresh metadata so that it is up-to-date
        pMetadata.refresh();

        // Get schema
        Catalog catalog = pMetadata.getDefaultCatalog();
        Schema schema = pSchema == null || pSchema.trim().isEmpty() ? pMetadata.getDefaultSchema() : catalog.getSchema(pSchema);
        if (schema == null)
        {
          schema = pMetadata.getDefaultSchema(); // default Schema
          if (schema != null)
            log.warning(SCHEMA_NOT_FOUND_STRING + pAliasConfigInfo.getDefinitionName() + " ('" + pSchema + "'). Using default schema '" + schema + "'.");
        }
        if (schema == null)
        {
          schema = catalog.getSchemas().stream().findFirst().orElse(null); // kein Default-Schema vorhanden -> einfach irgendeins
          if (schema != null)
            log.warning(SCHEMA_NOT_FOUND_STRING + pAliasConfigInfo.getDefinitionName() + " ('" + pSchema + "'). Using schema '" + schema + "'.");
        }
        if (schema == null)
        {
          // no schema available
          log.warning(SCHEMA_NOT_FOUND_STRING + pAliasConfigInfo.getDefinitionName() + " ('" + pSchema + "'). No schema available.");
          return;
        }

        // Dissolve and wrap so that no metadata-sensitive content gets outside, see metadata documentation
        schema = MetadataElementHandle.create(schema).resolve(pMetadata);

        // create callback
        CallbackImpl callback = new CallbackImpl(pAliasConfigInfo);

        // read tables
        List<ITableMetadata> tables;
        if (pTables != null)
        {
          tables = new ArrayList<>(pTables.size());
          pTables.forEach(pS -> tables.add(null));
          for (int i = 0; i < pTables.size(); i++)
          {
            Table table = schema.getTable(pTables.get(i));
            if (table != null)
              tables.set(i, createTableMeta(table, callback));
            else
            {
              View view = schema.getView(pTables.get(i));
              if (view != null && pIncludeViews)
                tables.set(i, createViewMeta(view, callback));
            }
          }
        }
        else
        {
          //noinspection FuseStreamOperations We may add elements to the list here later -> wrap in ArrayList so that the list is mutable
          tables = new ArrayList<>(schema.getTables().stream()
                                       .map(pInnerTable -> createTableMeta(pInnerTable, callback))
                                       .collect(Collectors.toList()));
          if (pIncludeViews)
            tables.addAll(schema.getViews().stream().map(pView -> createViewMeta(pView, callback)).collect(Collectors.toList()));
        }

        // From here on, the callback is invalid and may no longer be used.
        // Otherwise, data from the metadata layer would be given outside, which must not be the case (see above)
        callback.invalidate();

        result.set(tables);
      });
    }
    catch (Exception e)
    {
      throw new DatabaseException("Failed to retrieve metadata", e);
    }

    List<ITableMetadata> res = result.get();
    return res == null ? List.of() : res;
  }

  /**
   * Creates the {@link ITableMetadata} for the given db table
   *
   * @param pTable    Table to create the metadata container for
   * @param pCallback Callback to use for inner calculations
   * @return the metadata
   */
  @NonNull
  private ITableMetadata createTableMeta(@NonNull Table pTable, @Nullable OnlineMetaDataProvider.IMetadataCallback pCallback)
  {
    try
    {
      if (NBTABLEMETA_CONSTR == null)
      {
        Class<?> clazz = Class.forName("de.adito.aditoweb.nbm.designerdb.impl.metadata.online.NBTableMetadata");
        NBTABLEMETA_CONSTR = clazz.getDeclaredConstructor(Table.class, OnlineMetaDataProvider.IMetadataCallback.class);
        NBTABLEMETA_CONSTR.setAccessible(true);
      }

      return (ITableMetadata) NBTABLEMETA_CONSTR.newInstance(pTable, pCallback);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates the {@link ITableMetadata} for the given db view
   *
   * @param pView     View to create the metadata container for
   * @param pCallback Callback to use for inner calculations
   * @return the metadata
   */
  @NonNull
  private ITableMetadata createViewMeta(@NonNull View pView, @Nullable OnlineMetaDataProvider.IMetadataCallback pCallback)
  {
    try
    {
      if (NBVIEWMETA_CONSTR == null)
      {
        Class<?> clazz = Class.forName("de.adito.aditoweb.nbm.designerdb.impl.metadata.online.NBViewMetadata");
        NBVIEWMETA_CONSTR = clazz.getDeclaredConstructor(View.class, OnlineMetaDataProvider.IMetadataCallback.class);
        NBVIEWMETA_CONSTR.setAccessible(true);
      }

      return (ITableMetadata) NBVIEWMETA_CONSTR.newInstance(pView, pCallback);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Callback-Impl
   */
  @AllArgsConstructor
  private static class CallbackImpl implements OnlineMetaDataProvider.IMetadataCallback
  {
    private IAliasConfigInfo configInfo;

    @NonNull
    @Override
    public List<String> getUniqueColumns(@Nullable String pSchema, @NonNull String pTable)
    {
      if (!isValid())
        throw new IllegalArgumentException("callback already invalid");

      try
      {
        return DatabaseAccessProvider.getInstance().getConnectionManagement().withJDBCConnection(configInfo, pCon -> {
          assert pCon != null;
          boolean isLowerCased = configInfo.getDatabaseType() == IDBTypes.POSTGRESQL_8_1;
          String table = isLowerCased ? pTable.toLowerCase() : pTable;
          String schema = isLowerCased && pSchema != null ? pSchema.toLowerCase() : pSchema;

          List<String> result = new ArrayList<>();
          try (ResultSet rs = pCon.getMetaData().getIndexInfo(null, schema, table, true, true))
          {
            while (rs.next())
              result.add(rs.getString("COLUMN_NAME"));
          }
          return result;
        });
      }
      catch (DatabaseException | SQLException e)
      {
        Logger.getLogger(OnlineMetaDataProvider.class.getName()).log(Level.WARNING, e, () -> NbBundle.getMessage(OnlineMetaDataProvider.class, "" +
            "ERROR_TableMetadata", pTable));
        return List.of();
      }
    }

    @Nullable
    @Override
    public String getDefaultValue(@Nullable String pSchema, @NonNull String pTable, @NonNull String pColumn)
    {
      if (!isValid())
        throw new IllegalArgumentException("callback already invalid");

      try
      {
        String columnDef = DatabaseAccessProvider.getInstance().getConnectionManagement().withJDBCConnection(configInfo, pCon -> {
          assert pCon != null;
          boolean isLowerCased = configInfo.getDatabaseType() == IDBTypes.POSTGRESQL_8_1;
          String table = isLowerCased ? pTable.toLowerCase() : pTable;
          String schema = isLowerCased && pSchema != null ? pSchema.toLowerCase() : pSchema;
          String column = isLowerCased ? pColumn.toLowerCase() : pColumn;

          try (ResultSet col = pCon.getMetaData().getColumns(null, schema, table, column))
          {
            if (col.next())
              return col.getString("COLUMN_DEF");
          }

          return null;
        });

        // we do not want NULL here, if the database returned it
        if (Utility.isNullOrEmptyTrimmedString(columnDef) || "null".equalsIgnoreCase(columnDef.trim()))
          return null;

        // sometimes the value may be in brackets
        if (columnDef.startsWith("(") && columnDef.endsWith(")"))
          columnDef = columnDef.substring(1, columnDef.length() - 1);

        return columnDef;
      }
      catch (Exception e)
      {
        return null;
      }
    }

    /**
     * Ininvalidates the callback so that it can no longer execute anything database-relevant and frees up all resources
     */
    public void invalidate()
    {
      configInfo = null;
    }

    /**
     * @return returns true, if this callback is valid
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // increased readability
    public boolean isValid()
    {
      return configInfo != null;
    }
  }

}
