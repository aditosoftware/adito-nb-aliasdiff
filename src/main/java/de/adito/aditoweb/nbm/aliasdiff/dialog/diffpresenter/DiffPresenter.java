package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.common.IButtonInscriptions;
import de.adito.aditoweb.core.util.lang.Pair;
import de.adito.aditoweb.designer.dataobjects.data.miscobjects.AliasDefinitionDataObject;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.aditonetbeansutil.notification.NotifyUtil;
import de.adito.aditoweb.nbm.designer.commoninterface.lookup.editorcontext.IEditorContextProvider;
import de.adito.aditoweb.nbm.designer.commoninterface.refactor.IReferenceManager;
import de.adito.aditoweb.nbm.designer.commoninterface.services.editorcontext.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.EDiff;
import de.adito.aditoweb.system.crmcomponents.IDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.aliasdefsubs.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.IEntityFieldDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.AliasDefinitionDataModel;
import org.jetbrains.annotations.*;
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
import java.util.stream.Collectors;

/**
 * Zeigt entweder einen Dialog mit dem Diffpanel an, oder eine Balloon Information.
 *
 * @author t.tasior, 09.02.2018
 */
public class DiffPresenter
{
  /**
   * Nimmt die Baumstruktur entgegen und zeigt entweder eine Balloon Information
   * wenn es keine Differenzen im Baum gibt, oder einen Dialog mit dem Diffpanel.
   * Nimmt einen Listener entgegen der beim Klick auf "OK" im Dialog aufgerufen wird.
   *
   * @param pRoot               Baumstruktur die ggf. Differenzen enthält.
   * @param pUpdateHandler      entscheidet welche Updates auf den Datenmodellen erlaubt sind.
   * @param pListener           hört auf den Button "OK" im Dialog.
   * @param pUserdefinedToolTip ein spezieller Tooltip für die Info Komponente rechts oben.
   */
  public static void show(IDiffNode pRoot, IUpdateHandler pUpdateHandler, ActionListener pListener,
                          JToolTip pUserdefinedToolTip, Project pProject)
  {
    String leftTitle = DiffPanel.getLeftTitle(pRoot);
    String rightTitle = DiffPanel.getRightTitle(pRoot);
    String title = NbBundle.getMessage(DiffPresenter.class, "LBL_Diff", leftTitle, rightTitle);
    if (pRoot.getChildCount() == 0)
    {
      NotifyUtil.balloon().info(title, NbBundle.getMessage(DiffPresenter.class, "TXT_Equal_Content"));
      return;
    }

    final String ok = IButtonInscriptions.OK;
    final String cancel = IButtonInscriptions.CANCEL;
    Object[] buttons = {ok, cancel};

    final DialogDescriptor descriptor = new DialogDescriptor(new DiffPanel(pRoot, pUpdateHandler, pUserdefinedToolTip),
                                                             title, true, buttons, ok,
                                                             DialogDescriptor.BOTTOM_ALIGN, null, null);
    descriptor.setClosingOptions(new Object[]{});//Den Dialog schliessen wir.

    final Dialog dlg = DialogDisplayer.getDefault().createDialog(descriptor);
    descriptor.setButtonListener(e -> {
      if (e.getSource() == ok)
      {
        IDataModel<?, ?> model = _findRootModel(pRoot, pProject);
        if (model != null)
        {
          Pair<List<String>, List<Pair<String, List<String>>>> deleteCandidates = _extractDeleteCandidates(pRoot);
          List<IDataModel<?, ?>> toDelete = new ArrayList<>();
          // alle gelöschten Tabellen finden
          deleteCandidates.a.stream()
              .map(pTableName -> _findTable(pTableName, model))
              .filter(Objects::nonNull)
              .forEach(toDelete::add);

          // alle gelöschten Spalten finden
          deleteCandidates.b.stream()
              .map(pPair -> new Pair<>(_findTable(pPair.a, model), pPair.b))
              .filter(pPair -> pPair.a != null)
              .map(pPair -> pPair.b
                  .stream()
                  .map(pString -> _findColumn(pString, pPair.a))
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
   * Liefert die Position des Datenmodells das lokal auf der Platte (im Projekt) ist
   * und editiert werden kann.
   *
   * @param pRoot wird nach den o.g. Eigenschaften befragt.
   * @return eine der beiden Konstanten, oder null.
   */
  private static EDirection _getDirectionOfLocalReadableModel(IDiffNode pRoot)
  {
    EDirection left = EDirection.LEFT;
    EDirection right = EDirection.RIGHT;

    if (!pRoot.isRemote(left) && !pRoot.isReadOnly(left))
      return left;
    else if (!pRoot.isRemote(right) && !pRoot.isReadOnly(right))
      return right;

    return null;
  }

  /**
   * Findet alle gelöschten Tabellen und Spalten
   *
   * @param pRoot der Root-Node des Diffs
   * @return Liste mit allen gelöschten Tabellen und eine Liste von Pairs mit den gelöschten Spalten und der dazugehörige Tabellenname.
   */
  @NotNull
  private static Pair<List<String>, List<Pair<String, List<String>>>> _extractDeleteCandidates(@NotNull IDiffNode pRoot)
  {
    EDirection direction = _getDirectionOfLocalReadableModel(pRoot);

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
          if (child.getDiff(direction) == EDiff.DELETED)//Eine Spalte soll gelöscht werden.
          {
            deletedColumns.add(((DefaultMutableTreeNode) child).getUserObject().toString());
          }
        }
        if (!deletedColumns.isEmpty())
          columns.add(new Pair<>(tableName, deletedColumns));

      }
      return new Pair<>(tables, columns);
    }
    return new Pair<>(List.of(), List.of());
  }

  @Nullable
  private static EntityDBDataModel _findTable(@NotNull String pName, @NotNull IDataModel<?, ?> pAlias)
  {
    if (pAlias instanceof AliasDefinitionDataModel)
    {
      //noinspection unchecked
      AliasDefinitionDataObject aliasDefinitionDataObject = (AliasDefinitionDataObject) DataObjectUtil.get(pAlias);
      AbstractAliasDefSubDataModel<?> subdm = aliasDefinitionDataObject.observeSubModel().blockingFirst().orElse(null);
      if (subdm instanceof AliasDefDBDataModel)
      {
        EntityGroupDBDataModel entityGroup = ((AliasDefDBDataModel) subdm).getValue(AliasDefDBDataModel.entityGroup);
        if (entityGroup != null)
        {
          List<EntityDBDataModel> result = entityGroup.getEntities().stream().filter(pEntityDBDataModel -> pEntityDBDataModel.getName().equals(pName)).collect(Collectors.toList());
          if (result.size() == 1)
            return result.get(0);
        }
      }
    }
    return null;
  }

  @Nullable
  private static IEntityFieldDataModel<?> _findColumn(@Nullable String pNameColumn, @NotNull EntityDBDataModel pTable)
  {
    List<IEntityFieldDataModel<?>> result = pTable.getChildren().stream().filter(pField -> pField.getName().equals(pNameColumn)).collect(Collectors.toList());
    if (result.size() == 1)
      return result.get(0);

    return null;
  }

  private static IDataModel<?, ?> _findRootModel(IDiffNode pRoot, Project pProject)
  {
    EDirection direction = _getDirectionOfLocalReadableModel(pRoot);
    if (direction != null)
    {
      IEditorContextProvider p = EditorContextProviderQuery.find(pProject);
      if (p != null)
      {
        IEditorContext<?> ec = p.find(pRoot.getRootName(direction));
        if (ec != null)
          return ec.getModelRootAsProperty().getValue();
      }
    }
    return null;
  }
}
