package de.adito.aditoweb.nbm.aliasdiff.impl;

import com.google.common.base.Strings;
import de.adito.aditoweb.database.IAliasConfigInfo;
import de.adito.aditoweb.designer.dataobjects.data.db.*;
import de.adito.aditoweb.filesystem.datamodelfs.misc.IContextualAliasConfigResolver;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.*;
import de.adito.aditoweb.nbm.aliasdiff.impl.db.IAliasConfigResolverProvider;
import de.adito.aditoweb.nbm.aliasdiff.impl.entity.IEntityDBFactory;
import de.adito.aditoweb.nbm.aliasdiff.impl.gui.*;
import de.adito.aditoweb.nbm.aliasdiff.impl.update.StructureToDBPerformer;
import de.adito.aditoweb.nbm.designer.commonclasses.util.SaveUtil;
import de.adito.aditoweb.nbm.designer.commoninterface.dataobjects.IDesignerDataObject;
import de.adito.aditoweb.nbm.entitydbeditor.dataobjects.EntityGroupDBDataObject;
import de.adito.aditoweb.system.crmcomponents.datamodels.aliasdefsubs.AliasDefDBDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasDefinitionDataModel;
import de.adito.notification.INotificationFacade;
import de.adito.propertly.core.spi.*;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.progress.*;
import org.netbeans.api.project.Project;
import org.openide.util.*;
import org.openide.util.lookup.ServiceProvider;

import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.Supplier;

/**
 * GUI implementation of {@link IAliasDiffFacade}
 *
 * @author w.glanzer, 12.07.2022
 */
@ServiceProvider(service = IAliasDiffFacade.class)
public class AliasDiffFacadeImpl implements IAliasDiffFacade
{
  private final IEntityDBFactory entityDBFactory = Lookup.getDefault().lookup(IEntityDBFactory.class);
  private final IAliasConfigResolverProvider aliasConfigResolverProvider = Lookup.getDefault().lookup(IAliasConfigResolverProvider.class);

  @Override
  public void executeDatabaseDiffWithGUI(@NonNull IEntityGroupDBDataObject pEntityGroup)
  {
    EntityGroupDBDataModel group = pEntityGroup.getProperty().getValue();
    if (group == null)
      return;

    Project project = getProject(Set.of(pEntityGroup));
    RemoteAliasSupplier remoteAliasSupplier = RemoteAliasSupplier.create(project, aliasConfigResolverProvider, group);
    if (remoteAliasSupplier == null)
      return;

    startDiff(project, remoteAliasSupplier, DiffNodeCreatorFactory.forWholeAliasDBDiff(entityDBFactory, remoteAliasSupplier, group), new DBUpdateHandler());
  }

  @Override
  public void executeDatabaseDiffWithGUI(@NonNull IEntityGroupDBDataObject pEntityGroup, @NonNull Set<IEntityDBDataObject<?>> pTableDataObjects)
  {
    if (pTableDataObjects.isEmpty())
      return;

    Project project = getProject(pTableDataObjects);
    RemoteAliasSupplier remoteAliasSupplier = RemoteAliasSupplier.create(project, aliasConfigResolverProvider, pEntityGroup.getProperty().getValue());
    if (remoteAliasSupplier == null)
      return;

    startDiff(project, remoteAliasSupplier, DiffNodeCreatorFactory.forSomeTableDBDiff(entityDBFactory, remoteAliasSupplier, pTableDataObjects), new DBUpdateHandler());
  }

