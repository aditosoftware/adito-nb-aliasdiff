package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import org.jetbrains.annotations.*;

import java.util.List;

/**
 * @author m.kaspera, 07.06.2021
 */
public class ViewColumnUpdateInfo extends ColumnUpdateInfo
{
  public ViewColumnUpdateInfo(@NotNull String pObjectName, @Nullable String pParentObjectName)
  {
    super(pObjectName, pParentObjectName);
  }

  public ViewColumnUpdateInfo(@NotNull List<PropertyValue> pColumnProperties, @NotNull String pObjectName, @Nullable String pParentObjectName, @NotNull UpdateKind pUpdateKind)
  {
    super(pColumnProperties, pObjectName, pParentObjectName, pUpdateKind);
  }
}
