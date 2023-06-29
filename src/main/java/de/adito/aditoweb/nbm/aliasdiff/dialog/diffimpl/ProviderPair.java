package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.system.crmcomponents.IDataModel;
import de.adito.propertly.core.api.Hierarchy;
import de.adito.propertly.core.spi.*;
import de.adito.propertly.core.spi.extension.AbstractIndexedMutablePPP;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;


/**
 * @author t.tasior, 07.02.2018
 */
public class ProviderPair extends AbstractPair
{
  private boolean bothPresent = false;
  private IPropertyPitProvider leftProvider;
  private IPropertyPitProvider rightProvider;
  private String name = null;

  private ProviderRestoreHandler handler = null;
  private RestoreHandler restoreHandler = null;

  public ProviderPair(PropertyNode pHost)
  {
    super(pHost);
  }

  public void setProvider(@NotNull EDirection pDirection, IPropertyPitProvider pProvider)
  {
    if ((pProvider != null) & (name == null))
    {
      IProperty ownProp = pProvider.getPit().getOwnProperty();
      Object value = ownProp.getValue();
      if (value instanceof IDataModel)
        name = ((IDataModel) value).getName();

      name = ownProp.getName();
    }
    switch (pDirection)
    {
      case LEFT:
        leftProvider = pProvider;
        break;

      case RIGHT:
        rightProvider = pProvider;
        break;
    }
    bothPresent = (leftProvider != null) & (rightProvider != null);
  }

  @Override
  public Object getManagedObject(@NotNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT)
      return leftProvider;

    if (pDirection == EDirection.RIGHT)
      return rightProvider;

