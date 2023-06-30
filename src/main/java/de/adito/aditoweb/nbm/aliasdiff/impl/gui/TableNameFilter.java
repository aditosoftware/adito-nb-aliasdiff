package de.adito.aditoweb.nbm.aliasdiff.impl.gui;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.DefaultPropertyFilter;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.EntityDBDataModel;
import de.adito.propertly.core.spi.IProperty;
import lombok.*;

import java.util.Set;

/**
 * In addition to the default behavior, filters out tables that should not be compared.
 *
 * @author w.glanzer, 14.07.2022
 */
@RequiredArgsConstructor
class TableNameFilter extends DefaultPropertyFilter
{
  /**
   * Names of the tables that should be included
   */
  @NonNull
  private final Set<String> tableNames;

  @Override
  public boolean test(@NonNull IProperty<?, ?> pProperty)
  {
    boolean canMatch = super.test(pProperty);

    if (canMatch && EntityDBDataModel.class.isAssignableFrom(pProperty.getType()))
    {
      EntityDBDataModel model = (EntityDBDataModel) pProperty.getValue();
      if (model != null)
        return tableNames.contains(model.getName());
    }

    return canMatch;
  }
}
