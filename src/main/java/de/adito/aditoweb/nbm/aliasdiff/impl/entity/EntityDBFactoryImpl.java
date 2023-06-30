package de.adito.aditoweb.nbm.aliasdiff.impl.entity;

import de.adito.aditoweb.common.util.PropertlyUtil;
import de.adito.aditoweb.core.IAliasKey;
import de.adito.aditoweb.core.util.Utility;
import de.adito.aditoweb.database.*;
import de.adito.aditoweb.database.general.metainfo.*;
import de.adito.aditoweb.database.general.sqlstatement.DBIDGenerator;
import de.adito.aditoweb.designer.dataobjects.DesignerDataModelHierarchy;
import de.adito.aditoweb.nbm.aliasdiff.impl.db.CustomOnlineMetadataProvider;
import de.adito.aditoweb.nbm.designerdb.api.DatabaseAccessProvider;
import de.adito.aditoweb.nbm.designerdb.impl.metadata.online.NBViewMetadata;
import de.adito.aditoweb.system.crmcomponents.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.aliasdefsubs.AliasDefDBDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.IEntityDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.provider.IEntityProvider;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasDefinitionDataModel;
import de.adito.propertly.core.api.Hierarchy;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.openide.util.lookup.ServiceProvider;

import java.util.*;

/**
 * Factory based on the {@link CustomOnlineMetadataProvider}
 *
 * @author w.glanzer, 12.07.2022
 */
@ServiceProvider(service = IEntityDBFactory.class)
public class EntityDBFactoryImpl implements IEntityDBFactory
{

  private static final CustomOnlineMetadataProvider customOnlineMetadataProvider = new CustomOnlineMetadataProvider();

  @NonNull
  @Override
  public EntityGroupDBDataModel create(@NonNull IAliasConfigInfo pConfig) throws EntityDBModelCreationException
  {
    return extractFromAlias(pConfig, null);
  }

  @NonNull
  @Override
  public EntityGroupDBDataModel create(@NonNull IAliasConfigInfo pConfig, @NonNull Set<String> pTables) throws EntityDBModelCreationException
  {
    return extractFromAlias(pConfig, pTables);
  }

  /**
   * Creates a new {@link EntityGroupDBDataModel} based on the database of the given config.
   * Will extract only tables that are identified in the given set.
   *
   * @param pConfig Alias to read from
   * @param pTables Tables that should be read.
   *                If a table does not exist in the database, then it will be ignored and not written in the resulting model
   * @return the entity model with some tables
   * @throws EntityDBModelCreationException if something failed during creation
   */
  @NonNull
  private EntityGroupDBDataModel extractFromAlias(@NonNull IAliasConfigInfo pConfig, @Nullable Set<String> pTables) throws EntityDBModelCreationException
  {
    try
    {
      // read the table metadata
      List<ITableMetadata> tables;
      if (pTables != null)
        tables = customOnlineMetadataProvider.getTableMetaData(pConfig, new ArrayList<>(pTables), pConfig.getProperty(IAliasKey.SCHEMA), true);
      else
        tables = DatabaseAccessProvider.getInstance().getMetadataProvider().getOnline().getTableMetaData(pConfig, true);

      // create entity model
      EntityGroupDBDataModel root = createNewGroupModel(pConfig.getDefinitionName());
      tables.stream()
          .filter(Objects::nonNull)
          //sort, so that the returning model is not in random order
          .sorted(Comparator.comparing(ITableMetadata::getName, String.CASE_INSENSITIVE_ORDER))
          .forEach(pTable -> addTable(root, pTable));

      return root;
    }
    catch (Exception e)
    {
      throw new EntityDBModelCreationException("Failed to read tables from alias", e);
    }
  }

  /**
   * Creates a new empty AliasDefinitionDataModel containing an empty EntityGroupDBDataModel
   *
   * @param pName Name of the AliasDefinition
   * @return the newly created GroupDBDataModel
   */
  @NonNull
  private EntityGroupDBDataModel createNewGroupModel(@NonNull String pName)
  {
    AliasDefinitionDataModel alias = new DataModelFactory().create(pName, AliasDefinitionDataModel.class);

    //noinspection unchecked
    alias = (AliasDefinitionDataModel) new DesignerDataModelHierarchy((Hierarchy<IDataModel<?, ?>>) alias.getPit().getHierarchy(), null).getValue();
    alias.getPit().setValue(AliasDefinitionDataModel.datasourceType, IDataSourceTypes.EValue.DB.getId());

    AliasDefDBDataModel sub = (AliasDefDBDataModel) alias.getPit().setValue(AliasDefinitionDataModel.aliasDefinitionSub, new AliasDefDBDataModel());
    assert sub != null;
    EntityGroupDBDataModel model = sub.getPit().setValue(AliasDefDBDataModel.entityGroup, new EntityGroupDBDataModel());
    assert model != null;
    model.getPit().setValue(IEntityProvider.entities, new IEntityProvider.Entities());
    return model;
  }