    return null;
  }


  private boolean _emptyAndNull()
  {
    if ((leftProvider != null) & (rightProvider != null))
      return false;

    return false;
  }

  @Override
  public EDiff typeOfDiff(@NotNull EDirection pDirection)
  {
    if (bothPresent | _emptyAndNull())
      return EDiff.BOTH_PRESENT;

    if (((leftProvider != null) & (rightProvider != null)))//isValueApplied() & 
      return EDiff.EQUAL;

    if ((leftProvider == null) & (rightProvider == null))
      return EDiff.DELETED;

    if ((pDirection == EDirection.LEFT) & (leftProvider == null))
      return EDiff.MISSING;

    if ((pDirection == EDirection.RIGHT) & (rightProvider == null))
      return EDiff.MISSING;

    return EDiff.NOT_EVALUATED;
  }

  @Override
  public boolean isEqual()
  {
    if ((leftProvider == null) & (rightProvider == null))
      return true;

    if ((leftProvider != null) & (rightProvider == null))
      return leftProvider.getPit().getProperties().size() == 0;//Länge 0 und null gleich behandeln

    if ((leftProvider == null) & (rightProvider != null))
      return rightProvider.getPit().getProperties().size() == 0;//null und Länge 0 gleich

    return true;

  }

  private IPropertyPitProvider _createScheme(IPropertyPitProvider pProvider)
  {
    String name = pProvider.getPit().getOwnProperty().getName();
    Class type = pProvider.getPit().getOwnProperty().getType();
    return FACTORY.create(name, type);
  }

  public IPropertyPitProvider _activate(IPropertyPitProvider pParent, IPropertyPitProvider pScheme)
  {
    if (pParent instanceof AbstractIndexedMutablePPP)
    {
      AbstractIndexedMutablePPP parentPPP = (AbstractIndexedMutablePPP) pParent;
      IProperty property = parentPPP.findProperty(pScheme.getPit().getOwnProperty().getDescription());

      if (parentPPP.findProperty(pScheme.getPit().getOwnProperty().getDescription()) == null)
        property = parentPPP.addProperty(pScheme);

      return (IPropertyPitProvider) property.getValue();
    }
    else
    {
      IPropertyDescription description = pScheme.getPit().getOwnProperty().getDescription();
      return (IPropertyPitProvider) pParent.getPit().setValue(description, pScheme);
    }
  }

  private void _delete(IPropertyPitProvider pParent, IPropertyDescription pDescription)
  {
    if (pParent instanceof AbstractIndexedMutablePPP)
    {
      AbstractIndexedMutablePPP parentPPP = (AbstractIndexedMutablePPP) pParent;
      IProperty property = parentPPP.findProperty(pDescription);
      parentPPP.removeProperty(property);
    }
    else
    {
      pParent.getPit().setValue(pDescription, null);
    }
  }
  

  @Override
  public void createDown(@NotNull EDirection pDirection, IPropertyPitProvider pParent)
  {
    if (pDirection == EDirection.RIGHT)
    {
      if ((rightProvider == null) & (leftProvider != null))
      {
        rightProvider = _activate(pParent, _createScheme(leftProvider));
        _createHandler();
        handler.providerCreated(rightProvider);
      }

      if (rightProvider != null)
        getHost().createDown(pDirection, rightProvider);
    }

    if (pDirection == EDirection.LEFT)
    {
      if ((leftProvider == null) & (rightProvider != null))
      {
        leftProvider = _activate(pParent, _createScheme(rightProvider));
        _createHandler();
        handler.providerCreated(leftProvider);
      }

      if (leftProvider != null)
        getHost().createDown(pDirection, leftProvider);
    }
  }

  private void _createHandler()
  {
    if (handler == null)
      handler = new ProviderRestoreHandler();
  }

  @Override
  public void deleteDown(@NotNull EDirection pDirection)
  {
    _createHandler();

    if (pDirection == EDirection.RIGHT & rightProvider != null)
    {
      handler.providerToRemove(pDirection, rightProvider);
      getHost().deleteDown(pDirection);
      rightProvider = null;
    }

    if (pDirection == EDirection.LEFT & leftProvider != null)
    {
      handler.providerToRemove(pDirection, leftProvider);
      getHost().deleteDown(pDirection);
      leftProvider = null;
    }

  }
  

  @Override
  protected void create(@NotNull EDirection pDirection, AtomicReference pRef)
  {
    if (pDirection == EDirection.LEFT)
    {
      if (leftProvider == null & restoreHandler == null)
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setPropertly(null);//merkt sich das der Provider auf dieser Seite null war
        AtomicReference<Object> ref = new AtomicReference<>();
        getHost().parent().getPair().create(pDirection,  ref);
        Object parentObj = ref.get();

        if (parentObj instanceof AbstractIndexedMutablePPP)
        {
          AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parentObj;
          IProperty property = mutableParent.findProperty(rightProvider.getPit().getOwnProperty().getName());
          if (property != null)
          {
            leftProvider = (IPropertyPitProvider) property.getValue();
          }
          else
          {
            IPropertyPitProvider value = new Hierarchy<>(rightProvider.getPit().getOwnProperty().getName(), rightProvider).getValue();
            leftProvider = (IPropertyPitProvider) mutableParent.addProperty(value).getValue();
          }
        }
        else if (parentObj instanceof IPropertyPitProvider)
        {
          IPropertyPitProvider prov = (IPropertyPitProvider) parentObj;
          leftProvider = (IPropertyPitProvider) prov.getPit().getValue(rightProvider.getPit().getOwnProperty().getDescription());
        }
      }

      pRef.set(leftProvider);
    }

    if (pDirection == EDirection.RIGHT)
    {
      if (rightProvider == null & restoreHandler == null)
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setPropertly(null);//merkt sich das der Provider auf dieser Seite null war
        AtomicReference<Object> ref = new AtomicReference<>();
        getHost().parent().getPair().create(pDirection, ref);
        Object parentObj = ref.get();

        if (parentObj instanceof AbstractIndexedMutablePPP)
        {
          AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parentObj;
          IProperty property = mutableParent.findProperty(leftProvider.getPit().getOwnProperty().getName());
          if (property != null)
          {
            rightProvider = (IPropertyPitProvider) property.getValue();
          }
          else
          {
            IPropertyPitProvider value = new Hierarchy<>(leftProvider.getPit().getOwnProperty().getName(), leftProvider).getValue();
            rightProvider = (IPropertyPitProvider) mutableParent.addProperty(value).getValue();
          }
        }
        else if (parentObj instanceof IPropertyPitProvider)
        {
          IPropertyPitProvider prov = (IPropertyPitProvider) parentObj;
          rightProvider = (IPropertyPitProvider) prov.getPit().getValue(leftProvider.getPit().getOwnProperty().getDescription());
        }
      }
      pRef.set(rightProvider);

    }
  }


  @Override
  public boolean containsProvider(IPropertyPitProvider pProvider)
  {
    String name = pProvider.getPit().getOwnProperty().getName();
    if ((leftProvider != null) && leftProvider.getPit().getOwnProperty().getName().equalsIgnoreCase(name))
      return true;

    if ((rightProvider != null) && rightProvider.getPit().getOwnProperty().getName().equalsIgnoreCase(name))
      return true;

    return false;
  }

  public String nameForDisplay(EDirection pDirection)
  {

    if (pDirection == EDirection.LEFT)
    {
      if (leftProvider != null && leftProvider.getPit().isValid())
        return leftProvider.getPit().getOwnProperty().getName();

      return name;
    }

    if (pDirection == EDirection.RIGHT)
    {
      if (rightProvider != null && rightProvider.getPit().isValid())
        return rightProvider.getPit().getOwnProperty().getName();

      return name;
    }

    return name;
  }

  public String nameForDebugPrint()
  {
    String leftPart = EDirection.LEFT.name() + " ____ ";
    if (leftProvider != null)
      leftPart = EDirection.LEFT.name() + " " + _buildName(leftProvider);

    String rightPart = EDirection.RIGHT.name() + " ____ ";
    if (rightProvider != null)
      rightPart = EDirection.RIGHT.name() + " " + _buildName(rightProvider);

    return leftPart + "   " + rightPart;
  }

  @Override
  public String nameForIdentification()
  {
    if (name == null)
      return "Root";

    return name;
  }

  private String _buildName(IPropertyPitProvider pProvider)
  {
    IProperty ownProp = pProvider.getPit().getOwnProperty();
    return ownProp.getName();
  }

  /**
   * Für Anzeige im IntelliJ Debugger
   */
  @Override
  public String toString()
  {
    return nameForDisplay(EDirection.LEFT);//getName() + "  Prov";
  }

  @Override
  public void update(@NotNull EDirection pDirection)
  {
    if (restoreHandler != null)
      return;

    if ((leftProvider != null) & (rightProvider != null))
    {
      getHost().postUpdateDown(pDirection);
      return;
    }

    if (pDirection == EDirection.LEFT)//delete / create linkes Datenmodell
    {
      if (rightProvider == null)
      {
        //delete links
        restoreHandler = new RestoreHandler(pDirection);

        if (leftProvider.getPit().isValid())
        {
          IPropertyPitProvider propertly = new Hierarchy<>(leftProvider.getPit().getOwnProperty().getName(), leftProvider).getValue();
          restoreHandler.setPropertly(propertly);//merkt sich das der Provider auf dieser Seite null war
        }
        else
          restoreHandler.setPropertly(leftProvider);


        if (leftProvider.getPit().isValid())
        {
          IPropertyPitProvider parent = leftProvider.getPit().getParent();

          if (parent instanceof AbstractIndexedMutablePPP)
          {
            AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parent;
            mutableParent.removeProperty(leftProvider.getPit().getOwnProperty());
          }
        }

        leftProvider = null;
        getHost().postUpdateDown(pDirection);
      }
      else
      {
        //create linkes Datenmodell
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setPropertly(null);//merkt sich das der Provider auf dieser Seite null war
        AtomicReference<Object> ref = new AtomicReference<>();
        getHost().parent().getPair().create(pDirection, ref);
        Object parentObj = ref.get();

        if (parentObj instanceof AbstractIndexedMutablePPP)
        {
          AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parentObj;
          IProperty property = mutableParent.findProperty(rightProvider.getPit().getOwnProperty().getName());
          if (property != null)
          {
            leftProvider = (IPropertyPitProvider) property.getValue();
            getHost().postUpdateDown(pDirection);
          }
          else
          {
            IPropertyPitProvider value = new Hierarchy<>(rightProvider.getPit().getOwnProperty().getName(), rightProvider).getValue();
            leftProvider = (IPropertyPitProvider) mutableParent.addProperty(value).getValue();
            getHost().postUpdateDown(pDirection);
          }
        }
        else if (parentObj instanceof IPropertyPitProvider)
        {
          IPropertyPitProvider prov = (IPropertyPitProvider) parentObj;
          leftProvider = (IPropertyPitProvider) prov.getPit().getValue(rightProvider.getPit().getOwnProperty().getDescription());
          getHost().postUpdateDown(pDirection);
        }
      }
    }

    if (pDirection == EDirection.RIGHT)//delete / create rechtes Datenmodell
    {
      if (leftProvider == null)
      {
       
        //delete rechts
        restoreHandler = new RestoreHandler(pDirection);

        if (rightProvider.getPit().isValid())
        {
          IPropertyPitProvider propertly = new Hierarchy<>(rightProvider.getPit().getOwnProperty().getName(), rightProvider).getValue();
          restoreHandler.setPropertly(propertly);//merkt sich das der Provider auf dieser Seite null war
        }
        else
          restoreHandler.setPropertly(rightProvider);
        

        if (rightProvider.getPit().isValid())
        {
          IPropertyPitProvider parent = rightProvider.getPit().getParent();

          if (parent instanceof AbstractIndexedMutablePPP)
          {
            AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parent;
            mutableParent.removeProperty(rightProvider.getPit().getOwnProperty());
          }
        }

        rightProvider = null;
        getHost().postUpdateDown(pDirection);
      }
      else
      {//create rechtes Datenmodell
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setPropertly(null);//merkt sich das der Provider auf dieser Seite null war
        AtomicReference<Object> ref = new AtomicReference<>();
        getHost().parent().getPair().create(pDirection, ref);
        Object parentObj = ref.get();

        if (parentObj instanceof AbstractIndexedMutablePPP)
        {
          AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parentObj;
          IProperty property = mutableParent.findProperty(leftProvider.getPit().getOwnProperty().getName());
          if (property != null)
          {
            rightProvider = (IPropertyPitProvider) property.getValue();
            getHost().postUpdateDown(pDirection);
          }
          else
          {//Tiefe Kopie des linken Datenmodells erzeugen
            IPropertyPitProvider value = new Hierarchy<>(leftProvider.getPit().getOwnProperty().getName(), leftProvider).getValue();
            rightProvider = (IPropertyPitProvider) mutableParent.addProperty(value).getValue();
            getHost().postUpdateDown(pDirection);
          }
        }
        else if (parentObj instanceof IPropertyPitProvider)
        {
          IPropertyPitProvider prov = (IPropertyPitProvider) parentObj;
          rightProvider = (IPropertyPitProvider) prov.getPit().getValue(leftProvider.getPit().getOwnProperty().getDescription());
          getHost().postUpdateDown(pDirection);
        }
      }
    }
  }

  @Override
  public void restore()
  {
    if ((restoreHandler != null))
    {
      //Hier restoren
      EDirection direction = restoreHandler.getDirection();
      Object propertly = restoreHandler.getPropertly();

      if (direction == EDirection.LEFT)
      {
        if (RestoreHandler.isSet(propertly) & propertly == null)
        {
          if (leftProvider.getPit().isValid())
          {
            IPropertyPitProvider parent = leftProvider.getPit().getParent();

            if (parent instanceof AbstractIndexedMutablePPP)
            {
              AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parent;
              mutableParent.removeProperty(leftProvider.getPit().getOwnProperty());
            }
          }
          restoreHandler = null;
          leftProvider = null;
          getHost().restoreDown();
        }
        else
        {
          AtomicReference<Object> ref = new AtomicReference<>();
          getHost().parent().getPair().create(direction, ref);
          Object parentObj = ref.get();
          if (parentObj==null)
          {
            getHost().parent().getPair().restore();
            return;
          }

          if (parentObj instanceof AbstractIndexedMutablePPP)
          {
            AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parentObj;
            IProperty property = mutableParent.findProperty(name);
            if (property != null)
            {
              leftProvider = (IPropertyPitProvider) property.getValue();
              getHost().restoreDown();
            }
            else
            {
              IPropertyPitProvider value = new Hierarchy<>(name, (IPropertyPitProvider) propertly).getValue();
              leftProvider = (IPropertyPitProvider) mutableParent.addProperty(value).getValue();
              getHost().restoreDown();
            }
          }
          else if (parentObj instanceof IPropertyPitProvider)
          {
            IPropertyPitProvider prov = (IPropertyPitProvider) parentObj;
            leftProvider = (IPropertyPitProvider) prov.getPit().findProperty(name).getValue();//(IPropertyPitProvider) prov.getPit().getValue(rightProvider.getPit().getOwnProperty().getDescription());


            getHost().restoreDown();
          }
        }
      }
//**************************************************************
      if (direction == EDirection.RIGHT)
      {
        if (RestoreHandler.isSet(propertly) & propertly == null)
        {
          if (rightProvider.getPit().isValid())
          {
            IPropertyPitProvider parent = rightProvider.getPit().getParent();

            if (parent instanceof AbstractIndexedMutablePPP)
            {
              AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parent;
              mutableParent.removeProperty(rightProvider.getPit().getOwnProperty());
            }
          }
          restoreHandler = null;
          rightProvider = null;
          getHost().restoreDown();
        }
        else
        {
          AtomicReference<Object> ref = new AtomicReference<>();
          getHost().parent().getPair().create(direction, ref);
          Object parentObj = ref.get();
          if (parentObj==null)
          {
            getHost().parent().getPair().restore();
            return;
          }
    
          if (parentObj instanceof AbstractIndexedMutablePPP)
          {
            AbstractIndexedMutablePPP mutableParent = (AbstractIndexedMutablePPP) parentObj;
            IProperty property = mutableParent.findProperty(name);
            if (property != null)
            {
              rightProvider = (IPropertyPitProvider) property.getValue();
              getHost().restoreDown();
            }
            else
            {
              IPropertyPitProvider value = new Hierarchy<>(name, (IPropertyPitProvider) propertly).getValue();
              rightProvider = (IPropertyPitProvider) mutableParent.addProperty(value).getValue();
              getHost().restoreDown();
            }
          }
          else if (parentObj instanceof IPropertyPitProvider)
          {
            IPropertyPitProvider prov = (IPropertyPitProvider) parentObj;
            rightProvider = (IPropertyPitProvider) prov.getPit().findProperty(name).getValue();//(IPropertyPitProvider) prov.getPit().getValue(rightProvider.getPit().getOwnProperty().getDescription());
            
            
            getHost().restoreDown();
          }
        }
      }
      restoreHandler = null;
    }
    else//restoreHandler = null;
    {
      getHost().restoreDown();
    }

  }
}
