package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.nbm.aliasdiff.dialog.PropertyNode;

import javax.swing.*;
import java.awt.*;

/**
 * Symbolizes the value differences of a {@link PropertyNode} in color.
 *
 * @author t.tasior, 07.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public class DiffIcon implements Icon
{
  private static final int Y = 2;
  private static final int W = 7;
  private static final int H = 17;
  private static final int WIDTH = W * 4;
  private static final int HEIGHT = 21;

  private Color equalColor;
  private Color differentColor;
  private Color missingColor;
  private Color deletedColor;

  @Override
  public void paintIcon(Component c, Graphics g, int pX, int pY)
  {
    int currX = 0;
    if (deletedColor != null)
    {
      g.setColor(deletedColor);
      g.fillRect(currX, Y, W, H);
    }

    currX += W;
    if (missingColor != null)
    {
      g.setColor(missingColor);
      g.fillRect(currX, Y, W, H);
    }

    currX += W;
    if (differentColor != null)
    {
      g.setColor(differentColor);
      g.fillRect(currX, Y, W, H);
    }

    currX += W;
    if (equalColor != null)
    {
      g.setColor(equalColor);
      g.fillRect(currX, Y, W, H);
    }
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

  /**
   * Resets all currently set states
   */
  public void reset()
  {
    equalColor = null;
    differentColor = null;
    missingColor = null;
    deletedColor = null;
  }

  /**
   * Sets, that the icon should display an EQUAL state
   */
  public void setEqualColor()
  {
    equalColor = DiffColors.EQUAL;
  }

  /**
   * Sets, that the icon should display a DIFFERENT state
   */
  public void setDifferentColor()
  {
    differentColor = DiffColors.DIFFERENT;
  }

  /**
   * Sets, that the icon should display a MISSING state
   */
  public void setMissingColor()
  {
    missingColor = DiffColors.MISSING;
  }

  /**
   * Sets, that the icon should display a DELETED state
   */
  public void setDeletedColor()
  {
    deletedColor = DiffColors.DELETED;
  }
}
