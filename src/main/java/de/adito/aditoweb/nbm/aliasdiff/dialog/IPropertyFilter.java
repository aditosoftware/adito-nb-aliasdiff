package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.propertly.core.spi.IProperty;

import java.util.function.Predicate;

/**
 * An implementation only allows objects through that may be used for comparison.
 *
 * @author t.tasior, 31.01.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public interface IPropertyFilter extends Predicate<IProperty<?, ?>>
{

  /**
   * Called by the PropertypitMatcher twice, before each data model.
   * Settings made for the first data model can thus be discarded and reconfigured for the second run.
   */
  default void reset()
  {
    // nothing by default
  }

}
