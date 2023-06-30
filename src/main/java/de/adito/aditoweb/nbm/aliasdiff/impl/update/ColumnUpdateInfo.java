package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.core.multilanguage.IStaticResources;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.EntityFieldDBDataModel;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * @author C.Stadler on 08.02.2017.
 */
class ColumnUpdateInfo extends AbstractUpdateInfo
{

  public ColumnUpdateInfo(@NotNull String pObjectName, @Nullable String pParentObjectName)
  {
    this(new ArrayList<>(), pObjectName, pParentObjectName, UpdateKind.UNDEFINED);
  }

  public ColumnUpdateInfo(@NotNull List<PropertyValue> pColumnProperties, @NotNull String pObjectName, @Nullable String pParentObjectName, @NotNull UpdateKind pUpdateKind)
  {
    super(pColumnProperties, pObjectName, pParentObjectName, pUpdateKind);
  }

  public List<PropertyValue> getColumnProperties()
  {
    return properties;
  }

  public void setColumnProperties(List<PropertyValue> pColumnProperties)
  {
    properties = pColumnProperties;
  }

  @Override
  public boolean isSomethingToUpdate()
  {
    if (getDelete() || getNew())
      return true;

    return !getColumnProperties().isEmpty();// Nix zu tun
  }

  public boolean getIndex()
  {
    return _getBooleanProperty(EntityFieldDBDataModel.index);
  }

  public int getSize()
  {
    return _getIntProperty(EntityFieldDBDataModel.size);
  }

  public int getScale()
  {
    return _getIntProperty(EntityFieldDBDataModel.scale);
  }

  public int getDataType()
  {
    return _getIntProperty(EntityFieldDBDataModel.columnType);
  }

  public boolean getPrimarykey()
  {
    return _getBooleanProperty(EntityFieldDBDataModel.primaryKey);
  }

  public boolean getNotNull()
  {
    return _getBooleanProperty(EntityFieldDBDataModel.notNull);
  }

  public boolean getUnique()
  {
    return _getBooleanProperty(EntityFieldDBDataModel.isUnique);
  }

  private boolean _getBooleanProperty(Object pProp)
  {
    if (_getProperty(pProp) != null)
      return (boolean) _getProperty(pProp);
    return false;
  }

  private int _getIntProperty(Object pProp)
  {
    if (_getProperty(pProp) != null)
      return (int) _getProperty(pProp);
    return 0;
  }

  private Object _getProperty(Object pProp)
  {
    for (PropertyValue propertyValue : getColumnProperties())
    {
      if (propertyValue != null && propertyValue.getProperty().equals(pProp))
        return propertyValue.getNewValue();
    }
    return null;
  }

  @Override
  public String getDescription()
  {
    return getDescription(IStaticResources.DESC_COLUMN_NEW, IStaticResources.DESC_COLUMN_DEL, "", "");
  }

  @Override
  public boolean isAllowed()
  {
    return !getDelete();
  }
}