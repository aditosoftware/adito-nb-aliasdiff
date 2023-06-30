package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.propertly.core.spi.IPropertyDescription;

/**
 * @author C.Stadler on 08.02.2017.
 */
class PropertyValue
{
  IPropertyDescription property;
  Object newValue;

  public PropertyValue(IPropertyDescription pProperty, Object pNewValue)
  {
    property = pProperty;
    newValue = pNewValue;
  }

  public IPropertyDescription getProperty()
  {
    return property;
  }

  public Object getNewValue()
  {
    return newValue;
  }
}
