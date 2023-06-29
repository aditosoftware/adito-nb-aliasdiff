package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.IPropertyFilter;
import de.adito.aditoweb.system.crmcomponents.annotations.DIFF;
import de.adito.propertly.core.spi.IProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Standardfilter, der nur Properties durchlässt
 * die eine DIFF Annotation besitzen.
 * @see DIFF
 * @author t.tasior, 05.03.2018
 */
public class DefaultPropertyFilter implements IPropertyFilter
{

  @Override
  public boolean canMatch(@NotNull IProperty<?, ?> pProperty)
  {
    return (pProperty.getDescription().getAnnotation(DIFF.class) != null)
        | (pProperty.getDescription().getType().getAnnotation(DIFF.class) != null);
  }
}
