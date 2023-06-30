package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.common.IButtonInscriptions;
import de.adito.aditoweb.core.util.lang.Pair;
import de.adito.aditoweb.designer.dataobjects.data.miscobjects.AliasDefinitionDataObject;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.EDiff;
import de.adito.aditoweb.nbm.designer.commoninterface.lookup.editorcontext.IEditorContextProvider;
import de.adito.aditoweb.nbm.designer.commoninterface.refactor.IReferenceManager;
import de.adito.aditoweb.nbm.designer.commoninterface.services.editorcontext.*;
import de.adito.aditoweb.system.crmcomponents.IDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.aliasdefsubs.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.IEntityFieldDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasDefinitionDataModel;
import de.adito.notification.INotificationFacade;
import lombok.*;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.project.Project;
import org.openide.*;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Displays either a dialog with the diffpanel, or a balloon information.
 *
 * @author t.tasior, 09.02.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DiffPresenter
{
  /**
   * Receives the tree structure and shows either a balloon information if there are no differences in the tree,
   * or a dialog with the diffpanel. Accepts a listener that is called up in the dialog when you click on "OK".
   *
   * @param pProject            Project for the current working context
   * @param pRoot               Root Node that may contain differences
   * @param pUpdateHandler      Determines which updates the tree will allow
   * @param pListener           Listener that gets triggered, if OK was clicked
   * @param pUserdefinedToolTip a tooltip to display
   */
  public static void show(@NonNull Project pProject, @NonNull IDiffNode pRoot, @Nullable IUpdateHandler pUpdateHandler,
                          @Nullable ActionListener pListener, @Nullable JToolTip pUserdefinedToolTip)
  {
    String leftTitle = DiffPanel.getTitle(pRoot, EDirection.LEFT);
    String rightTitle = DiffPanel.getTitle(pRoot, EDirection.RIGHT);
    String title = NbBundle.getMessage(DiffPresenter.class, "LBL_Diff", leftTitle, rightTitle);
    if (pRoot.getChildCount() == 0)
    {
      INotificationFacade.INSTANCE.notify(title, NbBundle.getMessage(DiffPresenter.class, "TXT_Equal_Content"), true, null);
      return;
    }

    final String ok = IButtonInscriptions.OK;
    final String cancel = IButtonInscriptions.CANCEL;
    Object[] buttons = {ok, cancel};

    final DialogDescriptor descriptor = new DialogDescriptor(new DiffPanel(pRoot, pUpdateHandler, pUserdefinedToolTip),
                                                             title, true, buttons, ok,
                                                             DialogDescriptor.BOTTOM_ALIGN, null, null);
    descriptor.setClosingOptions(new Object[]{});

    final Dialog dlg = DialogDisplayer.getDefault().createDialog(descriptor);
    descriptor.setButtonListener(e -> {
      if (e.getSource() == ok)
      {
        AliasDefinitionDataModel model = findRootModel(pProject, pRoot);
        if (model != null)
        {
          Pair<List<String>, List<Pair<String, List<String>>>> deleteCandidates = extractDeleteCandidates(pRoot);
          List<IDataModel<?, ?>> toDelete = new ArrayList<>();
          // find all deleted tables
          deleteCandidates.a.stream()
              .map(pTableName -> findTable(model, pTableName))
              .filter(Objects::nonNull)
              .forEach(toDelete::add);

          // find all deleted columns
          deleteCandidates.b.stream()
              .map(pPair -> new Pair<>(findTable(model, pPair.a), pPair.b))
              .filter(pPair -> pPair.a != null)
              .map(pPair -> pPair.b
                  .stream()
                  .map(pString -> findColumn(pPair.a, pString))
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList()))
              .forEach(toDelete::addAll);

          IReferenceManager manager = IReferenceManager.get();
          manager.delete(toDelete);
          SwingUtilities.invokeLater(() -> {
            pRoot.write();
            if (pListener != null)
              pListener.actionPerformed(null);
            dlg.dispose();
          });
        }
      }
      else if (e.getSource() == cancel)
      {
        dlg.dispose();
      }
    });

    dlg.setSize(1200, 800);
    dlg.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
    dlg.setVisible(true);
  }

  /**
   * Extracts all deleted tables and columns
   *
   * @param pRoot Root to search for
   * @return List with all deleted tables and a list of pairs containing the deleted column and the table name
   */
  @NonNull
  private static Pair<List<String>, List<Pair<String, List<String>>>> extractDeleteCandidates(@NonNull IDiffNode pRoot)
  {
    EDirection direction = getDirectionOfLocalReadableModel(pRoot);

    if (direction != null)
    {
      List<Pair<String, List<String>>> columns = new ArrayList<>();
      List<String> tables = new ArrayList<>();

      for (TreeNode table : Collections.list(pRoot.children()))
      {
        IDiffNode node = (IDiffNode) table;
        String tableName = ((DefaultMutableTreeNode) table).getUserObject().toString();
        if (node.getDiff(direction) == EDiff.DELETED)
          tables.add(tableName);

        List<String> deletedColumns = new ArrayList<>();
        for (TreeNode column : Collections.list(node.children()))
        {
          IDiffNode child = (IDiffNode) column;
          if (child.getDiff(direction) == EDiff.DELETED)// column should be deleted
            deletedColumns.add(((DefaultMutableTreeNode) child).getUserObject().toString());
        }
        if (!deletedColumns.isEmpty())
          columns.add(new Pair<>(tableName, deletedColumns));

      }
      return new Pair<>(tables, columns);
    }
    return new Pair<>(List.of(), List.of());
  }

  /**
   * Searches a table with the given name in the given alias
   *
   * @param pAlias Alias to search in
   * @param pName  Name of the table that should be returned
   * @return the found table or null, if not found
   */
  @Nullable
  private static EntityDBDataModel findTable(@NonNull AliasDefinitionDataModel pAlias, @NonNull String pName)
  {
    //noinspection unchecked
    AliasDefinitionDataObject aliasDefinitionDataObject = (AliasDefinitionDataObject) DataObjectUtil.get(pAlias);
    AbstractAliasDefSubDataModel<?> subdm = aliasDefinitionDataObject.observeSubModel().blockingFirst().orElse(null);
    if (subdm instanceof AliasDefDBDataModel)
    {
      EntityGroupDBDataModel entityGroup = ((AliasDefDBDataModel) subdm).getValue(AliasDefDBDataModel.entityGroup);
      if (entityGroup != null)
      {
        List<EntityDBDataModel> result = entityGroup.getEntities().stream()
            .filter(pEntityDBDataModel -> pEntityDBDataModel.getName().equals(pName))
            .collect(Collectors.toList());
        if (result.size() == 1)
          return result.get(0);
      }
    }

    return null;
  }

  /**
   * Searches a column within the given table
   *
   * @param pTable      Table to search in
   * @param pNameColumn Name of the column to search
   * @return the found column or null, if not found
   */
  @Nullable
  private static IEntityFieldDataModel<?> findColumn(@NonNull EntityDBDataModel pTable, @Nullable String pNameColumn)
  {
    return pTable.getEntityField(pNameColumn);
  }

  /**
   * Searches the alias definition that the given diff node represents
   *
   * @param pProject Project to determine the definitions from
   * @param pRoot    Root to search
   * @return the found model or null, if nothing found
   */
  @Nullable
  private static AliasDefinitionDataModel findRootModel(@NonNull Project pProject, @NonNull IDiffNode pRoot)
  {
    EDirection direction = getDirectionOfLocalReadableModel(pRoot);
    if (direction != null)
    {
      IEditorContextProvider p = EditorContextProviderQuery.find(pProject);
      if (p != null)
      {
        IEditorContext<?> ec = p.find(pRoot.getRootName(direction));
        if (ec != null)
        {
          try
          {
            return (AliasDefinitionDataModel) ec.getModelRoot();
          }
          catch (WrongDataModelException e)
          {
            log.log(Level.WARNING, "", e);
          }
        }
      }
    }

    return null;
  }

  /**
   * Returns the position of the data model, which is local on disk (in the project) and can be edited.
   *
   * @param pRoot necessary to determine
   * @return the direction or null, if not calculatable
   */
  @Nullable
  private static EDirection getDirectionOfLocalReadableModel(@NonNull IDiffNode pRoot)
  {
    EDirection left = EDirection.LEFT;
    EDirection right = EDirection.RIGHT;

    if (!pRoot.isRemote(left) && !pRoot.isReadOnly(left))
      return left;
    else if (!pRoot.isRemote(right) && !pRoot.isReadOnly(right))
      return right;

    return null;
  }
}