  /**
   * Appends the given {@link ITableMetadata} information to the given {@link EntityGroupDBDataModel}
   *
   * @param pModel Model to append to
   * @param pTable Metadata that should be appended
   */
  private void addTable(@NonNull EntityGroupDBDataModel pModel, @NonNull ITableMetadata pTable)
  {
    IEntityProvider.Entities entities = PropertlyUtil.getInited(pModel, IEntityProvider.entities);
    EntityDBDataModel table = entities.addProperty(pTable.getName(), pTable instanceof NBViewMetadata ?
        new EntityDBViewDataModel() : new EntityDBDataModel()).getValue();
    assert table != null;

    // Meta-Infos
    List<String> uniqueColumns = getUniqueColumns(pTable.getIndexes());
    List<String> indexColumns = getIndexedColumns(pTable.getIndexes());
    String idColumn = getIDColumn(pTable);

    // Columns
    pTable.getColumns().forEach(pColumn -> addColumn(table, pColumn, uniqueColumns.contains(pColumn.getName()),
                                                     pTable.getPrimaryKeyColumns().contains(pColumn), indexColumns.contains(pColumn.getName())));

    // Own Properties
    table.getPit().setValue(EntityDBDataModel.idColumn, idColumn);
    table.getPit().setValue(EntityDBDataModel.idGeneratorType, getIDGeneratorType(pTable, idColumn));
    table.getPit().setValue(EntityDBDataModel.idGeneratorInterval, 1);
  }

  /**
   * Appends the given {@link IColumnMetadata} information to the given {@link EntityDBDataModel}
   *
   * @param pModel        Model to append to
   * @param pColumn       Metadata that should be appended
   * @param pIsUnique     Determines if this column is unique
   * @param pIsPrimaryKey Determines if this column is part of the primary key
   * @param pIsIndex      Determines if this column is part of an index
   */
  private void addColumn(@NonNull EntityDBDataModel pModel, @NonNull IColumnMetadata pColumn,
                         boolean pIsUnique, boolean pIsPrimaryKey, boolean pIsIndex)
  {
    IEntityDataModel.EntityFields fields = PropertlyUtil.getInited(pModel, IEntityDataModel.entityFields);
    EntityFieldDBDataModel column = fields.addProperty(pColumn.getName(), pModel instanceof EntityDBViewDataModel ?
        new EntityFieldViewDBDataModel() : new EntityFieldDBDataModel()).getValue();
    assert column != null;

    // Own Properties
    column.getPit().setValue(EntityFieldDBDataModel.columnType, pColumn.getDatatype());
    column.getPit().setValue(EntityFieldDBDataModel.size, pColumn.getSize());
    column.getPit().setValue(EntityFieldDBDataModel.scale, (int) pColumn.getScale());
    column.getPit().setValue(EntityFieldDBDataModel.isUnique, pIsUnique);
    column.getPit().setValue(EntityFieldDBDataModel.notNull, !pColumn.isNullAllowed());
    column.getPit().setValue(EntityFieldDBDataModel.primaryKey, pIsPrimaryKey);
    column.getPit().setValue(EntityFieldDBDataModel.index, pIsIndex);
  }

  /**
   * Returns a list of columns that are unique, based on the information about indices.
   *
   * @param pIndexes the indices of the searched tables
   * @return the list with column names
   */
  @NonNull
  private List<String> getUniqueColumns(@NonNull List<IIndexMetadata> pIndexes)
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
   * Returns a list of columns names that are part of an index.
   * Primary Keys will be ignored
   *
   * @param pIndexes the indices of a table
   * @return the list with column names
   */
  @NonNull
  private List<String> getIndexedColumns(@NonNull List<IIndexMetadata> pIndexes)
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

  /**
   * Returns the idColumn of a table.
   * Either a single primary key, or a single-columnn, unique index that does not allow null values.
   *
   * @param pTable the metadata to get the id column for
   * @return the id column or an empty string
   */
  @NonNull
  private String getIDColumn(@NonNull ITableMetadata pTable)
  {
    List<IColumnMetadata> keys = pTable.getPrimaryKeyColumns();
    if (keys.size() == 1) // only return, if the primary key is single-column-based
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

  /**
   * Returns the generatorType based on pTable and pIDColumn.
   *
   * @param pTable    table to get the generator type for
   * @param pIDColumn the idColumn of the table
   * @return generator type
   */
  @NonNull
  private Integer getIDGeneratorType(@NonNull ITableMetadata pTable, @NonNull String pIDColumn)
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

    return IDBConstants.ID_PER_SEQUENZ; //Sequence
  }

}
