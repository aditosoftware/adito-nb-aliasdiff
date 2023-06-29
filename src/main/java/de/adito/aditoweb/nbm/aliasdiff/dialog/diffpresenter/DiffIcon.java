package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import javax.swing.*;
import java.awt.*;

/**
 * Symbolisiert farblich die Wertunterschiede eines PropertyNode.
 *
 * @author t.tasior, 07.03.2018
 */
public class DiffIcon implements Icon
{
  private static final int y = 2;
  private static final int w = 7;
  private static final int h = 17;
  private static final int width = w * 4;
  private static final int height = 21;

  public Color equal_Color;
  public Color different_Color;
  public Color missing_Color;
  public Color deleted_Color;


  public void reset()
  {
    equal_Color = null;
    different_Color = null;
    missing_Color = null;
    deleted_Color = null;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int pX, int pY)
  {

    int _x = 0;
    if (deleted_Color != null)
    {
      g.setColor(deleted_Color);
      g.fillRect(_x, y, w, h);
    }

    _x += w;
    if (missing_Color != null)
    {
      g.setColor(missing_Color);
      g.fillRect(_x, y, w, h);
    }

    _x += w;
    if (different_Color != null)
    {
      g.setColor(different_Color);
      g.fillRect(_x, y, w, h);
    }

    _x += w;
    if (equal_Color != null)
    {
      g.setColor(equal_Color);
      g.fillRect(_x, y, w, h);
    }
  }

  public void setEqualColor()
  {
    equal_Color = DiffColors.EQUAL;
  }

  public void setDifferentColor()
  {
    different_Color = DiffColors.DIFFERENT;
  }

  public void setMissingColor()
  {
    missing_Color = DiffColors.MISSING;
  }

  public void setDeletedColor()
  {
    deleted_Color = DiffColors.DELETED;
  }

  @Override
  public int getIconWidth()
  {
    return width;
  }

  @Override
  public int getIconHeight()
  {
    return height;
  }
}
