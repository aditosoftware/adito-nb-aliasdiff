package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import lombok.*;
import org.openide.util.NbBundle;

import java.util.Map;

/**
 * Type to specify, that a table should be updated
 *
 * @author C.Stadler on 08.02.2017.
 * @author w.glanzer, 04.07.2023 (refactored, translated)
 */
class TableUpdateInfo extends AbstractUpdateInfo
{
  /**
   * Contains all column updates, that should be executed during this table update
   */
  @Getter
  @Setter
  private Map<String, ColumnUpdateInfo> columnUpdateInfos;

  public TableUpdateInfo(@NonNull String pObjectName)
  {
    super(pObjectName);
  }

  @NonNull
  @Override
  public String getDescription()
  {
    return NbBundle.getMessage(TableUpdateInfo.class, isNew() ? "TEXT_TableUpdateInfo_TableNew" : "TEXT_TableUpdateInfo_TableDelete");
  }

}
