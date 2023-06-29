package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.nbm.designer.commonclasses.icons.IconLoader;
import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.EDiff;
import de.adito.aditoweb.swingcommon.components.common.InfoLabel;
import de.adito.aditoweb.swingcommon.layout.tablelayout.*;
import de.adito.aditoweb.swingcommon.util.ButtonUtil;
import de.adito.aditoweb.swingcommon.util.treeutil.TreeUtil;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;


/**
 * Visualisiert den im DatabaseMatcher erzeugten Baum und bietet diverse
 * Funktionen zum Suchen, Werte übernehmen etc... an.
 * Der Baum wird optisch in zwei Bäume aufgeteilt. Je nachdem ob ein Wert links oder rechts
 * vorhanden ist, wird der Knoten farblich hervorgehoben, und der Wert entsprechen dargestellt.
 * Unterschiede bei den Werten werden dem Benutzer durch verschiedene Farben angezeigt.
 *
 * @author t.tasior, 09.02.2018
 */
public class DiffPanel extends JComponent
{
  private final static String READONLY = " " + NbBundle.getMessage(DiffPanel.class, "TXT_Readonly");
  private final static String REMOTE = " " + NbBundle.getMessage(DiffPanel.class, "TXT_Remote");
  private final static String LOCAL = " " + NbBundle.getMessage(DiffPanel.class, "TXT_Local");
  private static final int BAR_WIDTH = 42;

  private final IDiffNode root;
  private final IUpdateHandler updateHandler;
  private final JToolTip userdefinedToolTip;
  private final IDiffNode topNode;
  private final Integer PREVIOUS = -1;
  private final Integer NEXT = 1;
  private DefaultTreeModel model;
  private TreeSelectionModel selectionModel;
  private _ExpansionListener expansionListener;
  private JTree leftTree;
  private JTree rightTree;
  private BoundedRangeModel verticalModel;
  private final Border border = new EmptyBorder(0, 4, 0, 4);

  private IDiffNode mouseSelectedNode;
  private final NavigationHandler navigationHandler;
  private final Action actionUpdateRight = new _ActionUpdate(EDirection.RIGHT);
  private final Action actionUpdateLeft = new _ActionUpdate(EDirection.LEFT);
  private final Action actionRestore = new _ActionRestore();

  private _RightHeader rightHeader;
  private final String EXPAND = "EXPAND";
  private JButton buttonExpandCollapse;


  /**
   * Initialisierung mit dem Wurzelknoten.
   *
   * @param pRoot               der oberste Knoten der Baumstruktur.
   * @param pUpdateHandler      entscheidet welche Updates auf den Datenmodellen erlaubt sind.
   * @param pUserdefinedToolTip ein spezieller Tooltip für die Info Komponente rechts oben.
   */

  public DiffPanel(IDiffNode pRoot, IUpdateHandler pUpdateHandler, JToolTip pUserdefinedToolTip)
  {
    root = pRoot;
    navigationHandler = new NavigationHandler(root);
    updateHandler = (pUpdateHandler != null) ? pUpdateHandler : new IUpdateHandler.DefaultHandler();
    userdefinedToolTip = pUserdefinedToolTip;

    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    double gap = 4;

    double[] cols = {gap, fill, pref, fill, gap};
    double[] rows = {gap,
                     pref,
                     0,
                     fill,
                     gap * 4,
                     };

    setLayout(new TableLayout(cols, rows));
    TableLayoutUtil tlu = new TableLayoutUtil(this);

    _precreate(pRoot);

    tlu.add(1, 1, new _LeftHeader());//Nur Darstellung
    tlu.add(3, 1, _createRightHeader());//Darstellung und Aktualisierung.
    tlu.add(1, 3, _createLeftTree());
    tlu.add(3, 3, _createRightTree());
    tlu.add(2, 1, 2, 3, new _ButtonBar());

    TreePath[] paths = TreeUtil.getPaths(leftTree, true);
    topNode = (IDiffNode) paths[0].getLastPathComponent();
    TreeUtil.expandChilds(leftTree, paths[0], true);

    rightHeader.updateDifferences();

    InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = getActionMap();
    int fingerFracturingMask = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

    KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_UP, fingerFracturingMask);
    inputMap.put(key, PREVIOUS);
    Action actionPrevious = new _ActionNavigate(PREVIOUS);
    actionMap.put(PREVIOUS, actionPrevious);

