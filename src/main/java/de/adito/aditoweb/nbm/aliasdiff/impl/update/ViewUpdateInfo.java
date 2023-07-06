package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import lombok.NonNull;
import org.openide.util.NbBundle;

/**
 * Type to specify, that a view should be updated
 *
 * @author m.kaspera, 31.05.2021
 * @author w.glanzer, 04.07.2023 (refactored, translated)
 */
class ViewUpdateInfo extends TableUpdateInfo
{

  public ViewUpdateInfo(@NonNull String pObjectName)
  {
    super(pObjectName);
  }

  @NonNull
  @Override
  public String getDescription()
  {
    return NbBundle.getMessage(ViewUpdateInfo.class, isNew() ? "TEXT_ViewUpdateInfo_ViewNew" : "TEXT_ViewUpdateInfo_ViewDelete");
  }

}
