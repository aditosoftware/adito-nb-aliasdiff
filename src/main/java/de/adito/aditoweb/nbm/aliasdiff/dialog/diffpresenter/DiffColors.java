package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import de.adito.aditoweb.core.util.ObjectUtil;
import lombok.*;

import javax.swing.*;
import java.awt.*;

/**
 * Contains the colors that are presented in the diff dialog
 *
 * @author t.tasior, 09.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DiffColors
{
  public static final Color EQUAL = ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.versioning.ignored.color"), new Color(100, 255, 200));
  public static final Color DIFFERENT = ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.diff.changed.color"), new Color(120, 220, 255));
  public static final Color MISSING = ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.diff.added.color"), new Color(235, 235, 235));
  public static final Color DELETED = ObjectUtil.firstNonNullThrow(UIManager.getColor("nb.diff.deleted.color"), new Color(255, 120, 120));
  public static final Color BACKGROUND = new JButton().getBackground();
}
