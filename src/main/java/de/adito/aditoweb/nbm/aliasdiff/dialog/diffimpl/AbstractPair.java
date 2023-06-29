package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.propertly.core.spi.*;
import lombok.*;
import org.jetbrains.annotations.Nullable;

/**
 * Contains a pair of IPropertyPitProvider or IProperty objects.
 * Is embedded in a {@link PropertyNode} - the inner objects may be null.
 * <p>
 * The implementations are specialized on one single type.
 * <p>
 * Which of the two objects is to be addressed is indicated by a direction indication.
 *
 * @author t.tasior, 07.02.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 * @see EDirection
 * @see PropertyNode
 */
@RequiredArgsConstructor
public abstract class AbstractPair
{
  @NonNull
  @Getter(AccessLevel.PACKAGE)
  private final PropertyNode host;

  /**
   * @return the name of an IDataModel implementation or a name of an IProperty,
   * that uniquely identifies an object inside a model hierarchy
   */
  @NonNull
  public abstract String nameForIdentification();

  /**
   * Returns a name that should be displayed on a GUI.
   *
   * @param pDirection determines on which side the name should be displayed
   * @return a string or null
   */
  @Nullable
  public abstract String nameForDisplay(@NonNull EDirection pDirection);

  /**
   * Returns the type of the difference on the given side
   *
   * @param pDirection the searched side
   * @return an {@link EDiff} constant
   */
  @NonNull
  public abstract EDiff typeOfDiff(@NonNull EDirection pDirection);

  /**
   * @return true, if both objects are equal
   */
  public abstract boolean isEqual();

  /**
   * An object should be created and appended into the parent object
   *
   * @param pDirection determines where the object should be created
   * @param pParent    a data model to which a new value (object) is set
   */
  public abstract void createDown(@NonNull EDirection pDirection, @Nullable IPropertyPitProvider<?, ?, ?> pParent);

  /**
   * A node fires, that the children should be deleted
   *
   * @param pDirection direction where the children should be deleted
   */
  public abstract void deleteDown(@NonNull EDirection pDirection);

  /**
   * Sets the provider in the given direction
   *
   * @param pDirection direction to set the provider
   * @param pProvider  the provider to be managed, or null if there is none in the analyzed data model.
   */
  public void setProvider(@NonNull EDirection pDirection, @Nullable IPropertyPitProvider<?, ?, ?> pProvider)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the property in the given direction
   *
   * @param pDirection direction to set the property
   * @param pProperty  the property to be managed, or null if there is none in the analyzed data model.
   */
  public void setProperty(@NonNull EDirection pDirection, @Nullable IProperty<?, ?> pProperty)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the IProperty already exists.
   * It doesn't matter in which of the two data models.
   *
   * @param pProp IProperty to check
   * @return true, if it exists
   */
  public boolean containsProperty(@NonNull IProperty<?, ?> pProp)
  {
    return false;
  }

  /**
   * Returns true if the IPropertyPitProvider already exists.
   * It doesn't matter in which of the two data models.
   *
   * @param pProvider IPropertyPitProvider to check
   * @return true, if it exists
   */
  public boolean containsProvider(@NonNull IPropertyPitProvider<?, ?, ?> pProvider)
  {
    return false;
  }

  /**
   * Initiates the setting or removal of a value in the respective data model.
   *
   * @param pDirection Specifies in which of the compared data models a value is written.
   */
  public abstract void update(@NonNull EDirection pDirection);

  /**
   * Invoked to restore the original state in the data model.
   */
  public abstract void restore();

  /**
   * Creates a new datamodel in the given direction
   *
   * @param pDirection direction where the model should be created
   * @return the created model
   */
  @Nullable
  protected abstract IPropertyPitProvider<?, ?, ?> create(@NonNull EDirection pDirection);

  /**
   * Returns the managed object
   *
   * @param pDirection determines the side for which the object should be retrieved
   * @return the IProperty, IPropertyPitProvider or null
   */
  @Nullable
  public abstract Object getManagedObject(@NonNull EDirection pDirection);


}
