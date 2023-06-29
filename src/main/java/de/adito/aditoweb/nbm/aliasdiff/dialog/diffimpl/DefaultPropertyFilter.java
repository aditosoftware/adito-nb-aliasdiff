package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.IPropertyFilter;
import de.adito.aditoweb.system.crmcomponents.annotations.DIFF;
import de.adito.propertly.core.spi.*;
import lombok.NonNull;

/**
 * Default filter that only allows properties that are annotated with {@link DIFF}
 *
 * @author t.tasior, 05.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 * @see DIFF
 */
public class DefaultPropertyFilter implements IPropertyFilter
{

  @Override
  public boolean test(@NonNull IProperty<?, ?> pProperty)
  {
    IPropertyDescription<?, ?> pDescr = pProperty.getDescription();
    return pDescr.isAnnotationPresent(DIFF.class) || pDescr.getType().isAnnotationPresent(DIFF.class);
  }
}
