package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.core.util.ObjectUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Enthält die Farben die im Diffdialog präsentiert werden.
 * @author t.tasior, 09.03.2018
 */
public final class DiffColors
{
  public final static Color EQUAL = ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.versioning.ignored.color"), new Color(100, 255, 200));
  public final static Color DIFFERENT =  ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.diff.changed.color"), new Color(120, 220, 255));
  public final static Color MISSING =  ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.diff.added.color"), new Color(235, 235, 235));
  public final static Color DELETED = ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.diff.deleted.color"), new Color(255, 120,120));
  public final static Color BACKGROUND = new JButton().getBackground();
}
