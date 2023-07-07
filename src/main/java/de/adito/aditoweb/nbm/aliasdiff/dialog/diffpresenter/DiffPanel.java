package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.EDiff;
import de.adito.aditoweb.nbm.designer.commonclasses.icons.IconLoader;
import de.adito.aditoweb.swingcommon.components.common.InfoLabel;
import de.adito.aditoweb.swingcommon.layout.tablelayout.*;
import de.adito.aditoweb.swingcommon.util.ButtonUtil;
import de.adito.aditoweb.swingcommon.util.treeutil.TreeUtil;
import lombok.*;
import org.jetbrains.annotations.Nullable;
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
 * Visualizes the tree created in the DatabaseMatcher and offers various functions for searching,
 * applying values, etc. The tree is visually divided into two trees.
 * Depending on whether there is a value on the left or right, the node is highlighted
 * in color and the value is displayed accordingly. Differences in the values
 * are indicated to the user by different colors.
 *
 * @author t.tasior, 09.02.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public class DiffPanel extends JComponent
{
  private static final String READONLY = " " + NbBundle.getMessage(DiffPanel.class, "TXT_Readonly");
  private static final String REMOTE = " " + NbBundle.getMessage(DiffPanel.class, "TXT_Remote");
  private static final String LOCAL = " " + NbBundle.getMessage(DiffPanel.class, "TXT_Local");
  private static final int BAR_WIDTH = 42;
  private static final Integer PREVIOUS = -1;
  private static final Integer NEXT = 1;
  private static final String EXPAND = "EXPAND";

  private final transient IDiffNode root;
  private final transient IUpdateHandler updateHandler;
  private final JToolTip userdefinedToolTip;
  private final transient IDiffNode topNode;
  private DefaultTreeModel model;
  private TreeSelectionModel selectionModel; //NOSONAR
  private ExpansionListener expansionListener; //NOSONAR
  private JTree leftTree;
  private JTree rightTree;
  private BoundedRangeModel verticalModel; //NOSONAR
  private final Border border = new EmptyBorder(0, 4, 0, 4);

  private transient IDiffNode mouseSelectedNode;
  private final transient NavigationHandler navigationHandler;
  private final ActionUpdate actionUpdateRight = new ActionUpdate(EDirection.RIGHT);
  private final ActionUpdate actionUpdateLeft = new ActionUpdate(EDirection.LEFT);
  private final Action actionRestore = new ActionRestore();

  private final RightHeader rightHeader;
  private JButton buttonExpandCollapse;


  /**
   * Initialize with the root node
   *
   * @param pRoot               root node of the tree
   * @param pUpdateHandler      determines which updates are allowed on gui
   * @param pUserdefinedToolTip a specialized tooltip on the right upper corner
   */
  public DiffPanel(@NonNull IDiffNode pRoot, @Nullable IUpdateHandler pUpdateHandler, @Nullable JToolTip pUserdefinedToolTip)
  {
    root = pRoot;
    navigationHandler = new NavigationHandler(root);
    updateHandler = (pUpdateHandler != null) ? pUpdateHandler : IUpdateHandler.DEFAULT;
    userdefinedToolTip = pUserdefinedToolTip;

    double fill = TableLayoutConstants.FILL;
    double pref = TableLayoutConstants.PREFERRED;
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

    precreate(pRoot);

    rightHeader = new RightHeader();

    tlu.add(1, 1, new LeftHeader()); // left - only view
    tlu.add(3, 1, rightHeader); // right - view and computation
    tlu.add(1, 3, createLeftTree());
    tlu.add(3, 3, createRightTree());
    tlu.add(2, 1, 2, 3, new ButtonBar());

    TreePath[] paths = TreeUtil.getPaths(leftTree, true);
    topNode = (IDiffNode) paths[0].getLastPathComponent();
    TreeUtil.expandChilds(leftTree, paths[0], true);

    rightHeader.updateDifferences();

    InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = getActionMap();
    int fingerFracturingMask = InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

    KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_UP, fingerFracturingMask);
    inputMap.put(key, PREVIOUS);
    Action actionPrevious = new ActionNavigate(PREVIOUS);
    actionMap.put(PREVIOUS, actionPrevious);

    key = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, fingerFracturingMask);
    inputMap.put(key, NEXT);
    Action actionNext = new ActionNavigate(NEXT);
    actionMap.put(NEXT, actionNext);

    updateExpandCollapseState();
  }

  /**
   * Determines, if the given side was changed by any user input
   *
   * @param pDirection Side to check
   * @return true, if the given side was changed by user
   */
  public boolean isChangedByUser(@NonNull EDirection pDirection)
  {
    return pDirection == EDirection.RIGHT ? actionUpdateRight.isExecuted() : actionUpdateLeft.isExecuted();
  }

  /**
   * In order to synchronize behavior (selection, vert. scrolling) of the
   * two trees, the (swing) data models are combined where necessary.
   *
   * @param pRoot the root to display
   */
  private void precreate(@Nullable TreeNode pRoot)
  {
    JTree t = new JTree();
    model = (DefaultTreeModel) t.getModel();
    if (pRoot != null)
      model = new DefaultTreeModel(pRoot, true);

    selectionModel = t.getSelectionModel();
    expansionListener = new ExpansionListener();

    JScrollPane s = new JScrollPane();
    verticalModel = s.getVerticalScrollBar().getModel();
  }

  /**
   * @return the left tree as swing component
   */
  @NonNull
  private JComponent createLeftTree()
  {
    final EDirection left = EDirection.LEFT;
    leftTree = new OverallSelectionTree(model);

    leftTree.setCellRenderer(new CellRenderer(left));
    leftTree.setSelectionModel(selectionModel);
    leftTree.addTreeExpansionListener(expansionListener);
    leftTree.setOpaque(false);
    leftTree.setRootVisible(false);
    initKeyStrokesAndListener(leftTree);

    JScrollPane sp = new JScrollPane();
    sp.setLayout(new LeftVSBLayout());
    sp.setViewport(new MarkerViewport(left));
    sp.setViewportView(leftTree);
    sp.setBorder(border);

    sp.getVerticalScrollBar().setModel(verticalModel);
    sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    return sp;
  }

  /**
   * @return the right tree as swing component
   */
  @NonNull
  private JComponent createRightTree()
  {
    final EDirection right = EDirection.RIGHT;
    rightTree = new OverallSelectionTree(model);

    rightTree.setCellRenderer(new CellRenderer(right));
    rightTree.setSelectionModel(selectionModel);
    rightTree.addTreeExpansionListener(expansionListener);
    rightTree.setOpaque(false);
    rightTree.setRootVisible(false);
    initKeyStrokesAndListener(rightTree);

    JScrollPane sp = new JScrollPane();
    sp.setBorder(border);
    sp.setViewport(new MarkerViewport(right));
    sp.setViewportView(rightTree);

    sp.getVerticalScrollBar().setModel(verticalModel);
    sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    return sp;
  }

  /**
   * Updates the {@link DiffPanel#buttonExpandCollapse}
   */
  private void updateExpandCollapseState()
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

  /**
   * Updates the left and right tree states
   *
   * @param pNode node to start the refresh from, null if root should be refreshed
   */
  private void refreshTreeState(@Nullable TreeNode pNode)
  {
    TreeNode node = (pNode != null) ? pNode : topNode;
    TreePath path = TreeUtil.createSelectionPath(node);
    selectionModel.setSelectionPath(path);

    TreePath selectionPath = TreeUtil.createSelectionPath(node);
    TreeUtil.expandChilds(leftTree, selectionPath, true);

    Rectangle pathBounds = rightTree.getPathBounds(path);
    SwingUtilities.invokeLater(() -> rightTree.scrollRectToVisible(pathBounds));
  }

  /**
   * Initializes the shortcuts
   *
   * @param pComponent Component to initialize
   */
  private void initKeyStrokesAndListener(@NonNull JComponent pComponent)
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
   * The title of the left / right side of the tree
   *
   * @param pRoot      Necessary to determine additional information about the model
   * @param pDirection Direction that needs the title
   * @return the title
   */
  @NonNull
  protected static String getTitle(@NonNull IDiffNode pRoot, @NonNull EDirection pDirection)
  {
    String s = pRoot.getRootName(pDirection);
    s += (pRoot.isRemote(pDirection)) ? REMOTE : LOCAL;

    if (pRoot.isReadOnly(pDirection))
      s += READONLY;

    return s;
  }

  /**
   * Fired when a value is to be written to the data model.
   */
  @RequiredArgsConstructor
  private class ActionUpdate extends AbstractAction
  {
    @NonNull
    private final EDirection direction;

    @Getter
    private boolean executed = false;

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
          executed = true;
        }
      }

      rightHeader.updateDifferences();
    }
  }

  /**
   * Fired when a value is about to be restored in the data model.
   */
  private class ActionRestore extends AbstractAction
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
   * Selects the next or previous node containing a difference.
   */
  @RequiredArgsConstructor
  private class ActionNavigate extends AbstractAction
  {
    private final int direction;

    @Override
    public void actionPerformed(ActionEvent e)
    {
      TreeNode node = null;

      if (direction == PREVIOUS)
        node = navigationHandler.previous(mouseSelectedNode);
      else if (direction == NEXT)
        node = navigationHandler.next(mouseSelectedNode);

      mouseSelectedNode = null;
      refreshTreeState(node);
    }
  }

  /**
   * Handles the expansion on both trees at the same time
   */
  private class ExpansionListener implements TreeExpansionListener
  {
    @Override
    public void treeExpanded(TreeExpansionEvent e)
    {
      if (e.getSource() == leftTree)
        rightTree.expandPath(e.getPath());
      else
        leftTree.expandPath(e.getPath());

      updateExpandCollapseState();
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent e)
    {
      if (e.getSource() == leftTree)
        rightTree.collapsePath(e.getPath());
      else
        leftTree.collapsePath(e.getPath());

      updateExpandCollapseState();
    }
  }

  /**
   * Allows you to select a node when the mouse is clicked anywhere in the area of the tree.
   */
  private static class OverallSelectionTree extends JTree
  {
    public OverallSelectionTree(@Nullable TreeModel pModel)
    {
      super(pModel);
      setLargeModel(true);
      putClientProperty("TreeTableTree", Boolean.TRUE);
    }
  }

  /**
   * Renders an IDiffNode and also visualizes the difference in value in color.
   *
   * @see IDiffNode
   */
  private static class CellRenderer implements TreeCellRenderer
  {
    private static final Color COLOR = new JTable().getForeground();
    private static final Color SELECTED_COLOR = new JTable().getSelectionForeground();
    private final EDirection direction;
    private final CrippledLabel label;
    private final DiffIcon diffIcon;

    public CellRenderer(@NonNull EDirection pDirection)
    {
      direction = pDirection;
      diffIcon = new DiffIcon();
      label = new CrippledLabel();
      label.setIcon(diffIcon);
      label.setFont(label.getFont().deriveFont(Font.PLAIN, 13));
      label.setBorder(null);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
      IDiffNode node = (IDiffNode) value;
      label.setText(node.nameForDisplay(direction));

      // update icon
      node.collectDiffStates(null).update(diffIcon);

      if (selected)
        label.setForeground(SELECTED_COLOR);
      else
        label.setForeground(COLOR);

      return label;
    }
  }

  /**
   * Symbolizes the value differences of an IDiffNode in color and draws the selection of a node.
   */
  @RequiredArgsConstructor
  private static class MarkerViewport extends JViewport
  {
    private static final Color BG_SELECTED = new JTable().getSelectionBackground();

    @NonNull
    private final EDirection direction;

    @Override
    protected void paintComponent(Graphics pG)
    {
      super.paintComponent(pG);
      Graphics2D g = (Graphics2D) pG;
      JTree tree = (JTree) getView();

      paintDiff(g, tree);
      paintTreeSelection(g, tree);
    }

    /**
     * Paints the difference rectangle
     *
     * @param pGraphics Graphics to render to
     * @param pTree     Tree to render
     */
    private void paintDiff(@NonNull Graphics2D pGraphics, @NonNull JTree pTree) //NOSONAR I won't refactor this, because something will break for sure..
    {
      int count = pTree.getRowCount();
      for (int i = 0; i < count; i++)
      {
        TreePath path = pTree.getPathForRow(i);
        if (path != null)
        {
          IDiffNode n = (IDiffNode) path.getLastPathComponent();

          EDiff diff = n.getDiff(direction);
          if (diff != EDiff.NOT_EVALUATED)
          {
            Rectangle r = pTree.getPathBounds(path);
            if (r == null)
              r = new Rectangle();
            r.x = 0;
            r.width = getViewRect().width;
            r.y -= (getViewPosition().y);

            if (diff == EDiff.EQUAL)
            {
              pGraphics.setColor(DiffColors.EQUAL);
              pGraphics.fill(r);
            }

            if (diff == EDiff.DIFFERENT)
            {
              pGraphics.setColor(DiffColors.DIFFERENT);
              pGraphics.fill(r);
            }

            if (diff == EDiff.MISSING)
            {
              pGraphics.setColor(DiffColors.MISSING);
              pGraphics.fill(r);
            }

            if (diff == EDiff.DELETED)
            {
              pGraphics.setColor(DiffColors.DELETED);
              pGraphics.fill(r);
            }
          }
        }
        else
          break;
      }
    }

    /**
     * Paints the selection inside the tree
     *
     * @param pGraphics Graphics to render to
     * @param pTree     Tree to render
     */
    private void paintTreeSelection(@NonNull Graphics2D pGraphics, @NonNull JTree pTree)
    {
      pGraphics.setColor(BG_SELECTED);
      TreePath[] p = pTree.getSelectionPaths();
      if (p != null)
      {
        for (TreePath treePath : p)
        {
          Rectangle r = pTree.getPathBounds(treePath);
          if (r != null)
          {
            r.x = 0;
            r.width = getViewRect().width;
            r.y -= (getViewPosition().y);
            pGraphics.fill(r);
          }
        }
      }
    }
  }

  /**
   * Container for the function buttons (transfer values, expand, etc...)
   */
  private class ButtonBar extends JPanel
  {
    private static final String DISABLED = "_disabled_";
    private JButton buttonUpdateRight;
    private JButton buttonUpdateLeft;
    private JButton buttonRestore;
    private JButton buttonJumpToNextDifference;
    private JButton buttonJumpToPreviousDifference;

    public ButtonBar()
    {
      double fill = TableLayoutConstants.FILL;
      double pref = TableLayoutConstants.PREFERRED;
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
      tlu.add(1, 1, createButtonCollapse());
      tlu.add(1, 5, createButtonPreviousDiff());
      tlu.add(1, 7, createButtonNextDiff());
      tlu.add(1, 9, createButtonUpdateRight());
      tlu.add(1, 11, createButtonUpdateLeft());
      tlu.add(1, 13, createButtonRestore());


      setPreferredSize(new Dimension(BAR_WIDTH, 50));
      setBackground(DiffColors.BACKGROUND);

      UpdateListener updateListener = new UpdateListener(this);

      model.addTreeModelListener(updateListener);
      selectionModel.addTreeSelectionListener(updateListener);

      MouseAdapter mouseAdapter = createNodeSelector();

      leftTree.addMouseListener(mouseAdapter);
      rightTree.addMouseListener(mouseAdapter);
    }

    /**
     * Creates a mouse listener that extracts the currently selected node
     */
    @NonNull
    private MouseAdapter createNodeSelector()
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

    /**
     * Configures the given button, so that it is able to be added into this panel
     *
     * @param pButton  Buton to configure
     * @param pTooltip Tooltip to set
     */
    private void configure(@NonNull JButton pButton, @Nullable String pTooltip)
    {
      ButtonUtil.shrink(pButton);
      pButton.setUI(new BasicButtonUI());
      pButton.setOpaque(false);
      pButton.setToolTipText(pTooltip);
      pButton.setPreferredSize(new Dimension(32, 32));
    }

    /**
     * Enabled / Disables buttons depending on the state of the tree or its nodes.
     */
    void updateButtons()
    {
      final int differences = root.countDifferences();
      TreePath path = selectionModel.getLeadSelectionPath();
      if (buttonUpdateRight.getClientProperty(DISABLED) != DISABLED)
      {
        buttonUpdateRight.setEnabled(false);
        if (path != null && differences > 0)
        {
          IDiffNode n = (IDiffNode) path.getLastPathComponent();
          EDirection right = EDirection.RIGHT;
          boolean remote = root.isRemote(right);
          buttonUpdateRight.setEnabled(updateHandler.canUpdate(right, n, remote));
        }
      }

      if (buttonUpdateLeft.getClientProperty(DISABLED) != DISABLED)
      {
        buttonUpdateLeft.setEnabled(false);
        if (path != null && differences > 0)
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

    /**
     * @return the button to collapse the tree
     */
    @NonNull
    private JButton createButtonCollapse()
    {
      buttonExpandCollapse = new JButton();
      ButtonUtil.shrink(buttonExpandCollapse);
      buttonExpandCollapse.setUI(new BasicButtonUI());
      buttonExpandCollapse.setOpaque(false);
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

    /**
     * @return the button to update the right node
     */
    @NonNull
    private JButton createButtonUpdateRight()
    {
      buttonUpdateRight = new JButton(IconLoader.loadIcon(IconLoader.VIcon.ANGLE_DOUBLE_RIGHT, 16));
      buttonUpdateRight.putClientProperty(DISABLED, DISABLED);
      buttonUpdateRight.setEnabled(false);

      configure(buttonUpdateRight, NbBundle.getMessage(DiffPanel.class, "TXT_Provide_Data_To_R", getTitle(root, EDirection.RIGHT)) + " (Ctrl + R)");

      if (!root.isReadOnly(EDirection.RIGHT))
      {
        buttonUpdateRight.addActionListener(actionUpdateRight);
        buttonUpdateRight.putClientProperty(DISABLED, null);
      }

      return buttonUpdateRight;
    }

    /**
     * @return the button to update the left node
     */
    @NonNull
    private JButton createButtonUpdateLeft()
    {
      buttonUpdateLeft = new JButton(IconLoader.loadIcon(IconLoader.VIcon.ANGLE_DOUBLE_LEFT, 16));
      buttonUpdateLeft.putClientProperty(DISABLED, DISABLED);
      buttonUpdateLeft.setEnabled(false);

      configure(buttonUpdateLeft, NbBundle.getMessage(DiffPanel.class, "TXT_Provide_Data_To_L", getTitle(root, EDirection.LEFT)) + " (Ctrl + L)");

      if (!root.isReadOnly(EDirection.LEFT))
      {
        buttonUpdateLeft.addActionListener(actionUpdateLeft);
        buttonUpdateLeft.putClientProperty(DISABLED, null);
      }

      return buttonUpdateLeft;
    }

    /**
     * @return the button to restore the selected node
     */
    @NonNull
    private JComponent createButtonRestore()
    {
      buttonRestore = new JButton(IconLoader.loadIcon(IconLoader.VIcon.ARROW_BACKWARD, 16));
      configure(buttonRestore, NbBundle.getMessage(DiffPanel.class, "TXT_Restore_Data"));
      buttonRestore.setEnabled(false);
      buttonRestore.addActionListener(actionRestore);
      return buttonRestore;
    }

    /**
     * @return the button to select the previous diff node
     */
    @NonNull
    private JButton createButtonPreviousDiff()
    {
      buttonJumpToPreviousDifference = new JButton(IconLoader.loadIcon(IconLoader.VIcon.CHEVRON_UP_SMALL, 16));
      configure(buttonJumpToPreviousDifference, NbBundle.getMessage(DiffPanel.class, "TXT_Expand_Next_Difference_Up"));

      buttonJumpToPreviousDifference.addActionListener(e -> {
        TreeNode previous = navigationHandler.previous(mouseSelectedNode);
        mouseSelectedNode = null;
        refreshTreeState(previous);
      });

      return buttonJumpToPreviousDifference;
    }

    /**
     * @return the button to select the next diff node
     */
    @NonNull
    private JButton createButtonNextDiff()
    {
      buttonJumpToNextDifference = new JButton(IconLoader.loadIcon(IconLoader.VIcon.CHEVRON_DOWN_SMALL, 16));
      configure(buttonJumpToNextDifference, NbBundle.getMessage(DiffPanel.class, "TXT_Expand_Next_Difference_Down"));

      buttonJumpToNextDifference.addActionListener(e -> {
        TreeNode next = navigationHandler.next(mouseSelectedNode);
        mouseSelectedNode = null;
        refreshTreeState(next);
      });

      return buttonJumpToNextDifference;
    }
  }

  /**
   * Panel above the left tree
   */
  private class LeftHeader extends JPanel
  {
    public LeftHeader()
    {
      double fill = TableLayoutConstants.FILL;
      double pref = TableLayoutConstants.PREFERRED;
      double gap = 4;

      double[] cols = {gap, fill};
      double[] rows = {gap,
                       pref,
                       gap,
                       };

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);

      JLabel label = new JLabel(getTitle(root, EDirection.LEFT));
      label.setFont(label.getFont().deriveFont(Font.BOLD));
      tlu.add(1, 1, label);
    }
  }

  /**
   * Panel above the right tree
   */
  private class RightHeader extends JPanel
  {
    private final String differences = NbBundle.getMessage(DiffPanel.class, "TXT_Differences") + " ";
    private JLabel diffPresenter;

    public RightHeader()
    {
      double fill = TableLayoutConstants.FILL;
      double pref = TableLayoutConstants.PREFERRED;
      double gap = 4;

      double[] cols = {gap, fill, gap, 150, gap, pref, gap};
      double[] rows = {gap,
                       pref,
                       gap};

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);
      tlu.add(1, 1, createNameDisplay());
      tlu.add(3, 1, createDiffPresenter());
      tlu.add(5, 1, createInfoComponent());
    }

    /**
     * Updates the differences count
     */
    public void updateDifferences()
    {
      diffPresenter.setText(differences + root.countDifferences());
    }

    /**
     * @return the label to display the name
     */
    @NonNull
    private JLabel createNameDisplay()
    {
      JLabel l = new JLabel(getTitle(root, EDirection.RIGHT));
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      return l;
    }

    /**
     * @return the label to display the amount of differences found
     */
    @NonNull
    private JLabel createDiffPresenter()
    {
      diffPresenter = new JLabel(differences + 0);
      diffPresenter.setFont(diffPresenter.getFont().deriveFont(Font.BOLD));
      return diffPresenter;
    }

    /**
     * @return the label to display the user defined tooltip
     */
    @NonNull
    private JComponent createInfoComponent()
    {
      InfoLabel l = new InfoLabel("", userdefinedToolTip);
      l.setInfo("");
      return l;
    }
  }

  /**
   * Updates the buttons on specific actions
   */
  @RequiredArgsConstructor
  private class UpdateListener implements TreeSelectionListener, TreeModelListener
  {
    @NonNull
    private final ButtonBar buttonBar;

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
      // ignore
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e)
    {
      // ignore
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e)
    {
      // ignore
    }
  }


}
