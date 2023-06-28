package de.adito.aditoweb.nbm.aliasdiff.actions;

import de.adito.aditoweb.database.general.metainfo.ITableMetadata;
import de.adito.aditoweb.database.general.metainfo.providers.ITableMetadataProvider;
import de.adito.aditoweb.designer.dataobjects.data.db.IEntityDBDataObject;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.designer.commoninterface.dataobjects.IDesignerDataObject;
import de.adito.aditoweb.nbm.entitydbeditor.actions.AliasDiffUtil;
import de.adito.aditoweb.nbm.entitydbeditor.dataobjects.EntityGroupDBDataObject;
import de.adito.aditoweb.system.crmcomponents.datamodels.aliasdefsubs.AliasDefDBDataModel;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasDefinitionDataModel;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.openide.nodes.Node;

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

  /**
   * Gets called if the action should be executed
   *
   * @param pActivatedNodes the currently active nodes
   */
  public void performAction(@Nullable Node[] pActivatedNodes)
  {
    if (pActivatedNodes == null || pActivatedNodes.length == 0)
      return;

    //noinspection unchecked Extract the selected dataobject from the lookup
    Arrays.stream(pActivatedNodes)
        .map(pNode -> pNode.getLookup().lookup(IDesignerDataObject.class))

        // Retrieve its root model, so we can obtain the entitygroup from
        .map(pDataObject -> pDataObject.getHierarchy().getValue())

        // The root model should be an aliasdefinition model
        .filter(AliasDefinitionDataModel.class::isInstance)
        .map(AliasDefinitionDataModel.class::cast)

        // Extract the sub model, so we can ensure, that this aliasdefinition belongs to a DB alias
        .map(AliasDefinitionDataModel::getAliasDefinitionSub)
        .filter(AliasDefDBDataModel.class::isInstance)
        .map(AliasDefDBDataModel.class::cast)

        // Retrieve the entitygroup dataobject instance from the db sub model
        .map(AliasDefDBDataModel::getEntityGroup)
        .map(DataObjectUtil::get)
        .filter(EntityGroupDBDataObject.class::isInstance)
        .map(EntityGroupDBDataObject.class::cast)

        // Start the alias diff workflow on the first valid model we find
        .findFirst()
        .ifPresent(pDataObject -> performAliasDiff(pDataObject, containsOnlyTables(pActivatedNodes) ? collectTableNames(pActivatedNodes) : null));
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
   * Collects table names from the array.
   *
   * @param pActivatedNodes is searched for table objects.
   * @return always a set, possibly empty.
   */
  @NonNull
  private Set<String> collectTableNames(@Nullable Node[] pActivatedNodes)
  {
    if (pActivatedNodes == null || pActivatedNodes.length == 0)
      return Set.of();

    return Arrays.stream(pActivatedNodes)
        .map(pNode -> pNode.getLookup().lookup(ITableMetadataProvider.class))
        .filter(Objects::nonNull)
        .map(ITableMetadataProvider::getTableMetadata)
        .filter(Objects::nonNull)
        .map(ITableMetadata::getName)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Starts the Diff workflow for the given alias and the optionally given table names
   *
   * @param pAlias      Alias to diff
   * @param pTableNames Selected table names
   */
  private void performAliasDiff(@NonNull EntityGroupDBDataObject pAlias, @Nullable Set<String> pTableNames)
  {
    AliasDiffUtil.performDatabaseDiff(pAlias, pTableNames, pAlias.getProject());
  }

}