  /**
   * Starts the diff process and then displays a dialog.
   * This dialog allows the user to apply changes to the DB / local
   * and then executes them when clicking OK.
   *
   * @param pProject             Project that was the origin of the diff event
   * @param pRemoteAliasSupplier Supplier to retrieve the target remote alias
   * @param pOperation           Operation that returns a diff node to present in the dialog
   * @param pUpdateHandler       Update Handler that determines the actions, that are possible
   */
  private void startDiff(@NonNull Project pProject, @NonNull Supplier<IAliasConfigInfo> pRemoteAliasSupplier,
                         @NonNull ProgressRunnable<IDiffNode> pOperation, @NonNull IUpdateHandler pUpdateHandler)
  {
    IDiffNode rootNode = BaseProgressUtils.showProgressDialogAndRun(pOperation,
                                                                    NbBundle.getMessage(AliasDiffFacadeImpl.class, "PROGRESS_ExecDBDiffWithTables"),
                                                                    false);

    // cancelled?
    if (rootNode == null)
      return;

    // Show the dialog
    DiffPresenter.show(pProject, rootNode, pUpdateHandler, e -> {
      EDirection remoteSide = rootNode.isRemote(EDirection.RIGHT) ? EDirection.RIGHT : EDirection.LEFT;

      // save everything the user changed in the dialog
      SaveUtil.saveUnsavedStates(null, true);

      // update model in database, if needed
      if (hasRemoteSideChanged(e, remoteSide))
        executeUpdatesInDB(pRemoteAliasSupplier, rootNode, remoteSide);
    }, new DiffDBToolTip());
  }

  /**
   * Extracts the project from the given data objects
   *
   * @param pDataObjects tables, entities, or any other dataobjects
   * @return the project
   * @throws IllegalArgumentException if the project is missing
   */
  @NonNull
  private Project getProject(@NonNull Set<? extends IDesignerDataObject<?, ?>> pDataObjects)
  {
    return pDataObjects.stream()
        .map(IDesignerDataObject::getProject)
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing project" + pDataObjects));
  }

  /**
   * Determines, if the user did some changes on the remote side
   *
   * @param pEvent  Event that was given by the event listener
   * @param pRemote Determines, on which side of the diff the remote model was
   * @return true, if the remote side has changed or it could not be calculated (fallback)
   */
  private boolean hasRemoteSideChanged(@Nullable ActionEvent pEvent, @NonNull EDirection pRemote)
  {
    return Optional.ofNullable(pEvent)
        .map(ActionEvent::getSource)
        .filter(DiffPanel.class::isInstance)
        .map(DiffPanel.class::cast)
        .map(pPanel -> pPanel.isChangedByUser(pRemote))
        .orElse(true);
  }

  /**
   * Executes the updates, that the user did in the given {@link IDiffNode}, in the appropriate database
   *
   * @param pRemoteAliasSupplier Supplier to retrieve the target remote alias
   * @param pNode                Node to check for updates
   * @param pRemote              Determines, on which side of the diff the remote model was
   */
  private void executeUpdatesInDB(@NonNull Supplier<IAliasConfigInfo> pRemoteAliasSupplier, @NonNull IDiffNode pNode, @NonNull EDirection pRemote)
  {
    try
    {
      //noinspection unchecked
      StructureToDBPerformer.perform(pRemoteAliasSupplier, Optional.ofNullable(pNode.getHierarchy(pRemote))
          .map(IHierarchy::getValue)
          .filter(AliasDefinitionDataModel.class::isInstance)
          .map(pModel -> ((AliasDefinitionDataModel) pModel).getAliasDefinitionSub())
          .filter(AliasDefDBDataModel.class::isInstance)
          .map(pModel -> ((AliasDefDBDataModel) pModel).getEntityGroup())
          .map(pModel -> (EntityGroupDBDataObject) DataObjectUtil.get(pModel))
          .orElseThrow(() -> new IllegalStateException("Failed to find valid alias definition for node " + pNode.getRootName(pRemote))));
    }
    catch (Exception e)
    {
      INotificationFacade.INSTANCE.error(e);
    }
  }

  /**
   * Supplier to retrieve the {@link IAliasConfigInfo} for the appropriate remote alias to the given local alias
   */
  @RequiredArgsConstructor
  private static class RemoteAliasSupplier implements Supplier<IAliasConfigInfo>
  {
    @NonNull
    private final IContextualAliasConfigResolver resolver;

    @NonNull
    private final EntityGroupDBDataModel localAlias;

    @Override
    public IAliasConfigInfo get()
    {
      String definitionName = getDefinitionName(localAlias);

      try
      {
        return resolver.getConfigForDefinitionName(definitionName);
      }
      catch (Exception e)
      {
        throw new IllegalStateException("Failed to read config for definition name " + definitionName);
      }
    }

