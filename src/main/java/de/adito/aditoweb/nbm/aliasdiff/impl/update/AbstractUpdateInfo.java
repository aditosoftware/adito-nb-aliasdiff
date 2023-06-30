package de.adito.aditoweb.nbm.aliasdiff.impl.update;

import de.adito.aditoweb.core.multilanguage.*;
import org.jetbrains.annotations.*;

import java.util.List;

/**
 * @author C.Stadler on 08.02.2017.
 */
abstract class AbstractUpdateInfo
{
  protected List<PropertyValue> properties;
  private String errorMessage; // Alles was nicht NULL ist ist ein Fehler
  private String objectName;
  private String parentObjectName;
  protected ConvenienceTranslator ct;

  private UpdateKind updateKind;

  AbstractUpdateInfo(@NotNull List<PropertyValue> pProperties, @NotNull String pObjectName, @Nullable String pParentObjectName, @NotNull UpdateKind pUpdateKind)
  {
    updateKind = pUpdateKind;
    properties = pProperties;
    errorMessage = null;
    objectName = pObjectName;
    parentObjectName = pParentObjectName;
    ct = new ConvenienceTranslator(20);
  }

  @Override
  @NotNull
  public String toString()
  {
    return getObjectName() + ": " + getDescription();
  }

  public boolean isSomethingToUpdate()
  {
    return getDelete() || getNew();
  }

  public String getObjectName()
  {
    return objectName;
  }

  public void setObjectName(String pObjectName)
  {
    objectName = pObjectName;
  }

  public String getParentObjectName()
  {
    return parentObjectName;
  }

  public void setParentObjectName(String pParentObjectName)
  {
    parentObjectName = pParentObjectName;
  }

  public String getErrorMessage()
  {
    return errorMessage;
  }

  public void setErrorMessage(String pErrorMessage)
  {
    errorMessage = pErrorMessage;
  }

  public boolean getDelete()
  {
    return updateKind.equals(UpdateKind.DELETE_OBJECT);
  }

  public void setDelete()
  {
    updateKind = UpdateKind.DELETE_OBJECT;
  }

  public boolean getNew()
  {
    return updateKind.equals(UpdateKind.NEW_OBJECT);
  }

  public void setNew()
  {
    updateKind = UpdateKind.NEW_OBJECT;
  }

  public boolean getUpdate()
  {
    return updateKind.equals(UpdateKind.NEW_OBJECT);
  }

  public abstract String getDescription();

  protected String getDescription(String pResStringNew, String pResStringDel, String pResStringUpdate, String pNewValue)
  {
    switch(updateKind)
    {
      case NEW_OBJECT:
        return ct.translate(pResStringNew);
      case DELETE_OBJECT:
        return ct.translate(pResStringDel);
      case UPDATE_OBJECT:
        return ct.translate(pResStringUpdate) + ": " + String.format(ct.translate(IStaticResources.DESC_CHANGE), pNewValue);
      default:
        return "";
    }
  }

  public abstract boolean isAllowed();
}
