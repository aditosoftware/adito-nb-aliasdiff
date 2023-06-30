package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.core.multilanguage.IStaticResources;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * @author C.Stadler on 08.02.2017.
 */
class TableUpdateInfo extends AbstractUpdateInfo
{
  Map<String, ColumnUpdateInfo> columnUpdateInfos;

  public TableUpdateInfo(@NotNull String pObjectName, @Nullable String pParentObjectName)
  {
    this(new ArrayList<>(), new HashMap<>(), pObjectName, pParentObjectName, UpdateKind.UNDEFINED);
  }

  public TableUpdateInfo(@NotNull List<PropertyValue> pTableProperties,
                         @NotNull Map<String, ColumnUpdateInfo> pColumnUpdateInfos,
                         @NotNull String pObjectName, @Nullable String pParentObjectName, @NotNull UpdateKind pUpdateKind)
  {
    super(pTableProperties, pObjectName, pParentObjectName, pUpdateKind);
    columnUpdateInfos = pColumnUpdateInfos;
  }

  public ColumnUpdateInfo getColumnInfo(@NotNull String pObjectName, @Nullable String pParentObjectName)
  {
    return new ColumnUpdateInfo(pObjectName, pParentObjectName);
  }

  @NotNull
  @Override
  public String toString()
  {
    return getObjectName() + ":" + getDescription();
  }

  @Override
  public String getDescription()
  {
    return getDescription(IStaticResources.DESC_TABLE_NEW, IStaticResources.DESC_TABLE_DEL, "", "");
  }

  @Override
  public boolean isAllowed()
  {
    return !getDelete();
  }

  public List<PropertyValue> getTableProperties()
  {
    return properties;
  }

  public void setTableProperties(List<PropertyValue> pTableProperties)
  {
    properties = pTableProperties;
  }

  public Map<String, ColumnUpdateInfo> getColumnUpdateInfos()
  {
    return columnUpdateInfos;
  }

  public void setColumnUpdateInfos(Map<String, ColumnUpdateInfo> pColumnUpdateInfos)
  {
    columnUpdateInfos = pColumnUpdateInfos;
  }

  @Override
  public boolean isSomethingToUpdate()
  {
    if (getDelete() || getNew())
      return true;

    for (Map.Entry<String, ColumnUpdateInfo> o : getColumnUpdateInfos().entrySet())
    {
      ColumnUpdateInfo updateInfo = o.getValue();

      if (updateInfo.getDelete())
        return true;

      if (!updateInfo.getColumnProperties().isEmpty())
        return true;
    }

    return false; // Nix zu tun
  }
}
