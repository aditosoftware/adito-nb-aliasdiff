package de.adito.aditoweb.nbm.aliasdiff.impl;

import de.adito.aditoweb.designer.dataobjects.data.db.*;
import de.adito.aditoweb.filesystem.datamodelfs.misc.IContextualAliasConfigResolver;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.DiffPresenter;
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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.progress.*;
import org.netbeans.api.project.Project;
import org.openide.util.*;
import org.openide.util.lookup.ServiceProvider;

import java.util.*;

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
  public void executeDatabaseDiffWithGUI(@NonNull Set<IEntityDBDataObject<?>> pTableDataObjects)
  {
    if (pTableDataObjects.isEmpty())
      return;

    Project project = getProject(pTableDataObjects);
    IContextualAliasConfigResolver resolver = aliasConfigResolverProvider.getResolver(project);
    if (resolver == null)
      return;

    startDiff(project, resolver, DiffNodeCreatorFactory.forSomeTableDBDiff(entityDBFactory, resolver, pTableDataObjects), new DBUpdateHandler());
  }

  @Override
  public void executeDatabaseDiffWithGUI(@NonNull IEntityGroupDBDataObject pEntityGroup)
  {
    EntityGroupDBDataModel group = pEntityGroup.getProperty().getValue();
    if (group == null)
      return;

    Project project = getProject(Set.of(pEntityGroup));
    IContextualAliasConfigResolver resolver = aliasConfigResolverProvider.getResolver(project);
    if (resolver == null)
      return;

    startDiff(project, resolver, DiffNodeCreatorFactory.forWholeAliasDBDiff(entityDBFactory, resolver, group), new DBUpdateHandler());
  }

  /**
   * Starts the diff process and then displays a dialog.
   * This dialog allows the user to apply changes to the DB / local
   * and then executes them when clicking OK.
   *
   * @param pProject             Project that was the origin of the diff event
   * @param pAliasConfigResolver Resolver that allows the resolution of alias definition names during update phase after the dialog
   * @param pOperation           Operation that returns a diff node to present in the dialog
   * @param pUpdateHandler       Update Handler that determines the actions, that are possible
   */
  private void startDiff(@NonNull Project pProject, @NonNull IContextualAliasConfigResolver pAliasConfigResolver,
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
      executeUpdatesInDB(pAliasConfigResolver, rootNode, remoteSide);
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
   * Executes the updates, that the user did in the given {@link IDiffNode}, in the appropriate database
   *
   * @param pAliasConfigResolver Resolver to get the alias config from
   * @param pNode                Node to check for updates
   * @param pRemote              Determines, on which side of the diff the remote model was
   */
  private void executeUpdatesInDB(@NonNull IContextualAliasConfigResolver pAliasConfigResolver, @NonNull IDiffNode pNode, @NonNull EDirection pRemote)
  {
    try
    {
      //noinspection unchecked
      StructureToDBPerformer.perform(pAliasConfigResolver, Optional.ofNullable(pNode.getHierarchy(pRemote))
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
