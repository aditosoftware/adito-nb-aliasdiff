package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.common.util.PropertlyUtil;
import de.adito.aditoweb.core.database.ESystemTable;
import de.adito.aditoweb.designer.dataobjects.DesignerDataModelHierarchy;
import de.adito.aditoweb.filesystem.datamodelfs.creation.DefaultModelCreation;
import de.adito.aditoweb.nbm.entityeditorcommon.utility.EntityUtil;
import de.adito.aditoweb.system.crmcomponents.IDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.IEntityDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.databasesys.tables.api.*;
import de.adito.aditoweb.system.databasesys.tables.impl.DummySystemTables;
import de.adito.propertly.core.api.Hierarchy;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Erzeugt aus den AbstactSystemTables eine Entity Struktur
 *
 * @author c.stadler, 16.11.2017
 */
public class SystemTableStructureCreator
{
  private final EntityGroupDBDataModel groupDBDataModel;

  public SystemTableStructureCreator()
  {
    groupDBDataModel = _createNewGroupDBModel();
    _prefillWithSystemTableInformation(new DummySystemTables());
  }

  /**
   * Liefert die SystemTabellen-Struktur in einem EntityGroupDBDataModel, um mit dem "originalen" besser vergleichen zu können
   *
   * @return das Modell
   */
  @NotNull
  public EntityGroupDBDataModel getEntityStructure()
  {
    return groupDBDataModel;
  }

  /**
   * Iteriert über alle Entities dieses Datenmodells und liefert deren Namen.
   *
   * @return alle Tabellennamen.
   */
  @NotNull
  public Set<String> getTableNames()
  {
    return groupDBDataModel.getEntities().stream()
        .map(IDataModel::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Fügt eine neue Tabelle mit dem übergebenen Namen hinzu
   *
   * @param pName Name der Tabelle
   * @return die hinzugefügte Tabelle als Datenmodell
   */
  @NotNull
  protected EntityDBDataModel addTableEntityModel(@NotNull String pName)
  {
    return EntityUtil.createEntityModel(groupDBDataModel, EntityDBDataModel.class, pName);
  }

  /**
   * Fügt eine neue Spalte mit den übergebenen Informationen hinzu
   *
   * @param pEntityModel Parent-Tabelle
   */
  protected void addColumnFieldModel(EntityDBDataModel pEntityModel, String pColumnName, String pDbName, Boolean pPrimaryKey,
                                     Integer pColumnType, Integer pSize, Integer pScale, Boolean pNotNull, Boolean pIsUnique, Boolean pIndex,
                                     String pTitle, String pDesc)
  {
    EntityFieldDBDataModel model = PropertlyUtil.getInited(pEntityModel, IEntityDataModel.entityFields)
        .addProperty(DefaultModelCreation.createModel(EntityFieldDBDataModel.class, pColumnName)).getValue();
    assert model != null;
    model.getPit().setValue(EntityFieldDBDataModel.dbName, pDbName);
    model.getPit().setValue(EntityFieldDBDataModel.primaryKey, pPrimaryKey);
    model.getPit().setValue(EntityFieldDBDataModel.columnType, pColumnType);
    model.getPit().setValue(EntityFieldDBDataModel.size, pSize);
    model.getPit().setValue(EntityFieldDBDataModel.scale, pScale);
    model.getPit().setValue(EntityFieldDBDataModel.notNull, pNotNull);
    model.getPit().setValue(EntityFieldDBDataModel.isUnique, pIsUnique);
    model.getPit().setValue(EntityFieldDBDataModel.index, pIndex);
    model.getPit().setValue(EntityFieldDBDataModel.title, pTitle);
    model.getPit().setValue(EntityFieldDBDataModel.description, pDesc);
  }

  /**
   * Erstellt ein neues GroupDBDataModel in der richtigen Hierarchie.
   * Würde dies in der normalen Hierarchy passieren, hätten wir keinen Zugriff auf die DataObjects in der Datenbankschicht
   *
   * @return ein neues GroupDBDataModel in der richtigen Hierarchy
   */
  @NotNull
  private EntityGroupDBDataModel _createNewGroupDBModel()
  {
    EntityGroupDBDataModel systemTablesModel = DefaultModelCreation.createModel(EntityGroupDBDataModel.class, "SystemTables");
    //noinspection unchecked,rawtypes geht nicht anders
    Hierarchy<IDataModel<?, ?>> modelHierarchy = (Hierarchy) systemTablesModel.getPit().getHierarchy();
    EntityGroupDBDataModel model = (EntityGroupDBDataModel) new DesignerDataModelHierarchy(modelHierarchy, null).getValue();
    model.getPit().setValue(EntityGroupDBDataModel.entities, new EntityGroupDBDataModel.Entities());
    return model;
  }

  private void _prefillWithSystemTableInformation(@NotNull ISystemTables pSystemTables)
  {
    for (ESystemTable systemTable : ESystemTable.values())
    {
      if (!systemTable.isLegacy())
      {
        TableModel tableModel = pSystemTables.getTable(systemTable);
        EntityDBDataModel entityModel = addTableEntityModel(tableModel.getName());

        for (ITableColumn tableColumn : tableModel.getColumns())
          addColumnFieldModel(entityModel, tableColumn.getName(), "", tableColumn.isPrimaryKey(), tableColumn.getType(),
                              tableColumn.getSize(), tableColumn.getScale(), !tableColumn.isNullable(), tableColumn.isUnique(), tableColumn.isIndex(), "", "");
      }
    }
  }

}
