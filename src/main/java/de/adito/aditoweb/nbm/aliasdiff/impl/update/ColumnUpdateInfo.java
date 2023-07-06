package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import lombok.NonNull;
import org.openide.util.NbBundle;

/**
 * Type to specify, that a column should be updated
 *
 * @author C.Stadler on 08.02.2017.
 * @author w.glanzer, 04.07.2023 (refactored, translated)
 */
class ColumnUpdateInfo extends AbstractUpdateInfo
{

  public ColumnUpdateInfo(@NonNull String pObjectName)
  {
    super(pObjectName);
  }

  public ColumnUpdateInfo(@NonNull String pObjectName, @NonNull UpdateKind pUpdateKind)
  {
    super(pObjectName, pUpdateKind);
  }

  @NonNull
  @Override
  protected String getDescription()
  {
    return NbBundle.getMessage(ColumnUpdateInfo.class, isNew() ? "TEXT_ColumnUpdateInfo_ColumnNew" : "TEXT_ColumnUpdateInfo_ColumnDelete");
  }

}