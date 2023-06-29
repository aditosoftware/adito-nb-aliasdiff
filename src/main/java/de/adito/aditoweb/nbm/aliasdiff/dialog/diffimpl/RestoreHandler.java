package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.EDirection;
import lombok.*;
import org.jetbrains.annotations.Nullable;

/**
 * Contains models or values, that are necessary for our restore function
 *
 * @author T.Tasior, 05.04.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
@RequiredArgsConstructor
public class RestoreHandler
{
  private static final Object NOT_SET = "NOT_SET";

  /**
   * The side that should be restored
   */
  @Getter
  @NonNull
  private final EDirection direction;

  /**
   * Model that is the base of the restore
   */
  @Getter
  @Setter
  @Nullable
  private Object model = NOT_SET;

  /**
   * Contains the value of the property / {@link RestoreHandler#model}
   */
  @Getter
  @Setter
  @Nullable
  private Object value = NOT_SET;

  /**
   * Simple implementation to determine if the passed value has been set to this handler.
   *
   * @param pTest Object to test
   * @return true, if this handler "remembers" the passed object
   */
  public static boolean isSet(@Nullable Object pTest)
  {
    return pTest != NOT_SET;
  }

}
