package de.adito.aditoweb.nbm.aliasdiff.impl.gui;

import de.adito.aditoweb.database.general.metainfo.ITableMetadata;
import de.adito.aditoweb.database.general.metainfo.providers.ITableMetadataProvider;
import de.adito.aditoweb.designer.dataobjects.data.db.IEntityDBDataObject;
import de.adito.aditoweb.filesystem.datamodelfs.misc.IContextualAliasConfigResolver;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.impl.entity.IEntityDBFactory;
import de.adito.aditoweb.nbm.designer.commoninterface.dataobjects.IDesignerDataObject;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.EntityGroupDBDataModel;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasConfigDataModel;
import de.adito.notification.INotificationFacade;
import de.adito.propertly.core.spi.IProperty;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.progress.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Is responsible for creating ProgressRunnables, which create a DiffNode for the diff dialog.
 *
 * @author w.glanzer, 31.01.2023
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DiffNodeCreatorFactory
{

  /**
   * Creates a {@link ProgressRunnable} for a diff of a whole alias with a database
   *
   * @param pFactory  Factory to create entity models from database
   * @param pResolver Resolver to load alias configs
   * @param pAlias    Local alias that should be compared
   * @return a {@link ProgressRunnable} containing the {@link IDiffNode}
   */
  @NonNull
  public static ProgressRunnable<IDiffNode> forWholeAliasDBDiff(@NonNull IEntityDBFactory pFactory, @NonNull IContextualAliasConfigResolver pResolver,
                                                                @NonNull EntityGroupDBDataModel pAlias)
  {
    return new DBDiffNodeRunnableForWholeAlias(pFactory, pResolver, pAlias);
  }

  /**
   * Creates a {@link ProgressRunnable} for a diff of some tables with a database
   *
   * @param pFactory  Factory to create entity models from database
   * @param pResolver Resolver to load alias configs
   * @param pTables   Set of tables that should be compared
   * @return a {@link ProgressRunnable} containing the {@link IDiffNode}
   */
  @NonNull
  public static ProgressRunnable<IDiffNode> forSomeTableDBDiff(@NonNull IEntityDBFactory pFactory, @NonNull IContextualAliasConfigResolver pResolver,
                                                               @NonNull Set<IEntityDBDataObject<?>> pTables)
  {
    return new DBDiffNodeRunnableForSomeTables(pFactory, pResolver, pTables);
  }

  /**
   * Abstract runnable to diff something
   */
  private abstract static class AbstractDiffNodeRunnable implements ProgressRunnable<IDiffNode>
  {
    @Override
    public final IDiffNode run(ProgressHandle pHandle)
    {
      try
      {
        return run0(pHandle);
      }
      catch (Exception e)
      {
        // Notify the user via balloon, because he clicked on
        // something and wants some type of response
        INotificationFacade.INSTANCE.error(e);
        return null;
      }
    }

    /**
     * Gets called if the {@link IDiffNode} should be calculcated
     *
     * @param pHandle Handle to show progress
     * @return the created node
     * @throws Exception if something failed during creation
     */
    @Nullable
    protected abstract IDiffNode run0(@NonNull ProgressHandle pHandle) throws Exception; //NOSONAR a new exception will be a bit too heavy here..

    /**
     * Creates the necessary diff node for the diff dialog to differentiate a local alias with a remote alias
     *
     * @param pLocal          Representation of the local alias in project
     * @param pRemote         Representation of the remote alias (maybe in database)
     * @param pDiffFilter     Filter to exclude some properties from diff
     * @param pRemoteReadOnly true if the remote side should be read only
     * @return the node
     */
    @NonNull
    @SuppressWarnings("SameParameterValue") // ignore this warning, because we may add another runnables for diffing the system tables too
    protected IDiffNode createDiffNode(@NonNull EntityGroupDBDataModel pLocal, @NonNull EntityGroupDBDataModel pRemote,
                                       @Nullable IPropertyFilter pDiffFilter, boolean pRemoteReadOnly)
    {
      return (IDiffNode) new EntityTreeNodeFilter()
          .filterNode(new PropertyPitMatcher<>(pLocal, pDiffFilter, EDirection.RIGHT, pRemote,
                                               pRemoteReadOnly ? EDirection.RIGHT : null).match());
    }

    /**
     * Returns the common root of the given tables
     *
     * @param pTables Tables
     * @return the root model
     */
    @NonNull
    protected EntityGroupDBDataModel getRoot(@NonNull Set<IEntityDBDataObject<?>> pTables)
    {
      return pTables.stream()
          .map(IDesignerDataObject::getParent)
          .filter(Objects::nonNull)
          .map(IDesignerDataObject::getParent)
          .filter(Objects::nonNull)
          .map(IDesignerDataObject::getProperty)
          .map(IProperty::getValue)
          .filter(Objects::nonNull)
          .filter(EntityGroupDBDataModel.class::isInstance)
          .map(EntityGroupDBDataModel.class::cast)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Missing entitygroup parent " + pTables));
    }

    /**
     * Extracts the table names from the given tables
     *
     * @param pTables Tables to extract the names from
     * @return the table names
     */
    @NonNull
    protected Set<String> getTableNames(@NonNull Set<IEntityDBDataObject<?>> pTables)
    {
      return pTables.stream()
          .filter(ITableMetadataProvider.class::isInstance)
          .map(ITableMetadataProvider.class::cast)
          .map(ITableMetadataProvider::getTableMetadata)
          .filter(Objects::nonNull)
          .map(ITableMetadata::getName)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    }
  }

  /**
   * Abstract runnable to diff something with a database
   */
  @AllArgsConstructor(access = AccessLevel.PROTECTED)
  private abstract static class AbstractDBDiffNodeRunnable extends AbstractDiffNodeRunnable
  {
    @NonNull
    private final IEntityDBFactory entityFactory;

    @NonNull
    private final IContextualAliasConfigResolver aliasConfigResolver;

    /**
     * Loads the equivalent of the passed alias from the database.
     * Can be narrowed down to specific tables if necessary
     *
     * @param pLocalAlias Local alias definition
     * @param pTableNames Names of the tables to diff. NULL will diff every table in the given alias.
     * @return A newly created GroupDBModel with the contents of the tables
     * @throws Exception if an error occurred while loading the AliasConfig or creating the entity
     */
    @NonNull
    protected EntityGroupDBDataModel resolveEntityGroupInDB(@NonNull EntityGroupDBDataModel pLocalAlias, @Nullable Set<String> pTableNames)
        throws Exception //NOSONAR generic exception handling is okay here..
    {
      // Read out which database we should read from and get the config
      AliasConfigDataModel config = aliasConfigResolver.getConfigForDefinitionName(pLocalAlias.getPit().getHierarchy().getProperty().getName());

      // Read the entity from the DB and possibly filter it by table name
      if (pTableNames == null)
        return entityFactory.create(config);
      else
        return entityFactory.create(config, pTableNames);
    }
  }

  /**
   * Runnable that diffs a whole alias with a database
   */
  private static class DBDiffNodeRunnableForWholeAlias extends AbstractDBDiffNodeRunnable
  {
    private final EntityGroupDBDataModel groupModel;

    /**
     * Creates the runnable based on a local alias definition
     *
     * @param pFactory    Factory to create entity models from database
     * @param pResolver   Resolver to load alias configs
     * @param pGroupModel Local alias to diff
     */
    public DBDiffNodeRunnableForWholeAlias(@NonNull IEntityDBFactory pFactory, @NonNull IContextualAliasConfigResolver pResolver,
                                           @NonNull EntityGroupDBDataModel pGroupModel)
    {
      super(pFactory, pResolver);
      groupModel = pGroupModel;
    }

    @Override
    protected IDiffNode run0(@NonNull ProgressHandle pHandle) throws Exception
    {
      // Read model from database
      EntityGroupDBDataModel extractedModel = resolveEntityGroupInDB(groupModel, null);

      // create the diff node from the extracted model
      return createDiffNode(groupModel, extractedModel, null, false);
    }
  }

  /**
   * Runnable that diffs one or more tables with a database
   */
  private static class DBDiffNodeRunnableForSomeTables extends AbstractDBDiffNodeRunnable
  {
    @NonNull
    private final Set<IEntityDBDataObject<?>> tables;

    /**
     * Creates the runnable based on some db tables
     *
     * @param pFactory  Factory to create entity models from database
     * @param pResolver Resolver to load alias configs
     * @param pTables   tables to diff
     */
    public DBDiffNodeRunnableForSomeTables(@NonNull IEntityDBFactory pFactory, @NonNull IContextualAliasConfigResolver pResolver,
                                           @NonNull Set<IEntityDBDataObject<?>> pTables)
    {
      super(pFactory, pResolver);
      tables = pTables;
    }

    @Override
    protected IDiffNode run0(@NonNull ProgressHandle pHandle) throws Exception
    {
      EntityGroupDBDataModel groupModel = getRoot(tables);
      Set<String> tableNames = getTableNames(tables);

      // Read model from database
      EntityGroupDBDataModel extractedModel = resolveEntityGroupInDB(groupModel, tableNames);

      // create the diff node from the extracted model
      return createDiffNode(groupModel, extractedModel, new TableNameFilter(tableNames), false);
    }
  }

}
