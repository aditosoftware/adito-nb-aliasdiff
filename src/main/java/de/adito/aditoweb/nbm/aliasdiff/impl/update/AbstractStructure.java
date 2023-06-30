package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.core.checkpoint.exception.mechanics.AditoException;
import de.adito.aditoweb.core.multilanguage.ConvenienceTranslator;
import de.adito.aditoweb.database.general.metainfo.*;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.designer.commonclasses.SystemDefinitionAliasConfigResolver;
import de.adito.aditoweb.nbm.designer.commoninterface.dataobjects.IDesignerDataObject;
import de.adito.aditoweb.nbm.designerdb.api.*;
import de.adito.aditoweb.nbm.designerdb.impl.metadata.online.NBViewMetadata;
import de.adito.aditoweb.nbm.entitydbeditor.dataobjects.EntityGroupDBDataObject;
import de.adito.aditoweb.nbm.entityeditorcommon.utility.EntityUtil;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.IEntityFieldDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * @author c.stadler, 18.01.2017.
 */
abstract class AbstractStructure
{
  protected Map<String, TableUpdateInfo> tableUpdateInfos;
  protected EntityGroupDBDataModel entityGroup;
  protected Map<String, ITableMetadata> tableGroup = new HashMap<>();
  protected EntityGroupDBDataObject entityGroupDBDataObject;
  protected ConvenienceTranslator ct;

  protected AbstractStructure(EntityGroupDBDataObject pEntityGroupDataObject)
  {
    entityGroup = pEntityGroupDataObject.getPropertyPitProvider();
    entityGroupDBDataObject = pEntityGroupDataObject;
    ct = new ConvenienceTranslator(20);
  }

  public enum CONTAINER_TYPE
  {
    TABLE,
    VIEW;

    static CONTAINER_TYPE get(ITableMetadata pMetadata)
    {
      if (pMetadata instanceof NBViewMetadata)
        return VIEW;
      return TABLE;
    }
  }

  /**
   * Sammelt Metadaten aus der gewählten Datenbank
   *
   * @param resolver beinhaltet die gewählte Alias-Konfiguration
   * @return eine Liste von ITableMetadata
   */
  protected List<ITableMetadata> collectTableMetadata(SystemDefinitionAliasConfigResolver resolver, EntityGroupDBDataModel pEntityGroup)
      throws AditoException, DatabaseException
  {
    //noinspection unchecked
    String groupName = getDefinitionName((EntityGroupDBDataObject) DataObjectUtil.get(pEntityGroup));
    List<ITableMetadata> result = DatabaseAccessProvider.getInstance().getMetadataProvider().getOnline().getTableMetaData(resolver.getConfigForDefinitionName(groupName),
                                                                                                                          true);
    return Collections.unmodifiableList(result);
  }

  /**
   * Liefert den Namen der Definition, welcher beim Abgleich mit der Datenbank verwendet werden soll.
   * Für die Modularisierung ist es hier notwendig, dass auf den "targetAliasName" verwiesen wird,
   * da u.U. der Alias im Projekt anders heißt als wie er in der Datenbank liegt.
   *
   * @param pDataObject DataObject, zu dem gesucht wird
   * @return der Name der Definition
   */
  @Nullable
  protected String getDefinitionName(@NotNull EntityGroupDBDataObject pDataObject)
  {
    // Entweder suchen wir den Namen im targetAlias
    return Optional.ofNullable(pDataObject.getTargetAliasName())

        // Oder wir suchen ihn über den herkömmlichen Weg über den Parent
        .or(() -> Optional.ofNullable(pDataObject.getParent())
            .map(IDesignerDataObject::getParent)
            .map(IDesignerDataObject::getName))

        // Parent und Property gibt es nicht -> kein Name auffindbar
        .orElse(null);
  }

  protected TableUpdateInfo _getTableUpdateInfoByName(@NotNull String pName, @NotNull CONTAINER_TYPE pContainerType)
  {
    TableUpdateInfo tableUpdateInfo = tableUpdateInfos.get(pName);
    if (tableUpdateInfo == null)
    {
      if (pContainerType == CONTAINER_TYPE.TABLE)
        tableUpdateInfo = new TableUpdateInfo(pName, null);
      else
        tableUpdateInfo = new ViewUpdateInfo(pName, null);
    }

    return tableUpdateInfo;
  }

  protected boolean isIndex(String pColumnName, List<IIndexMetadata> pIndexes)
  {
    for (IIndexMetadata index : pIndexes)
    {
      if (_containsColumn(index.getColumns(), pColumnName))
        return true;
    }
    return false;
  }

  protected boolean isUniqueIndex(String pColumnName, List<IIndexMetadata> pIndexes)
  {
    for (IIndexMetadata index : pIndexes)
    {
      if (_containsColumn(index.getColumns(), pColumnName) && index.isUnique())
        return true;
    }
    return false;
  }

  private boolean _containsColumn(List<IColumnMetadata> pIIndexColumnMetadatas, String pColumnName)
  {
    for (IColumnMetadata column : pIIndexColumnMetadatas)
    {
      if (column.getName().equalsIgnoreCase(pColumnName))
        return true;
    }
    return false;
  }

  protected EntityFieldDBDataModel _findEntityFieldDataModel(@NotNull String pTableName, @NotNull TableUpdateInfo pTableUpdateInfo, @NotNull String pColumnName)
  {
    EntityDBDataModel entityDBDataModel = _findEntityDBDataModel(pTableName, pTableUpdateInfo);
    if (entityDBDataModel != null)
    {
      for (IEntityFieldDataModel<?> fieldDataModel : entityDBDataModel.getEntityFields())
      {
        if (fieldDataModel.getName().equalsIgnoreCase(pColumnName))
          return (EntityFieldDBDataModel) fieldDataModel;
      }
    }
    return null;
  }

  protected EntityDBDataModel _findEntityDBDataModel(@NotNull String pTableName, @NotNull TableUpdateInfo pTableUpdateInfo)
  {
    List<EntityDBDataModel> entities = entityGroup.getEntities();
    for (EntityDBDataModel entity : entities)
      if (entity.getName().equalsIgnoreCase(pTableName))
        return entity;

    // Falls nix gefnuden, wird ein neues erstellt
    if (pTableUpdateInfo instanceof ViewUpdateInfo)
      return EntityUtil.createEntityModel(entityGroup, EntityDBViewDataModel.class, pTableName);
    else
      return EntityUtil.createEntityModel(entityGroup, EntityDBDataModel.class, pTableName);
  }
}
