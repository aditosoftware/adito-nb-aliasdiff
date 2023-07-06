package de.adito.aditoweb.nbm.aliasdiff.impl.gui;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.*;
import de.adito.aditoweb.swingcommon.layout.tablelayout.*;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;

/**
 * Custom tooltip for the diff dialog
 *
 * @author w.glanzer, 12.07.2022
 */
public class DiffDBToolTip extends JToolTip
{
  private final ToolTipPanel toolTipPanel;

  public DiffDBToolTip()
  {
    setLayout(new BorderLayout());
    toolTipPanel = new ToolTipPanel();
    add(toolTipPanel);
  }

  @Override
  public Dimension getPreferredSize()
  {
    return toolTipPanel.getPreferredSize();
  }

  /**
   * Explains in a tooltip the colors in the diff dialog as well as the possible changes in the alias data models.
   */
  private static class ToolTipPanel extends JPanel
  {
    public ToolTipPanel()
    {
      double fill = TableLayoutConstants.FILL;
      double pref = TableLayoutConstants.PREFERRED;
      double gap = 4;

      double[] cols = {gap, pref, gap, pref, gap, fill, gap, pref};
      double[] rows = {gap,
                       pref,
                       gap,
                       pref,
                       gap,
                       pref,
                       gap,
                       pref,
                       gap,
                       pref,
                       gap * 3
      };

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);
      tlu.add(1, 1, createLabel(DiffColors.EQUAL, NbBundle.getMessage(DiffDBToolTip.class, "TXT_EQUAL")));
      tlu.add(1, 3, createLabel(DiffColors.DIFFERENT, NbBundle.getMessage(DiffDBToolTip.class, "TXT_DIFFERENT")));
      tlu.add(1, 5, createLabel(DiffColors.MISSING, NbBundle.getMessage(DiffDBToolTip.class, "TXT_MISSING")));
      tlu.add(1, 7, createLabel(DiffColors.DELETED, NbBundle.getMessage(DiffDBToolTip.class, "TXT_DELETED")));
      tlu.add(1, 9, createTextbox());

      setBackground(new JLabel().getBackground());
    }

    /**
     * Creates a new label that explains a different color
     *
     * @param pColor Color to describe
     * @param pText  Text that describes the given colors
     * @return the label
     */
    @NonNull
    private JComponent createLabel(@Nullable Color pColor, @Nullable String pText)
    {
      JLabel label = new JLabel(pText);
      DiffIcon diffIcon = new DiffIcon();
      diffIcon.setDifferentColor();
      label.setIcon(new ToolTipIcon(pColor));
      return label;
    }

    /**
     * @return Textbox explaining the actions
     */
    @NonNull
    private JComponent createTextbox()
    {
      JTextArea c = new JTextArea();
      c.setEditable(false);
      c.setText(NbBundle.getMessage(DiffDBToolTip.class, "TXT_Actions"));
      return c;
    }
  }

  /**
   * Icon that renders a custom color
   *
   * @see DiffColors
   */
  @RequiredArgsConstructor
  private static class ToolTipIcon implements Icon
  {
    private static final int WIDTH = 7;
    private static final int HEIGHT = 21;

    @Nullable
    private final Color color;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
      g.setColor(color);
      g.fillRect(x, y, WIDTH, HEIGHT);
    }

    @Override
    public int getIconWidth()
    {
      return WIDTH;
    }

    @Override
    public int getIconHeight()
    {
      return HEIGHT;
    }
  }
}