    /**
     * Creates a new instance of {@link RemoteAliasSupplier}
     *
     * @param pProject                     Project to get the alias config resolver for
     * @param pAliasConfigResolverProvider Provider to retrieve some {@link IContextualAliasConfigResolver}
     * @param pLocalAlias                  Alias to get the matching remote one for
     * @return the supplier to get the remote alias
     */
    @Nullable
    public static RemoteAliasSupplier create(@Nullable Project pProject, @Nullable IAliasConfigResolverProvider pAliasConfigResolverProvider,
                                             @Nullable EntityGroupDBDataModel pLocalAlias)
    {
      if (pProject == null || pAliasConfigResolverProvider == null || pLocalAlias == null)
        return null;

      IContextualAliasConfigResolver resolver = pAliasConfigResolverProvider.getResolver(pProject);
      if (resolver == null)
        return null;

      return new RemoteAliasSupplier(resolver, pLocalAlias);
    }

    /**
     * Returns the name, that should be used during database compare.
     * Because of modularization, we need to look for the "targetAliasName" property.
     *
     * @param pModel Model, that should be read
     * @return the name of the alias or null, if it can't be read
     */
    @Nullable
    private String getDefinitionName(@NonNull EntityGroupDBDataModel pModel)
    {
      // Search for the "targetAliasName" property
      //noinspection unchecked
      return Optional.of(pModel)
          .map(pGroupModel -> pGroupModel.getPit().getParent())
          .filter(AliasDefDBDataModel.class::isInstance)
          .map(pDefDBModel -> ((AliasDefDBDataModel) pDefDBModel).getPit().getValue(AliasDefDBDataModel.targetAliasName))
          .map(Strings::emptyToNull)

          // Read with the common "old" way
          .or(() -> Optional.of(DataObjectUtil.get(pModel))
              .map(IDesignerDataObject::getParent)
              .map(IDesignerDataObject::getName))

          // Parent or property is not available -> we do not know, how to handel this anymore..
          .orElse(null);
    }
  }

  /**
   * Only a table or column may be created in the database (remote).
   * Deletion or value change, e.g.: length of VARCHAR is not allowed.
   */
  private static class DBUpdateHandler implements IUpdateHandler
  {

    @Override
    public boolean canUpdate(@NonNull EDirection pDirection, IDiffNode pNode, boolean isRemote)
    {
      if (pDirection == EDirection.LEFT) // Update left side
      {
        Object sourceObject = pNode.getPair().getManagedObject(EDirection.RIGHT);
        Object targetObject = pNode.getPair().getManagedObject(EDirection.LEFT);
        if (isRemote)
          return checkRemoteUpdate(sourceObject, targetObject);
        else
          return true;
      }
      else if (pDirection == EDirection.RIGHT) // Update right (DB) side
      {
        Object sourceObject = pNode.getPair().getManagedObject(EDirection.LEFT);
        Object targetObject = pNode.getPair().getManagedObject(EDirection.RIGHT);
        if (isRemote)
          return checkRemoteUpdate(sourceObject, targetObject);
        else
          return true;
      }

      return false;
    }

    /**
     * Checks, if pTarget may be updated with the values of pSource
     *
     * @param pSource Source object to get the desired values
     * @param pTarget Target object, where pSource should input its values in
     * @return true, if the update is possible
     */
    @SuppressWarnings("ConstantConditions")
    private boolean checkRemoteUpdate(@Nullable Object pSource, @Nullable Object pTarget)
    {
      // Both sides are present -> table change -> remote not possible
      if (pSource != null && pTarget != null)
        return false;

      // Source is null -> target should be deleted
      if (pSource == null)
        return false;

      if (pSource instanceof EntityDBDataModel)
        // Table creation allowed, if it is not read only
        return !((EntityDBDataModel) pSource).isReadOnly();

      // Column creation allowed
      if (pSource instanceof EntityFieldDBDataModel)
        return true;

      // Creation / Write of value changes allowed
      return pSource instanceof IProperty;
    }
  }
}
