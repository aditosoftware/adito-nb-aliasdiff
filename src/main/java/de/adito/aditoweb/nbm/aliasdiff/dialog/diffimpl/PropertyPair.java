package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.filesystem.propertly.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.propertly.core.common.path.PropertyPath;
import de.adito.propertly.core.spi.*;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Implementation of {@link AbstractPair} that is based on a single property
 *
 * @author t.tasior, 07.02.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public class PropertyPair extends AbstractPair
{
  private RestoreHandler restoreHandler = null;
  private IProperty<?, ?> leftProperty;
  private IProperty<?, ?> rightProperty;
  private String name;

  public PropertyPair(@NonNull PropertyNode pHost)
  {
    super(pHost);
  }

  @Override
  public void setProperty(@NonNull EDirection pDirection, @Nullable IProperty<?, ?> pProperty)
  {
    if (pProperty != null && name == null)
      name = pProperty.getName();

    if (pDirection == EDirection.LEFT)
      leftProperty = pProperty;
    else if (pDirection == EDirection.RIGHT)
      rightProperty = pProperty;
  }

  @Override
  public boolean containsProperty(@NonNull IProperty<?, ?> pProp)
  {
    return (leftProperty != null && leftProperty.getName().equals(pProp.getName())) ||
        (rightProperty != null && rightProperty.getName().equals(pProp.getName()));
  }

  @Override
  public Object getManagedObject(@NonNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT)
      return leftProperty;

    if (pDirection == EDirection.RIGHT)
      return rightProperty;

    return null;
  }

  @Override
  public boolean isEqual()
  {
    if (leftProperty == null && rightProperty == null)
      return true;

    if (leftProperty == null || rightProperty == null)
      return false;

    if (Objects.equals(leftProperty.getValue(), rightProperty.getValue()))
      return true;

    return isDefaultOrNull(leftProperty) && isDefaultOrNull(rightProperty);
  }

  @NonNull
  @Override
  public EDiff typeOfDiff(@NonNull EDirection pDirection)
  {
    if (leftProperty == null && rightProperty == null)
      return EDiff.DELETED;

    if ((pDirection == EDirection.LEFT && leftProperty == null) ||
        (pDirection == EDirection.RIGHT && rightProperty == null))
      return EDiff.MISSING;

    if (leftProperty != null && rightProperty != null)
    {
      if (Objects.equals(leftProperty.getValue(), rightProperty.getValue()))
        return EDiff.EQUAL;
      else
        return EDiff.DIFFERENT;
    }

    return EDiff.NOT_EVALUATED;
  }

  @Override
  public void createDown(@NonNull EDirection pDirection, @Nullable IPropertyPitProvider<?, ?, ?> pParent) //NOSONAR I won't refactor this, because something will break for sure..
  {
    if (pDirection == EDirection.RIGHT && leftProperty != null)
    {
      if (rightProperty != null)
      {
        //noinspection unchecked,rawtypes
        ((IProperty) rightProperty).setValue(leftProperty.getValue());
      }
      else
      {
        //noinspection unchecked,rawtypes
        ((IPropertyPitProvider) Objects.requireNonNull(pParent)).getPit().setValue(leftProperty.getDescription(), leftProperty.getValue());
        rightProperty = pParent.getPit().findProperty(leftProperty.getDescription());
      }
    }

    if ((pDirection == EDirection.LEFT) && (rightProperty != null))
    {
      if (leftProperty != null)
      {
        //noinspection unchecked,rawtypes
        ((IProperty) leftProperty).setValue(rightProperty.getValue());
      }
      else
      {
        //noinspection unchecked,rawtypes
        ((IPropertyPitProvider) Objects.requireNonNull(pParent)).getPit().setValue(rightProperty.getDescription(), rightProperty.getValue());
        leftProperty = pParent.getPit().findProperty(rightProperty.getDescription());
      }
    }

    // Restore a property after it has been deleted.
    if (leftProperty == null && rightProperty == null && pParent != null)
    {
      if (pDirection == EDirection.LEFT)
        leftProperty = pParent.getPit().findProperty(name);

      if (pDirection == EDirection.RIGHT)
        rightProperty = pParent.getPit().findProperty(name);
    }

  }

  @Override
  public void deleteDown(@NonNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT)
      leftProperty = null;

    if (pDirection == EDirection.RIGHT)
      rightProperty = null;
  }

  @Nullable
  @Override
  protected IPropertyPitProvider<?, ?, ?> create(@NonNull EDirection pDirection)
  {
    // not needed, because we are in a Property-Pair, not in a Provider-Pair
    return null;
  }

  @NonNull
  @Override
  public String nameForIdentification()
  {
    return name;
  }

  @Nullable
  @Override
  public String nameForDisplay(@NonNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT)
      return nameForDisplay(leftProperty, name);

    if (pDirection == EDirection.RIGHT)
      return nameForDisplay(rightProperty, name);

    return name;
  }

  @Override
  public String toString()
  {
    return nameForDisplay(EDirection.LEFT);
  }

  @Override
  public void update(@NonNull EDirection pDirection) //NOSONAR I won't refactor this, because something will break for sure..
  {
    // as long as the old value has not been restored, no new one can be set.
    if (restoreHandler != null)
      return;

    if (pDirection == EDirection.LEFT)
    {
      if (rightProperty == null)
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setModel(leftProperty);
        getHost().parent().getPair().update(pDirection);
        leftProperty = null;
      }
      else
      {
        if (leftProperty != null)
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setValue(leftProperty.getValue());

          //noinspection unchecked,rawtypes
          ((IProperty) leftProperty).setValue(rightProperty.getValue());
        }
        else
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setModel(null);
          IPropertyPitProvider<?, ?, ?> o = getHost().parent().getPair().create(pDirection);

          //noinspection unchecked,rawtypes
          leftProperty = ((IPropertyPitProvider) Objects.requireNonNull(o)).getPit().getProperty(rightProperty.getDescription());
          getHost().parent().postUpdateDown(pDirection);
        }
      }
    }
    else if (pDirection == EDirection.RIGHT)
    {
      if (leftProperty == null)
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setModel(rightProperty);
        getHost().parent().getPair().update(pDirection);
        rightProperty = null;
      }
      else
      {
        if (rightProperty != null)
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setValue(rightProperty.getValue());

          //noinspection unchecked,rawtypes
          ((IProperty) rightProperty).setValue(leftProperty.getValue());
        }
        else
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setModel(null);
          IPropertyPitProvider<?, ?, ?> o = getHost().parent().getPair().create(pDirection);

          //noinspection unchecked,rawtypes
          rightProperty = ((IPropertyPitProvider) Objects.requireNonNull(o)).getPit().getProperty(leftProperty.getDescription());
          getHost().parent().postUpdateDown(pDirection);

        }

      }
    }
  }

  @Override
  public void restore() //NOSONAR I won't refactor this, because something will break for sure..
  {
    if (restoreHandler != null)
    {
      EDirection direction = restoreHandler.getDirection();
      Object propertly = restoreHandler.getModel();
      Object value = restoreHandler.getValue();

      if (direction == EDirection.LEFT)
      {
        if (leftProperty != null && RestoreHandler.isSet(value))
          //noinspection unchecked,rawtypes
          ((IProperty) leftProperty).setValue(value);
        else if (leftProperty != null && RestoreHandler.isSet(propertly))
        {
          restoreHandler = null;
          leftProperty = null;
          getHost().parent().getPair().restore();
        }
        else if (leftProperty == null && RestoreHandler.isSet(propertly))
        {
          //noinspection rawtypes
          IPropertyPitProvider<?, ?, ?> prov = (IPropertyPitProvider) getHost().parent().getPair().getManagedObject(direction);
          if (prov != null)
            leftProperty = prov.getPit().findProperty(name);
          else
            getHost().parent().getPair().restore();
        }
      }
      else if (direction == EDirection.RIGHT)
      {
        if (rightProperty != null && RestoreHandler.isSet(value))
          //noinspection unchecked,rawtypes
          ((IProperty) rightProperty).setValue(value);
        else if (rightProperty != null && RestoreHandler.isSet(propertly))
        {
          restoreHandler = null;
          rightProperty = null;
          getHost().parent().getPair().restore();
        }
        else if (rightProperty == null && RestoreHandler.isSet(propertly))
        {
          //noinspection rawtypes
          IPropertyPitProvider<?, ?, ?> prov = (IPropertyPitProvider) getHost().parent().getPair().getManagedObject(direction);
          if (prov != null)
            rightProperty = prov.getPit().findProperty(name);
          else
            getHost().parent().getPair().restore();
        }
      }

      restoreHandler = null;
    }
  }

  /**
   * Determines, if the given property is in its default state or has a null value
   *
   * @param pProperty Property that should be checked
   * @return true, if it is default or has a null value
   */
  private boolean isDefaultOrNull(@NonNull IProperty<?, ?> pProperty)
  {
    IHierarchy<?> rightHierarchy = ((BulkModifyHierarchy<?>) pProperty.getHierarchy()).getSourceHierarchy();
    if (rightHierarchy instanceof DataModelHierarchy)
      return DataModelHierarchy.isDefaultValue(Objects.requireNonNull(new PropertyPath(pProperty).find(rightHierarchy)));
    else
      return rightProperty.getValue() == null;
  }

  /**
   * Returns a readable string to display the property on GUI elements
   *
   * @param pProperty Property to get the name for
   * @param pFallback Fallback, if the property is null or invalid
   * @return a name to display
   */
  @Nullable
  private String nameForDisplay(@Nullable IProperty<?, ?> pProperty, @Nullable String pFallback)
  {
    if (pProperty != null && pProperty.isValid())
      return pProperty.getName() + " = " + pProperty.getValue();
    return pFallback;
  }

}
