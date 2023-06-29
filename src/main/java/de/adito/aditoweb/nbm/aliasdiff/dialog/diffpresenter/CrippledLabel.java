package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import javax.swing.*;
import java.awt.*;

/**
 * Used as a renderer for the {@link de.adito.aditoweb.nbm.aliasdiff.dialog.PropertyNode}.
 *
 * @author t.tasior, 07.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
 class CrippledLabel extends JLabel
{
  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void validate()
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   */
  @Override
  public void invalidate()
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void revalidate()
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void repaint(long tm, int x, int y, int width, int height)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void repaint(Rectangle r)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   */
  @Override
  public void repaint()
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
  {
    //noinspection StringEquality Strings get interned...
    if (propertyName == "text" // NOSONAR
        || ((propertyName == "font" || propertyName == "foreground") // NOSONAR
        && oldValue != newValue
        && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null))
    {

      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, byte oldValue, byte newValue)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, char oldValue, char newValue)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, short oldValue, short newValue)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, int oldValue, int newValue)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, long oldValue, long newValue)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, float oldValue, float newValue)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, double oldValue, double newValue)
  {
    // performance
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  @Override
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue)
  {
    // performance
  }
}
