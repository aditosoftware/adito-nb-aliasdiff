package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.filesystem.propertly.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.propertly.core.common.path.PropertyPath;
import de.adito.propertly.core.spi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 
 * @author t.tasior, 07.02.2018
 */
public class PropertyPair extends AbstractPair
{
  private PropertyRestoreHandler handler;
  private RestoreHandler restoreHandler = null;
  private IProperty leftProperty;
  private IProperty rightProperty;
  //Wir brauchen den Namen zum Wiederherstellen wenn beide Properties null sind.
  private String name;

  public PropertyPair(PropertyNode pHost)
  {
    super(pHost);
    handler = new PropertyRestoreHandler();
  }

  public void setProperty(@NotNull EDirection pDirection, IProperty pProperty)
  {
    if ((pProperty != null) & (name == null))
      name = pProperty.getName();

    switch (pDirection)
    {
      case LEFT:
        leftProperty = pProperty;
        break;

      case RIGHT:
        rightProperty = pProperty;
        break;
    }
  }

  public boolean containsProperty(@NotNull IProperty pProp)
  {
    if ((leftProperty != null) && leftProperty.getName().equals(pProp.getName()))
      return true;

    if ((rightProperty != null) && rightProperty.getName().equals(pProp.getName()))
      return true;

    return false;
  }


  @Override
  public Object getManagedObject(@NotNull EDirection pDirection)
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
    if ((leftProperty == null) & (rightProperty == null))
      return true;

    if ((leftProperty != null) & (rightProperty == null))
      return false;

    if ((leftProperty == null) & (rightProperty != null))
      return false;

    if (Objects.equals(leftProperty.getValue(), rightProperty.getValue()))
      return true;

    IHierarchy rightHierarchy = ((BulkModifyHierarchy) rightProperty.getHierarchy()).getSourceHierarchy();
    boolean rightIsDefaultOrNull;
    if (rightHierarchy instanceof DataModelHierarchy)
      rightIsDefaultOrNull = DataModelHierarchy.isDefaultValue(new PropertyPath(rightProperty).find(rightHierarchy));
    else
      rightIsDefaultOrNull = rightProperty.getValue() == null;

    IHierarchy leftHierarchy = ((BulkModifyHierarchy) leftProperty.getHierarchy()).getSourceHierarchy();
    boolean leftIsDefaultOrNull;
    if (leftHierarchy instanceof DataModelHierarchy)
      leftIsDefaultOrNull = DataModelHierarchy.isDefaultValue(new PropertyPath(leftProperty).find(leftHierarchy));
    else
      leftIsDefaultOrNull = leftProperty.getValue() == null;

    if (rightIsDefaultOrNull && leftIsDefaultOrNull)
      return true;

