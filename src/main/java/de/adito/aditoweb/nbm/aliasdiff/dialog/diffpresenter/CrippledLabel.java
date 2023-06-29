package de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter;

import javax.swing.*;
import java.awt.*;

/**
 * Wird als Renderer für den PropertyNode benutzt.
 * @author t.tasior, 07.03.2018
 */
 class CrippledLabel extends JLabel
{
  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void validate()
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   */
  public void invalidate()
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void revalidate()
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void repaint(long tm, int x, int y, int width, int height)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void repaint(Rectangle r)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   *
   * @since 1.5
   */
  public void repaint()
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
  {
    // Strings get interned...
    if (propertyName == "text"
        || ((propertyName == "font" || propertyName == "foreground")
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
  public void firePropertyChange(String propertyName, byte oldValue, byte newValue)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void firePropertyChange(String propertyName, char oldValue, char newValue)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void firePropertyChange(String propertyName, short oldValue, short newValue)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void firePropertyChange(String propertyName, int oldValue, int newValue)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void firePropertyChange(String propertyName, long oldValue, long newValue)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void firePropertyChange(String propertyName, float oldValue, float newValue)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void firePropertyChange(String propertyName, double oldValue, double newValue)
  {
  }

  /**
   * Overridden for performance reasons.
   * See the <a href="#override">Implementation Note</a>
   * for more information.
   */
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue)
  {
  }
}
