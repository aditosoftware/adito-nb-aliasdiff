package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import lombok.*;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract info to update elements, such as tables, views and columns
 *
 * @author C.Stadler on 08.02.2017.
 * @author w.glanzer, 04.07.2023 (refactored, translated)
 */
@RequiredArgsConstructor
abstract class AbstractUpdateInfo
{
  /**
   * Name of the object that should be updated
   */
  @Getter
  @NonNull
  private String objectName;

  /**
   * Operation, how the object gets updated.
   * Null, if the kind is not specified yet.
   */
  @Nullable
  private UpdateKind updateKind;

  /**
   * Message, if something failed during this update operation
   */
  @Getter
  @Setter
  @Nullable
  private String errorMessage = null;

  /**
   * Constructor to specify the object name and the update kind in one call
   *
   * @param pObjectName Name of the object
   * @param pUpdateKind Type of the operation
   */
  protected AbstractUpdateInfo(@NonNull String pObjectName, @NonNull UpdateKind pUpdateKind)
  {
    objectName = pObjectName;
    updateKind = pUpdateKind;
  }

  @Override
  @NonNull
  public String toString()
  {
    return getObjectName() + ": " + getDescription();
  }

  /**
   * Determines, if this update operation will create a new object.
   *
   * @return true, if this operation will create a new object
   */
  public boolean isNew()
  {
    return updateKind != null && updateKind.equals(UpdateKind.NEW_OBJECT);
  }

  /**
   * Sets, that this operation will create a new object
   */
  public void setNew()
  {
    updateKind = UpdateKind.NEW_OBJECT;
  }

  /**
   * @return a human readable string representation of this update operation
   */
  @NonNull
  protected abstract String getDescription();

  /**
   * Defines the type of this operation
   */
  public enum UpdateKind
  {
    /**
     * Type that defines, that something will be created in database
     */
    NEW_OBJECT,

    /**
     * Type that defines, that something will be deleted in database
     */
    DELETE_OBJECT
  }
}
