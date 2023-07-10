package de.adito.aditoweb.nbm.aliasdiff.actions;

import de.adito.aditoweb.designer.dataobjects.data.db.*;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.aliasdiff.impl.IAliasDiffFacade;
import de.adito.aditoweb.nbm.designer.commoninterface.dataobjects.IDesignerDataObject;
import de.adito.aditoweb.system.crmcomponents.datamodels.aliasdefsubs.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.EntityGroupDBDataModel;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasDefinitionDataModel;
import de.adito.propertly.core.spi.IPropertyPitProvider;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains all sourcecode that the execution of the "DiffWithDBTables" actions need.
 * We are unable to combine them in an abstract class, so we extracted the sources into this one.
 *
 * @author w.glanzer, 28.06.2023
 * @see DiffWithDBActionExt
 * @see DiffWithDBTablesActionExt
 */
class DiffWithDBActionExecutor
{

  private final IAliasDiffFacade aliasDiffFacade = Lookup.getDefault().lookup(IAliasDiffFacade.class);

  /**
   * Gets called if the action should be executed
   *
   * @param pActivatedNodes the currently active nodes
   */
  public void performAction(@Nullable Node[] pActivatedNodes)
  {
    if (pActivatedNodes == null)
      return;

    IEntityGroupDBDataObject groupDataObject = findGroup(pActivatedNodes);
    if (groupDataObject != null)
    {
      // If only tables are selected, then diff the tables - otherwise diff the whole database
      if (containsOnlyTables(pActivatedNodes))
        aliasDiffFacade.executeDatabaseDiffWithGUI(groupDataObject, collectTables(pActivatedNodes));
      else
        aliasDiffFacade.executeDatabaseDiffWithGUI(groupDataObject);
    }
  }

  /**
   * Extracts the {@link IEntityGroupDBDataObject} from the given nodes, if possible
   *
   * @param pActivatedNodes Nodes that are currently selected
   * @return the group or null, if none available
   */
  @Nullable
  private IEntityGroupDBDataObject findGroup(@NonNull Node[] pActivatedNodes)
  {
    if (pActivatedNodes.length > 0)
    {
      IDesignerDataObject<?, ?> ddo = pActivatedNodes[0].getLookup().lookup(IDesignerDataObject.class);
      if (ddo != null)
      {
        EntityGroupDBDataModel groupModel = findGroupModel(ddo);
        if (groupModel != null)
          //noinspection unchecked
          return (IEntityGroupDBDataObject) DataObjectUtil.find(groupModel);
      }
    }

    return null;
  }

  /**
   * Extracts the {@link EntityGroupDBDataModel} from the given dataobject, if possible
   *
   * @param pDataObject DataObject to extract the group from
   * @return the group model or null, if extraction is not possible
   */
  @Nullable
  private EntityGroupDBDataModel findGroupModel(@NonNull IDesignerDataObject<?, ?> pDataObject)
  {
    IPropertyPitProvider<?, ?, ?> value = pDataObject.getHierarchy().getValue();
    if (value instanceof AliasDefinitionDataModel)
    {
      AbstractAliasDefSubDataModel<?> subModel = ((AliasDefinitionDataModel) value).getAliasDefinitionSub();
      if (subModel instanceof AliasDefDBDataModel)
        return ((AliasDefDBDataModel) subModel).getEntityGroup();
    }

    return null;
  }

  /**
   * Returns true if the array contains only nodes
   * whose data models represent table objects.
   *
   * @return true if there are only tables in the array.
   */
  private boolean containsOnlyTables(@Nullable Node[] pActivatedNodes)
  {
    if (pActivatedNodes == null || pActivatedNodes.length == 0)
      return false;

    for (Node n : pActivatedNodes)
    {
      IDesignerDataObject<?, ?> ddo = n.getLookup().lookup(IDesignerDataObject.class);
      if (!(ddo instanceof IEntityDBDataObject<?>)) // if it is not a (valid) table, then instantly return
        return false;
    }

    return true;
  }

  /**
   * Extracts all currently selected tables
   *
   * @param pActivatedNodes Nodes that are currently selected
   * @return all tables, empty if nothing is selected
   */
  @NonNull
  private Set<IEntityDBDataObject<?>> collectTables(@Nullable Node[] pActivatedNodes)
  {
    if (pActivatedNodes == null || pActivatedNodes.length == 0)
      return Set.of();

    return Arrays.stream(pActivatedNodes)
        .map(pNode -> (IEntityDBDataObject<?>) pNode.getLookup().lookup(IEntityDBDataObject.class))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

}