    key = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, fingerFracturingMask);
    inputMap.put(key, NEXT);
    Action actionNext = new _ActionNavigate(NEXT);
    actionMap.put(NEXT, actionNext);

    _updateExpandCollapseState();

  }

  private _RightHeader _createRightHeader()
  {
    rightHeader = new _RightHeader();
    return rightHeader;
  }

  /**
   * Liefert den Titel des linken Baumes unter Zuhilfename der Baumwurzel.
   *
   * @param pRoot notwendig um Zusatinfos über das Datenmodell zu ermitteln.
   * @return den Namen des Datenmodells plus weiteren Eigenschaften in Klammern.
   */
  public static String getLeftTitle(IDiffNode pRoot)
  {
    String s = pRoot.getRootName(EDirection.LEFT);
    s += (pRoot.isRemote(EDirection.LEFT)) ? REMOTE : LOCAL;

    if (pRoot.isReadOnly(EDirection.LEFT))
      s += READONLY;

    return s;
  }

  /**
   * Liefert den Titel des rechten Baumes unter Zuhilfename der Baumwurzel.
   *
   * @param pRoot notwendig um Zusatinfos über das Datenmodell zu ermitteln.
   * @return den Namen des Datenmodells plus weiteren Eigenschaften in Klammern.
   */
  public static String getRightTitle(IDiffNode pRoot)
  {
    String s = pRoot.getRootName(EDirection.RIGHT);
    s += (pRoot.isRemote(EDirection.RIGHT)) ? REMOTE : LOCAL;

    if (pRoot.isReadOnly(EDirection.RIGHT))
      s += READONLY;

    return s;
  }

  /**
   * Um synchrones Verhalten (Selektion, vert. Scrollen) der beiden Bäume zu gewährleisten,
   * werden die (Swing) Datenmodelle wo notwendig zusammengefasst.
   *
   * @param pRoot der Darzustellende Baum.
   */
  private void _precreate(TreeNode pRoot)
  {
    JTree t = new JTree();
    model = (DefaultTreeModel) t.getModel();
    if (pRoot != null)
      model = new DefaultTreeModel(pRoot, true);

    selectionModel = t.getSelectionModel();
    expansionListener = new _ExpansionListener();

    JScrollPane s = new JScrollPane();
    verticalModel = s.getVerticalScrollBar().getModel();
  }

  private JComponent _createLeftTree()
  {
    final EDirection left = EDirection.LEFT;
    leftTree = new _OverallSelectionTree(model);

    leftTree.setCellRenderer(new _CellRenderer(left));
    leftTree.setSelectionModel(selectionModel);
    leftTree.addTreeExpansionListener(expansionListener);
    leftTree.setOpaque(false);
    leftTree.setRootVisible(false);
    _initKeyStrokesAndListener(leftTree);

    JScrollPane sp = new JScrollPane();
    sp.setLayout(new LeftVSBLayout());
    sp.setViewport(new _MarkerViewport(left));
    sp.setViewportView(leftTree);
    sp.setBorder(border);
    //sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    sp.getVerticalScrollBar().setModel(verticalModel);
    //sp.getHorizontalScrollBar().setModel(horizontalModel);
    sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    return sp;
  }

  /**
   * Signalisiert dem buttonExpandCollapse welches Icon anzuzeigen ist.
   */
  private void _updateExpandCollapseState()
  {
    TreePath path = TreeUtil.createSelectionPath(root);
    Enumeration<TreePath> en = leftTree.getExpandedDescendants(path);
    if (en == null)
      buttonExpandCollapse.putClientProperty(EXPAND, Boolean.TRUE);
    else
    {
      ArrayList<TreePath> list = Collections.list(en);
      if (list.size() <= 1)
        buttonExpandCollapse.putClientProperty(EXPAND, Boolean.TRUE);
      else
        buttonExpandCollapse.putClientProperty(EXPAND, Boolean.FALSE);
    }
  }


  private JComponent _createRightTree()
  {
    final EDirection right = EDirection.RIGHT;
    rightTree = new _OverallSelectionTree(model);

    rightTree.setCellRenderer(new _CellRenderer(right));
    rightTree.setSelectionModel(selectionModel);
    rightTree.addTreeExpansionListener(expansionListener);
    rightTree.setOpaque(false);
    rightTree.setRootVisible(false);
    _initKeyStrokesAndListener(rightTree);

    JScrollPane sp = new JScrollPane();
    sp.setBorder(border);
    sp.setViewport(new _MarkerViewport(right));
    sp.setViewportView(rightTree);
    //sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    sp.getVerticalScrollBar().setModel(verticalModel);
    //sp.getHorizontalScrollBar().setModel(horizontalModel);
    sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    return sp;
  }

  /**
   * Aktualisiert Aussehen und Position des Baumes
   *
   * @param pNode Startpunkt im Baum.
   */
  private void _refreshTreeState(TreeNode pNode)
  {
    TreeNode node = (pNode != null) ? pNode : topNode;
    TreePath path = TreeUtil.createSelectionPath(node);
    selectionModel.setSelectionPath(path);

    TreePath selectionPath = TreeUtil.createSelectionPath(node);
    TreeUtil.expandChilds(leftTree, selectionPath, true);

    Rectangle pathBounds = rightTree.getPathBounds(path);
    SwingUtilities.invokeLater(() -> rightTree.scrollRectToVisible(pathBounds));
  }

  private void _initKeyStrokesAndListener(JComponent pComponent)
  {
    InputMap in = pComponent.getInputMap(WHEN_FOCUSED);
    ActionMap act = pComponent.getActionMap();

    KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);

    EDirection left = EDirection.LEFT;
    in.put(key, left);
    act.put(left, actionUpdateLeft);

    key = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
    EDirection right = EDirection.RIGHT;
    in.put(key, right);
    act.put(right, actionUpdateRight);

    key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);
    String restore = "restore";
    in.put(key, restore);
    act.put(restore, actionRestore);
  }

  /**
   * Wird ausgelöst wenn ein Wert in das Datenmodell geschrieben werden soll.
   */
  private class _ActionUpdate extends AbstractAction
  {
    private final EDirection direction;

    public _ActionUpdate(EDirection pDirection)
    {
      direction = pDirection;
    }

    @Override
    public boolean isEnabled()
    {
      if (root.isReadOnly(direction))
        return false;

      TreePath[] sel = selectionModel.getSelectionPaths();
      return sel != null;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      TreePath[] sel = selectionModel.getSelectionPaths();
      if (sel != null)
      {
        List<TreePath> helper = new ArrayList<>(Arrays.asList(sel));

        for (TreePath path : helper)
        {
          IDiffNode n = (IDiffNode) path.getLastPathComponent();

          n.getPair().update(direction);
          TreeUtil.refresh(leftTree, n);
        }
      }

      rightHeader.updateDifferences();
    }
  }

  private class _ActionRestore extends AbstractAction
  {
    @Override
    public boolean isEnabled()
    {
      TreePath[] sel = selectionModel.getSelectionPaths();
      return sel != null;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      TreePath[] sel = selectionModel.getSelectionPaths();
      if (sel != null)
      {
        List<TreePath> helper = new ArrayList<>(Arrays.asList(sel));

        for (TreePath path : helper)
        {
          IDiffNode n = (IDiffNode) path.getLastPathComponent();

          n.getPair().restore();
          TreeUtil.refresh(leftTree, n);
        }
      }
    }
  }

  /**
   * Selektiert den nächsten bzw. vorherigen Node mit einer Differenz.
   */
  private class _ActionNavigate extends AbstractAction
  {
    private final int direction;

    public _ActionNavigate(int pDirection)
    {
      direction = pDirection;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      TreeNode node = null;

      if (direction == PREVIOUS)
        node = navigationHandler.previous(mouseSelectedNode);
      else if (direction == NEXT)
        node = navigationHandler.next(mouseSelectedNode);

      mouseSelectedNode = null;
      _refreshTreeState(node);
    }
  }

  /**
   * Sorgt dafür dass beide Bäume synchron auf/zuklappen.
   */
  private class _ExpansionListener implements TreeExpansionListener
  {
    @Override
    public void treeExpanded(TreeExpansionEvent e)
    {
      if (e.getSource() == leftTree)
        rightTree.expandPath(e.getPath());
      else
        leftTree.expandPath(e.getPath());

      _updateExpandCollapseState();
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent e)
    {
      if (e.getSource() == leftTree)
        rightTree.collapsePath(e.getPath());
      else
        leftTree.collapsePath(e.getPath());

      _updateExpandCollapseState();
    }
  }

  /**
   * Ermöglicht das Selektieren eines Knotens wenn mit der Maus
   * irgendwo im Bereich des Baumes geklickt wird.
   */
  private static class _OverallSelectionTree extends JTree
  {
    public _OverallSelectionTree(TreeModel pModel)
    {
      super(pModel);
      setLargeModel(true);
      //Konstante aus DarculaTreeUI - ich komme anders nicht ran.
      putClientProperty("TreeTableTree", Boolean.TRUE);
    }
  }

  /**
   * Rendert einen IDiffNode und visualisiert zusätzlich farblich
   * den Werteunterschied.
   *
   * @see IDiffNode
   */
  private static class _CellRenderer implements TreeCellRenderer
  {
    private final Color _COLOR = new JTable().getForeground();
    private final Color _SELECTED_COLOR = new JTable().getSelectionForeground();
    private final EDirection direction;
    private final CrippledLabel label;
    private final DiffIcon diffIcon;

    public _CellRenderer(EDirection pDirection)
    {
      direction = pDirection;
      diffIcon = new DiffIcon();
      label = new CrippledLabel();
      label.setIcon(diffIcon);
      label.setFont(label.getFont().deriveFont(Font.PLAIN, 13));
      label.setBorder(null);
    }

    private void _updateIcon(IDiffNode pNode)
    {
      DiffStateCollector coll = pNode.collectDiffStates(null);
      coll.update(diffIcon);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
      IDiffNode node = (IDiffNode) value;
      label.setText(node.nameForDisplay(direction));

      _updateIcon(node);
      if (selected)
        label.setForeground(_SELECTED_COLOR);
      else
        label.setForeground(_COLOR);

      return label;
    }

  }

  /**
   * Symbolisiert farblich die Wertunterschiede eines IDiffNode
   * und zeichnet die Selktion eines Knotens.
   */
  private static class _MarkerViewport extends JViewport
  {
    private final Color _BG_SELECTED = new JTable().getSelectionBackground();
    private final EDirection direction;

    public _MarkerViewport(EDirection pDirection)
    {
      direction = pDirection;
    }

    @Override
    protected void paintComponent(Graphics pG)
    {
      super.paintComponent(pG);
      Graphics2D g = (Graphics2D) pG;
      JTree tree = (JTree) getView();

      _paintDiff(g, tree);
      _paintTreeSelection(g, tree);

    }

    private void _paintDiff(Graphics2D g, JTree t)
    {
      int count = t.getRowCount();
      for (int i = 0; i < count; i++)
      {
        TreePath path = t.getPathForRow(i);
        if (path != null)
        {
          IDiffNode n = (IDiffNode) path.getLastPathComponent();

          EDiff diff = n.getDiff(direction);
          if (diff == EDiff.NOT_EVALUATED)
            continue;

          Rectangle r = t.getPathBounds(path);
          if (r == null)
            r = new Rectangle();
          r.x = 0;
          r.width = getViewRect().width;
          r.y -= (getViewPosition().y);

          if (diff == EDiff.EQUAL)
          {
            g.setColor(DiffColors.EQUAL);
            g.fill(r);
          }

          if (diff == EDiff.DIFFERENT)
          {
            g.setColor(DiffColors.DIFFERENT);
            g.fill(r);
          }

          if (diff == EDiff.MISSING)
          {
            g.setColor(DiffColors.MISSING);
            g.fill(r);
          }

          if (diff == EDiff.DELETED)
          {
            g.setColor(DiffColors.DELETED);
            g.fill(r);
          }

        }
        else
          break;
      }
    }

    private void _paintTreeSelection(Graphics2D g, JTree t)
    {
      g.setColor(_BG_SELECTED);
      TreePath[] p = t.getSelectionPaths();
      if (p != null)
      {
        for (TreePath treePath : p)
        {
          Rectangle r = t.getPathBounds(treePath);
          if (r != null)
          {
            r.x = 0;
            r.width = getViewRect().width;
            r.y -= (getViewPosition().y);
            g.fill(r);
          }

        }
      }
    }
  }

  /**
   * Container für die Funktionsbuttons (Werte übernehmen, Aufklappen etc...)
   */
  private class _ButtonBar extends JPanel
  {
    private final String disabled = "_disabled_";
    private JButton buttonUpdateRight;
    private JButton buttonUpdateLeft;
    private JButton buttonRestore;
    private JButton buttonJumpToNextDifference;
    private JButton buttonJumpToPreviousDifference;

    public _ButtonBar()
    {

      double fill = TableLayout.FILL;
      double pref = TableLayout.PREFERRED;
      double gap = 4;

      double[] cols = {gap, pref, gap};
      double[] rows = {gap,
                       pref,
                       gap,
                       pref,
                       gap * 2,
                       pref,
                       gap,
                       pref,
                       gap,
                       pref,
                       gap,
                       pref,
                       gap,
                       pref,
                       fill};

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);
      tlu.add(1, 1, _createButtonCollapse());
      tlu.add(1, 5, _createButtonPreviousDiff());
      tlu.add(1, 7, _createButtonNextDiff());
      tlu.add(1, 9, _createButtonUpdateRight());
      tlu.add(1, 11, _createButtonUpdateLeft());
      tlu.add(1, 13, _createButtonRestore());


      setPreferredSize(new Dimension(BAR_WIDTH, 50));
      setBackground(DiffColors.BACKGROUND);

      _UpdateListener updateListener = new _UpdateListener(this);

      model.addTreeModelListener(updateListener);
      selectionModel.addTreeSelectionListener(updateListener);

      MouseAdapter mouseAdapter = _createNodeSelector();

      leftTree.addMouseListener(mouseAdapter);
      rightTree.addMouseListener(mouseAdapter);
    }

    /**
     * Ist nicht mein Stil, vereinfacht das Ganze aber...
     */
    private MouseAdapter _createNodeSelector()
    {
      return new MouseAdapter()
      {
        @Override
        public void mousePressed(MouseEvent e)
        {
          TreePath lsp = ((JTree) e.getSource()).getLeadSelectionPath();
          if (lsp != null)
            mouseSelectedNode = (IDiffNode) lsp.getLastPathComponent();
        }
      };
    }


    private void _configure(JButton pButton, String pTooltip)
    {
      ButtonUtil.shrink(pButton);
      pButton.setUI(new BasicButtonUI());
      pButton.setOpaque(false);
      //pButton.setEnabled(false);
      pButton.setToolTipText(pTooltip);
      pButton.setPreferredSize(new Dimension(32, 32));
    }

    /**
     * Aktiviert/Deaktiviert Buttons in Abhängigkeit vom
     * Zustand des Baumes, bzw. seiner Knoten.
     */
    void updateButtons()
    {
      final int differences = root.countDifferences();
      TreePath path = selectionModel.getLeadSelectionPath();
      if (buttonUpdateRight.getClientProperty(disabled) != disabled)
      {
        buttonUpdateRight.setEnabled(false);
        if ((path != null) & (differences > 0))
        {
          IDiffNode n = (IDiffNode) path.getLastPathComponent();
          EDirection right = EDirection.RIGHT;
          boolean remote = root.isRemote(right);
          buttonUpdateRight.setEnabled(updateHandler.canUpdate(right, n, remote));
        }
      }

      if (buttonUpdateLeft.getClientProperty(disabled) != disabled)
      {
        buttonUpdateLeft.setEnabled(false);
        if ((path != null) & (differences > 0))
        {
          IDiffNode n = (IDiffNode) path.getLastPathComponent();
          EDirection left = EDirection.LEFT;
          boolean remote = root.isRemote(left);
          buttonUpdateLeft.setEnabled(updateHandler.canUpdate(left, n, remote));
        }
      }

      DiffStateCollector c = root.collectDiffStates(null);
      buttonRestore.setEnabled(c.canRestore());

      buttonJumpToPreviousDifference.setEnabled(differences > 0);
      buttonJumpToNextDifference.setEnabled(differences > 0);
    }

    private JButton _createButtonCollapse()
    {
      buttonExpandCollapse = new JButton();
      ButtonUtil.shrink(buttonExpandCollapse);
      buttonExpandCollapse.setUI(new BasicButtonUI());
      buttonExpandCollapse.setOpaque(false);
      //GUIHelper.setPreferredHeight(16, buttonExpandCollapse);
      buttonExpandCollapse.addActionListener(e -> {
        if (Boolean.TRUE.equals(buttonExpandCollapse.getClientProperty(EXPAND)))
          TreeUtil.expandAll(leftTree, true);
        else
        {
          for (int i = 0; i < topNode.getChildCount(); i++)
          {
            TreeNode node = topNode.getChildAt(i);
            TreePath path = TreeUtil.createSelectionPath(node);
            TreeUtil.expandPath(leftTree, path, false);
          }
        }
      });

      buttonExpandCollapse.addPropertyChangeListener(EXPAND, evt -> {
        if (Boolean.TRUE.equals(buttonExpandCollapse.getClientProperty(EXPAND)))
          buttonExpandCollapse.setIcon(IconLoader.loadIcon(IconLoader.VIcon.PLUS_SQUARE_LEFT_O, 16));
        else
          buttonExpandCollapse.setIcon(IconLoader.loadIcon(IconLoader.VIcon.MINUS_SQUARE_LEFT_O, 16));
      });
      return buttonExpandCollapse;
    }


    private JButton _createButtonUpdateRight()
    {
      buttonUpdateRight = new JButton(IconLoader.loadIcon(IconLoader.VIcon.ANGLE_DOUBLE_RIGHT, 16));
      buttonUpdateRight.putClientProperty(disabled, disabled);
      buttonUpdateRight.setEnabled(false);

      _configure(buttonUpdateRight, NbBundle.getMessage(DiffPanel.class, "TXT_Provide_Data_To_R", getRightTitle(root)) + " (Ctrl + R)");

      if (!root.isReadOnly(EDirection.RIGHT))
      {
        buttonUpdateRight.addActionListener(actionUpdateRight);
        buttonUpdateRight.putClientProperty(disabled, null);
      }

      return buttonUpdateRight;
    }

    private JButton _createButtonUpdateLeft()
    {
      buttonUpdateLeft = new JButton(IconLoader.loadIcon(IconLoader.VIcon.ANGLE_DOUBLE_LEFT, 16));
      buttonUpdateLeft.putClientProperty(disabled, disabled);
      buttonUpdateLeft.setEnabled(false);

      _configure(buttonUpdateLeft, NbBundle.getMessage(DiffPanel.class, "TXT_Provide_Data_To_L", getLeftTitle(root)) + " (Ctrl + L)");

      if (!root.isReadOnly(EDirection.LEFT))
      {
        buttonUpdateLeft.addActionListener(actionUpdateLeft);
        buttonUpdateLeft.putClientProperty(disabled, null);
      }

      return buttonUpdateLeft;
    }


    private JComponent _createButtonRestore()
    {
      buttonRestore = new JButton(IconLoader.loadIcon(IconLoader.VIcon.ARROW_BACKWARD, 16));
      _configure(buttonRestore, NbBundle.getMessage(DiffPanel.class, "TXT_Restore_Data"));
      buttonRestore.setEnabled(false);
      buttonRestore.addActionListener(actionRestore);
      return buttonRestore;
    }


    private JButton _createButtonPreviousDiff()
    {
      buttonJumpToPreviousDifference = new JButton(IconLoader.loadIcon(IconLoader.VIcon.CHEVRON_UP_SMALL, 16));
      _configure(buttonJumpToPreviousDifference, NbBundle.getMessage(DiffPanel.class, "TXT_Expand_Next_Difference_Up"));

      buttonJumpToPreviousDifference.addActionListener(e -> {
        TreeNode previous = navigationHandler.previous(mouseSelectedNode);
        mouseSelectedNode = null;
        _refreshTreeState(previous);
      });


      return buttonJumpToPreviousDifference;
    }


    private JButton _createButtonNextDiff()
    {
      buttonJumpToNextDifference = new JButton(IconLoader.loadIcon(IconLoader.VIcon.CHEVRON_DOWN_SMALL, 16));
      _configure(buttonJumpToNextDifference, NbBundle.getMessage(DiffPanel.class, "TXT_Expand_Next_Difference_Down"));

      buttonJumpToNextDifference.addActionListener(e -> {
        TreeNode next = navigationHandler.next(mouseSelectedNode);
        mouseSelectedNode = null;
        _refreshTreeState(next);
      });

      return buttonJumpToNextDifference;
    }


  }

  /**
   * Panel oberhalb dem linken Baum.
   */
  private class _LeftHeader extends JPanel
  {
    public _LeftHeader()
    {
      double fill = TableLayout.FILL;
      double pref = TableLayout.PREFERRED;
      double gap = 4;

      double[] cols = {gap, fill};
      double[] rows = {gap,
                       pref,
                       gap,
                       };

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);
      tlu.add(1, 1, _createDisplay());

    }

    private JLabel _createDisplay()
    {
      String rootName = root.getRootName(EDirection.LEFT);
      rootName += (root.isRemote(EDirection.LEFT)) ? REMOTE : LOCAL;

      if (root.isReadOnly(EDirection.LEFT))
        rootName += READONLY;

      JLabel l = new JLabel(rootName);
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      return l;
    }
  }

  /**
   * Panel oberhalb dem rechten Baum
   */
  private class _RightHeader extends JPanel
  {
    private JLabel diffPresenter;
    private final String differences = NbBundle.getMessage(DiffPanel.class, "TXT_Differences") + " ";

    public _RightHeader()
    {
      double fill = TableLayout.FILL;
      double pref = TableLayout.PREFERRED;
      double gap = 4;

      double[] cols = {gap, fill, gap, 150, gap, pref, gap};
      double[] rows = {gap,
                       pref,
                       gap};

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);
      tlu.add(1, 1, _createNameDisplay());
      tlu.add(3, 1, _createDiffPresenter());
      tlu.add(5, 1, _createInfoComponent());
    }

    private JLabel _createNameDisplay()
    {
      String rootName = root.getRootName(EDirection.RIGHT);
      rootName += (root.isRemote(EDirection.RIGHT)) ? REMOTE : LOCAL;

      if (root.isReadOnly(EDirection.RIGHT))
        rootName += READONLY;

      JLabel l = new JLabel(rootName);
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      return l;
    }

    private JLabel _createDiffPresenter()
    {
      diffPresenter = new JLabel(differences + 0);
      diffPresenter.setFont(diffPresenter.getFont().deriveFont(Font.BOLD));
      return diffPresenter;
    }

    private JComponent _createInfoComponent()
    {
      InfoLabel l = new InfoLabel("", userdefinedToolTip);
      l.setInfo("");
      return l;
    }

    /**
     * Ermittelt die Anzahl der Differenzen in den Datenmodellen und
     * zeig sie an.
     */
    public void updateDifferences()
    {
      diffPresenter.setText(differences + root.countDifferences());
    }
  }

  /**
   * Aktualisiert die Buttons aufgrund bestimmter Ereignisse.
   */
  private class _UpdateListener extends MouseAdapter implements TreeModelListener, TreeSelectionListener
  {
    private final _ButtonBar buttonBar;

    public _UpdateListener(_ButtonBar pButtonBar)
    {
      buttonBar = pButtonBar;
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e)
    {
      buttonBar.updateButtons();
    }

    @Override
    public void valueChanged(TreeSelectionEvent e)
    {
      buttonBar.updateButtons();
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e)
    {

    }

    @Override
    public void treeNodesInserted(TreeModelEvent e)
    {

    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e)
    {

    }
  }


}