    return false;
  }


  @Override
  public EDiff typeOfDiff(@NotNull EDirection pDirection)
  {
    //Eines der beiden Properties wurde gelöscht
    if ((leftProperty == null) & (rightProperty == null))
      return EDiff.DELETED;

    if ((pDirection == EDirection.LEFT) & (leftProperty == null))
      return EDiff.MISSING;

    if ((pDirection == EDirection.RIGHT) & (rightProperty == null))
      return EDiff.MISSING;

    if ((leftProperty != null) & (rightProperty != null))
    {
      if (Objects.equals(leftProperty.getValue(), rightProperty.getValue()))
        return EDiff.EQUAL;
      else
        return EDiff.DIFFERENT;
    }

    return EDiff.NOT_EVALUATED;
  }


  @Override
  public void createDown(@NotNull EDirection pDirection, IPropertyPitProvider pParent)
  {
    if ((pDirection == EDirection.RIGHT) & (leftProperty != null))
    {
      if (rightProperty != null)
      {
        handler.storeValue(pDirection, rightProperty.getValue());
        rightProperty.setValue(leftProperty.getValue());
      }
      else
      {
        pParent.getPit().setValue(leftProperty.getDescription(), leftProperty.getValue());
        rightProperty = pParent.getPit().findProperty(leftProperty.getDescription());
      }
    }

    if ((pDirection == EDirection.LEFT) & (rightProperty != null))
    {
      if (leftProperty != null)
      {
        handler.storeValue(pDirection, leftProperty.getValue());
        leftProperty.setValue(rightProperty.getValue());
      }
      else
      {
        pParent.getPit().setValue(rightProperty.getDescription(), rightProperty.getValue());
        leftProperty = pParent.getPit().findProperty(rightProperty.getDescription());
      }
    }

    //Wiederherstellen eines Properties nachdem es gelöscht wurde.
    if ((leftProperty == null) & (rightProperty == null))
    {
      if (pDirection == EDirection.LEFT)
        leftProperty = pParent.getPit().findProperty(name);

      if (pDirection == EDirection.RIGHT)
        rightProperty = pParent.getPit().findProperty(name);
    }

  }

  @Override
  public void deleteDown(@NotNull EDirection pDirection)
  {
    if (pDirection == EDirection.LEFT)
    {
      leftProperty = null;
    }

    if (pDirection == EDirection.RIGHT)
    {
      rightProperty = null;
    }
  }

  @Override
  protected void create(@NotNull EDirection pDirection,  AtomicReference pRef)
  {
     //tut nichts
  }


  @Override
  public String nameForIdentification()
  {
    return name;
  }


  @Override
  public String nameForDisplay(@NotNull EDirection pDirection)
  {

    if (pDirection == EDirection.LEFT)
    {
      if (leftProperty != null && leftProperty.isValid())
        return leftProperty.getName() + " = " + leftProperty.getValue();

      return name;
    }

    if (pDirection == EDirection.RIGHT)
    {
      if (rightProperty != null && rightProperty.isValid())
        return rightProperty.getName() + " = " + rightProperty.getValue();

      return name;
    }

    return name;
  }

  public String nameForDebugPrint()
  {
    String leftPart = EDirection.LEFT.name() + " ____ ";
    if (leftProperty != null)
      leftPart = EDirection.LEFT.name() + " " + leftProperty.getName() + " " + leftProperty.getValue();

    String rightPart = EDirection.RIGHT.name() + " ____ ";
    if (rightProperty != null)
      rightPart = EDirection.RIGHT.name() + " " + rightProperty.getName() + " " + rightProperty.getValue();

    return leftPart + "   " + rightPart;
  }

  @Override
  public String toString()
  {
    return nameForDisplay(EDirection.LEFT);//getName() + " Pair ";
  }

  @Override
  public void update(@NotNull EDirection pDirection)
  {
    //solange der alte Wert nicht restored wurde, kann kein neuer gesetzt werden.
    if (restoreHandler != null)
      return;

    if (pDirection == EDirection.LEFT)
    {
      if (rightProperty == null)
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setPropertly(leftProperty);
        getHost().parent().getPair().update(pDirection);
        leftProperty = null;
      }
      else
      {
        if (leftProperty != null)//Wert übernehmen links
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setValue(leftProperty.getValue());
          leftProperty.setValue(rightProperty.getValue());
        }
        else
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setPropertly(null);//leftProperty ist null
          AtomicReference<Object> ref = new AtomicReference<>();
          getHost().parent().getPair().create(pDirection, ref);
          Object o = ref.get();
//Wert vom Parent holen
          leftProperty = ((IPropertyPitProvider) o).getPit().getProperty(rightProperty.getDescription());
          getHost().parent().postUpdateDown(pDirection);
        }
      }
    }

    if (pDirection == EDirection.RIGHT)
    {
      if (leftProperty == null)//delete rechts
      {
        restoreHandler = new RestoreHandler(pDirection);
        restoreHandler.setPropertly(rightProperty);
        getHost().parent().getPair().update(pDirection);
        rightProperty = null;
      }
      else
      {
        if (rightProperty != null)//Wert übernehmen rechts
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setValue(rightProperty.getValue());
          rightProperty.setValue(leftProperty.getValue());
        }
        else
        {
          restoreHandler = new RestoreHandler(pDirection);
          restoreHandler.setPropertly(null);//rightProperty ist null
          AtomicReference<Object> ref = new AtomicReference<>();
          getHost().parent().getPair().create(pDirection, ref);
          Object o = ref.get();
//Wert vom Parent holen
          rightProperty = ((IPropertyPitProvider) o).getPit().getProperty(leftProperty.getDescription());
          getHost().parent().postUpdateDown(pDirection);

        }

      }
    }
  }

  @Override
  public void restore()
  {
    if (restoreHandler != null)
    {
      EDirection direction = restoreHandler.getDirection();
      Object propertly =  restoreHandler.getPropertly();
      Object value = restoreHandler.getValue();

      if (direction == EDirection.LEFT)//Links wiederherstellen
      {
        if (leftProperty != null & RestoreHandler.isSet(value))
          leftProperty.setValue(value);
        else if (leftProperty != null & RestoreHandler.isSet(propertly))
        {
          restoreHandler = null;
          leftProperty = null;
          getHost().parent().getPair().restore();

        }
        else if (leftProperty == null & RestoreHandler.isSet(propertly))//Property wieder aktivieren
        {
          IPropertyPitProvider prov = (IPropertyPitProvider) getHost().parent().getPair().getManagedObject(direction);
          if (prov != null)
          {
            leftProperty = prov.getPit().findProperty(name);
            restoreHandler = null;
          }
          else
          {
            getHost().parent().getPair().restore();
            restoreHandler = null;
          }

        }
      }
      restoreHandler = null;

      if (direction == EDirection.RIGHT)//Rechts wiederherstellen
      {
        if (rightProperty != null & RestoreHandler.isSet(value))
          rightProperty.setValue(value);
        else if (rightProperty != null & RestoreHandler.isSet(propertly))
        {
          restoreHandler = null;
          rightProperty = null;
          getHost().parent().getPair().restore();

        }
        else if (rightProperty == null & RestoreHandler.isSet(propertly))//Property wieder aktivieren
        {
          IPropertyPitProvider prov = (IPropertyPitProvider) getHost().parent().getPair().getManagedObject(direction);
          if (prov != null)
          {
            rightProperty = prov.getPit().findProperty(name);
            restoreHandler = null;
          }
          else
          {
            getHost().parent().getPair().restore();
            restoreHandler = null;
          }
          
        }
      }
      restoreHandler = null;
    }
    
  }

}
