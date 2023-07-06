package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.system.crmcomponents.*;
import de.adito.propertly.core.api.Hierarchy;
import de.adito.propertly.core.spi.*;
import de.adito.propertly.core.spi.extension.AbstractIndexedMutablePPP;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Implementation of {@link AbstractPair} that is based on an {@link IPropertyPitProvider}
 *
 * @author t.tasior, 07.02.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public class ProviderPair extends AbstractPair
{
  private boolean bothPresent = false;
  private IPropertyPitProvider<?, ?, ?> leftProvider;
  private IPropertyPitProvider<?, ?, ?> rightProvider;
  private String name = null;
  private RestoreHandler restoreHandler = null;

  public ProviderPair(@NonNull PropertyNode pHost)
  {
    super(pHost);
  }

  @Override
  public void setProvider(@NonNull EDirection pDirection, @Nullable IPropertyPitProvider<?, ?, ?> pProvider)
  {
    if (pProvider != null && name == null)
    {
      IProperty<?, ?> ownProp = pProvider.getPit().getOwnProperty();
      Object value = ownProp.getValue();
      if (value instanceof IDataModel<?, ?>)
        name = ((IDataModel<?, ?>) value).getName();

      name = ownProp.getName();
    }

    if (pDirection == EDirection.LEFT)
      leftProvider = pProvider;
    else if (pDirection == EDirection.RIGHT)
      rightProvider = pProvider;

    bothPresent = leftProvider != null && rightProvider != null;
  }

  @Nullable
  @Override
  public Object getManagedObject(@NonNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT)
      return leftProvider;

    if (pDirection == EDirection.RIGHT)
      return rightProvider;

    return null;
  }

  @NonNull
  @Override
  public EDiff typeOfDiff(@NonNull EDirection pDirection)
  {
    if (bothPresent)
      return EDiff.BOTH_PRESENT;

    if ((leftProvider != null && rightProvider != null))
      return EDiff.EQUAL;

    if (leftProvider == null && rightProvider == null)
      return EDiff.DELETED;

    if (pDirection == EDirection.LEFT && leftProvider == null)
      return EDiff.MISSING;

    if (pDirection == EDirection.RIGHT && rightProvider == null)
      return EDiff.MISSING;

    return EDiff.NOT_EVALUATED;
  }

  @Override
  public boolean isEqual()
  {
    if (leftProvider == null && rightProvider == null)
      return true;

    if (leftProvider != null && rightProvider == null)
      return leftProvider.getPit().getProperties().isEmpty();

    if (leftProvider == null)
      return rightProvider.getPit().getProperties().isEmpty();

    return true;
  }

  @Override
  public void createDown(@NonNull EDirection pDirection, @Nullable IPropertyPitProvider<?, ?, ?> pParent)
  {
    if (pDirection == EDirection.RIGHT)
    {
      if (rightProvider == null && leftProvider != null)
        rightProvider = activate(pParent, createScheme(leftProvider));

      if (rightProvider != null)
        getHost().createDown(pDirection, rightProvider);
    }

    if (pDirection == EDirection.LEFT)
    {
      if (leftProvider == null && rightProvider != null)
        leftProvider = activate(pParent, createScheme(rightProvider));

      if (leftProvider != null)
        getHost().createDown(pDirection, leftProvider);
    }
  }

  @Override
  public void deleteDown(@NonNull EDirection pDirection)
  {
    if (pDirection == EDirection.RIGHT && rightProvider != null)
    {
      getHost().deleteDown(pDirection);
      rightProvider = null;
    }

    if (pDirection == EDirection.LEFT && leftProvider != null)
    {
      getHost().deleteDown(pDirection);
      leftProvider = null;
    }
  }

  @Override
  protected IPropertyPitProvider<?, ?, ?> create(@NonNull EDirection pDirection) //NOSONAR I won't refactor this, because something will break for sure..
  {
    if (pDirection == EDirection.LEFT)
    {
      if (leftProvider == null && restoreHandler == null)
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setModel(null);

        IPropertyPitProvider<?, ?, ?> parentObj = getHost().parent().getPair().create(pDirection);
        if (parentObj instanceof AbstractIndexedMutablePPP)
        {
          AbstractIndexedMutablePPP<?, ?, ?> mutableParent = (AbstractIndexedMutablePPP<?, ?, ?>) parentObj;
          IProperty<?, ?> property = mutableParent.findProperty(rightProvider.getPit().getOwnProperty().getName());
          if (property != null)
            leftProvider = (IPropertyPitProvider<?, ?, ?>) property.getValue();
          else
          {
            IPropertyPitProvider<?, ?, ?> value = new Hierarchy<>(rightProvider.getPit().getOwnProperty().getName(), rightProvider).getValue();

            //noinspection unchecked,rawtypes
            leftProvider = (IPropertyPitProvider<?, ?, ?>) ((AbstractIndexedMutablePPP) mutableParent).addProperty(value).getValue();
          }
        }
        else if (parentObj != null)
          //noinspection unchecked,rawtypes
          leftProvider = (IPropertyPitProvider) ((IPropertyPitProvider) parentObj).getPit().getValue(rightProvider.getPit().getOwnProperty().getDescription());
      }

      return leftProvider;
    }

    if (pDirection == EDirection.RIGHT)
    {
      if (rightProvider == null && restoreHandler == null)
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setModel(null);

        IPropertyPitProvider<?, ?, ?> parentObj = getHost().parent().getPair().create(pDirection);
        if (parentObj instanceof AbstractIndexedMutablePPP<?, ?, ?>)
        {
          AbstractIndexedMutablePPP<?, ?, ?> mutableParent = (AbstractIndexedMutablePPP<?, ?, ?>) parentObj;
          IProperty<?, ?> property = mutableParent.findProperty(leftProvider.getPit().getOwnProperty().getName());
          if (property != null)
            rightProvider = (IPropertyPitProvider<?, ?, ?>) property.getValue();
          else
          {
            IPropertyPitProvider<?, ?, ?> value = new Hierarchy<>(leftProvider.getPit().getOwnProperty().getName(), leftProvider).getValue();

            //noinspection unchecked,rawtypes
            rightProvider = (IPropertyPitProvider<?, ?, ?>) ((AbstractIndexedMutablePPP) mutableParent).addProperty(value).getValue();
          }
        }
        else if (parentObj != null)
          //noinspection unchecked,rawtypes
          rightProvider = (IPropertyPitProvider) ((IPropertyPitProvider) parentObj).getPit().getValue(leftProvider.getPit().getOwnProperty().getDescription());
      }

      return rightProvider;
    }

    return null;
  }

  @Override
  public boolean containsProvider(@NonNull IPropertyPitProvider<?, ?, ?> pProvider)
  {
    String nameToCheck = pProvider.getPit().getOwnProperty().getName();
    return (leftProvider != null && leftProvider.getPit().getOwnProperty().getName().equalsIgnoreCase(nameToCheck)) ||
        (rightProvider != null && rightProvider.getPit().getOwnProperty().getName().equalsIgnoreCase(nameToCheck));
  }

  @Nullable
  public String nameForDisplay(@NonNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT)
      return nameForDisplay(leftProvider, name);
    else if (pDirection == EDirection.RIGHT)
      return nameForDisplay(rightProvider, name);
    return name;
  }

  @NonNull
  @Override
  public String nameForIdentification()
  {
    if (name == null)
      return "Root";
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
    if (restoreHandler != null)
      return;

    if (leftProvider != null && rightProvider != null)
    {
      getHost().postUpdateDown(pDirection);
      return;
    }

    if (pDirection == EDirection.LEFT) //delete / create left model
    {
      if (rightProvider == null) // delete left
      {
        restoreHandler = new RestoreHandler(pDirection);
        if (leftProvider != null && leftProvider.getPit().isValid())
          restoreHandler.setModel(new Hierarchy<>(leftProvider.getPit().getOwnProperty().getName(), leftProvider).getValue());
        else
          restoreHandler.setModel(leftProvider);

        if (leftProvider != null && leftProvider.getPit().isValid())
        {
          IPropertyPitProvider<?, ?, ?> parent = leftProvider.getPit().getParent();
          if (parent instanceof AbstractIndexedMutablePPP<?, ?, ?>)
            //noinspection unchecked,rawtypes
            ((AbstractIndexedMutablePPP) parent).removeProperty(leftProvider.getPit().getOwnProperty());
        }

        leftProvider = null;
        getHost().postUpdateDown(pDirection);
      }
      else // create left
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setModel(null);

        IPropertyPitProvider<?, ?, ?> parentObj = getHost().parent().getPair().create(pDirection);
        if (parentObj instanceof AbstractIndexedMutablePPP<?, ?, ?>)
        {
          AbstractIndexedMutablePPP<?, ?, ?> mutableParent = (AbstractIndexedMutablePPP<?, ?, ?>) parentObj;
          IProperty<?, ?> property = mutableParent.findProperty(rightProvider.getPit().getOwnProperty().getName());
          if (property != null)
          {
            leftProvider = (IPropertyPitProvider<?, ?, ?>) property.getValue();
            getHost().postUpdateDown(pDirection);
          }
          else
          {
            //noinspection unchecked,rawtypes
            leftProvider = (IPropertyPitProvider) ((AbstractIndexedMutablePPP) mutableParent)
                .addProperty(new Hierarchy<>(rightProvider.getPit().getOwnProperty().getName(), rightProvider).getValue())
                .getValue();
            getHost().postUpdateDown(pDirection);
          }
        }
        else if (parentObj != null)
        {
          //noinspection unchecked,rawtypes
          leftProvider = (IPropertyPitProvider) ((IPropertyPitProvider) parentObj).getPit().getValue(rightProvider.getPit().getOwnProperty().getDescription());
          getHost().postUpdateDown(pDirection);
        }
      }
    }
    else if (pDirection == EDirection.RIGHT) //delete / create right model
    {
      if (leftProvider == null) // delete right
      {
        restoreHandler = new RestoreHandler(pDirection);
        if (rightProvider != null && rightProvider.getPit().isValid())
          restoreHandler.setModel(new Hierarchy<>(rightProvider.getPit().getOwnProperty().getName(), rightProvider).getValue());
        else
          restoreHandler.setModel(rightProvider);

        if (rightProvider != null && rightProvider.getPit().isValid())
        {
          IPropertyPitProvider<?, ?, ?> parent = rightProvider.getPit().getParent();
          if (parent instanceof AbstractIndexedMutablePPP<?, ?, ?>)
            //noinspection unchecked,rawtypes
            ((AbstractIndexedMutablePPP) parent).removeProperty(rightProvider.getPit().getOwnProperty());
        }

        rightProvider = null;
        getHost().postUpdateDown(pDirection);
      }
      else // create right
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setModel(null);

        IPropertyPitProvider<?, ?, ?> parentObj = getHost().parent().getPair().create(pDirection);
        if (parentObj instanceof AbstractIndexedMutablePPP<?, ?, ?>)
        {
          AbstractIndexedMutablePPP<?, ?, ?> mutableParent = (AbstractIndexedMutablePPP<?, ?, ?>) parentObj;
          IProperty<?, ?> property = mutableParent.findProperty(leftProvider.getPit().getOwnProperty().getName());
          if (property != null)
          {
            rightProvider = (IPropertyPitProvider<?, ?, ?>) property.getValue();
            getHost().postUpdateDown(pDirection);
          }
          else
          {
            //noinspection unchecked,rawtypes
            rightProvider = (IPropertyPitProvider<?, ?, ?>) ((AbstractIndexedMutablePPP) mutableParent)
                .addProperty(new Hierarchy<>(leftProvider.getPit().getOwnProperty().getName(), leftProvider).getValue())
                .getValue();
            getHost().postUpdateDown(pDirection);
          }
        }
        else if (parentObj != null)
        {
          //noinspection unchecked,rawtypes
          rightProvider = (IPropertyPitProvider) ((IPropertyPitProvider) parentObj).getPit().getValue(leftProvider.getPit().getOwnProperty().getDescription());
          getHost().postUpdateDown(pDirection);
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
      Object property = restoreHandler.getModel();

      if (direction == EDirection.LEFT)
      {
        if (RestoreHandler.isSet(property) && property == null)
        {
          if (leftProvider.getPit().isValid())
          {
            IPropertyPitProvider<?, ?, ?> parent = leftProvider.getPit().getParent();
            if (parent instanceof AbstractIndexedMutablePPP)
              //noinspection unchecked,rawtypes
              ((AbstractIndexedMutablePPP) parent).removeProperty(leftProvider.getPit().getOwnProperty());
          }
          restoreHandler = null;
          leftProvider = null;
          getHost().restoreDown();
        }
        else
        {
          IPropertyPitProvider<?, ?, ?> parentObj = getHost().parent().getPair().create(direction);
          if (parentObj == null)
          {
            getHost().parent().getPair().restore();
            return;
          }

          if (parentObj instanceof AbstractIndexedMutablePPP)
          {
            AbstractIndexedMutablePPP<?, ?, ?> mutableParent = (AbstractIndexedMutablePPP<?, ?, ?>) parentObj;
            IProperty<?, ?> prop = mutableParent.findProperty(name);
            if (prop != null)
            {
              leftProvider = (IPropertyPitProvider<?, ?, ?>) prop.getValue();
              getHost().restoreDown();
            }
          }
          else
          {
            //noinspection rawtypes
            leftProvider = (IPropertyPitProvider) Objects.requireNonNull(((IPropertyPitProvider) parentObj).getPit().findProperty(name)).getValue();
            getHost().restoreDown();
          }
        }
      }
      else if (direction == EDirection.RIGHT)
      {
        if (RestoreHandler.isSet(property) && property == null)
        {
          if (rightProvider.getPit().isValid())
          {
            IPropertyPitProvider<?, ?, ?> parent = rightProvider.getPit().getParent();
            if (parent instanceof AbstractIndexedMutablePPP)
              //noinspection unchecked,rawtypes
              ((AbstractIndexedMutablePPP) parent).removeProperty(rightProvider.getPit().getOwnProperty());
          }
          restoreHandler = null;
          rightProvider = null;
          getHost().restoreDown();
        }
        else
        {
          IPropertyPitProvider<?, ?, ?> parentObj = getHost().parent().getPair().create(direction);
          if (parentObj == null)
          {
            getHost().parent().getPair().restore();
            return;
          }

          if (parentObj instanceof AbstractIndexedMutablePPP)
          {
            AbstractIndexedMutablePPP<?, ?, ?> mutableParent = (AbstractIndexedMutablePPP<?, ?, ?>) parentObj;
            IProperty<?, ?> prop = mutableParent.findProperty(name);
            if (prop != null)
            {
              rightProvider = (IPropertyPitProvider<?, ?, ?>) prop.getValue();
              getHost().restoreDown();
            }
          }
          else
          {
            //noinspection rawtypes
            rightProvider = (IPropertyPitProvider<?, ?, ?>) Objects.requireNonNull(((IPropertyPitProvider) parentObj).getPit().findProperty(name)).getValue();
            getHost().restoreDown();
          }
        }
      }

      restoreHandler = null;
    }
    else
      getHost().restoreDown();
  }

  /**
   * Creates a new instance of an {@link IPropertyPitProvider} that has the same type as the given one
   *
   * @param pProvider Provider to determine the type from
   * @param <T>       type of the provider
   * @return a newly created instance
   */
  @NonNull
  private <T extends IPropertyPitProvider<?, ?, ?>> T createScheme(@NonNull T pProvider)
  {
    //noinspection unchecked
    return new DataModelFactory().create(pProvider.getPit().getOwnProperty().getName(),
                                         (Class<T>) pProvider.getPit().getOwnProperty().getType());
  }

  /**
   * Updates the parent, so that the given scheme will be included
   *
   * @param pParent Parent that should contain the given scheme
   * @param pScheme Scheme that should be included
   * @param <T>     type of the scheme
   * @return the included scheme
   */
  @Nullable
  private <T extends IPropertyPitProvider<?, ?, ?>> T activate(@Nullable IPropertyPitProvider<?, ?, ?> pParent, @NonNull T pScheme)
  {
    if (pParent instanceof AbstractIndexedMutablePPP)
    {
      AbstractIndexedMutablePPP<?, ?, ?> parentPPP = (AbstractIndexedMutablePPP<?, ?, ?>) pParent;

      //noinspection unchecked,rawtypes
      IProperty<?, ?> property = parentPPP.findProperty((IPropertyDescription) pScheme.getPit().getOwnProperty().getDescription());
      if (property == null)
        //noinspection unchecked,rawtypes
        property = ((AbstractIndexedMutablePPP) parentPPP).addProperty(pScheme);

      //noinspection unchecked
      return (T) property.getValue();
    }
    else if (pParent != null)
      //noinspection unchecked,rawtypes
      return (T) ((IPropertyPitProvider) pParent).getPit().setValue(pScheme.getPit().getOwnProperty().getDescription(), pScheme);

    return null;
  }

  /**
   * Returns a readable string to display the {@link IPropertyPitProvider} on GUI elements
   *
   * @param pProvider {@link IPropertyPitProvider} to get the name for
   * @param pFallback Fallback, if the provider is null or invalid
   * @return a name to display
   */
  @Nullable
  private String nameForDisplay(@Nullable IPropertyPitProvider<?, ?, ?> pProvider, @Nullable String pFallback)
  {
    if (pProvider != null && pProvider.getPit().isValid())
      return pProvider.getPit().getOwnProperty().getName();
    return pFallback;
  }
}
