package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.propertly.core.spi.IProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Eine Implementierung lässt nur Objekte durch die
 * zum Vergleich herangezogen werden dürfen.
 * @author t.tasior, 31.01.2018
 */
public interface IPropertyFilter
{
  /**
   * Liefert true, wenn das übergebene Property verglichen werden darf.
   *
   * @param pProperty wird überprüft ob es verglichen werden darf.
   * @return true, wenn der Vergleich zulässig ist.
   */
  boolean canMatch(@NotNull IProperty<?, ?> pProperty);

  /**
   * Wird vom PropertypitMatcher zweimal aufgerufen, vor jedem
   * Datenmodell einmal. Einstellungen die für das erste Datenmodell
   * vorgenommen wurden, können somit verworfen und für den zweiten Durchlauf
   * neu konfiguriert werden.
   */
  default void reset()
  {
    //tut nichts
  }
}
