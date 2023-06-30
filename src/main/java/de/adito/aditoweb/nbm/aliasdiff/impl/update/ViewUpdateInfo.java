package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import org.jetbrains.annotations.*;

import java.util.Map;

/**
 * @author m.kaspera, 31.05.2021
 */
public class ViewUpdateInfo extends TableUpdateInfo
{

  public ViewUpdateInfo(@NotNull String pObjectName, @Nullable String pParentObjectName)
  {
    super(pObjectName, pParentObjectName);
  }

  public ColumnUpdateInfo getColumnInfo(@NotNull String pObjectName, @Nullable String pParentObjectName)
  {
    return new ViewColumnUpdateInfo(pObjectName, pParentObjectName);
  }

  @Override
  public Map<String, ColumnUpdateInfo> getColumnUpdateInfos()
  {
    return super.getColumnUpdateInfos();
  }

  @Override
  public void setColumnUpdateInfos(Map<String, ColumnUpdateInfo> pColumnUpdateInfos)
  {
    super.setColumnUpdateInfos(pColumnUpdateInfos);
  }

  @Override
  public boolean isSomethingToUpdate()
  {
    return super.isSomethingToUpdate();
  }

  @Override
  public String getDescription()
  {
    return "Database View";
  }
}
