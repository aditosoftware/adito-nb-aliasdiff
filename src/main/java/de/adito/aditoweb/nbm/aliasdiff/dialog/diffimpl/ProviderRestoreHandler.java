package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.EDirection;
import de.adito.propertly.core.api.Hierarchy;
import de.adito.propertly.core.spi.IPropertyPitProvider;
import org.jetbrains.annotations.*;

/**
 * Unterstützt das Wiederherstellen auf Providerebene.
 *
 * @author t.tasior, 19.03.2018
 */
class ProviderRestoreHandler
{
  private IPropertyPitProvider createdProvider;
  private EDirection removedDir;
  private IPropertyPitProvider removedProvider;

  

  /**
   * Nimmt einen neu erzeugten Provider entgegen.
   */
  public void providerCreated(@Nullable IPropertyPitProvider pProvider)
  {
    createdProvider = pProvider;
  }

  /**
   * Liefert den vorher erzeugten Provider zurück.
   */
  public IPropertyPitProvider getCreatedProvider()
  {
    IPropertyPitProvider provider = createdProvider;
    createdProvider = null;
    return provider;
  }

  /**
   * Nimmt den Provider entgegen der entfernt wurde.
   * @param pDirection auf welcher Seite er entfernt wurde.
   */
  public void providerToRemove(@NotNull EDirection pDirection, @NotNull IPropertyPitProvider pProvider)
  {
    removedDir = pDirection;
    String name = pProvider.getPit().getOwnProperty().getName();
    removedProvider = new Hierarchy<>(name, pProvider).getValue();
  }

  /**
   * Liefert die Seite auf der der Provider entfernt wurde.
   * @return eine Konstante, oder null.
   */
  public EDirection getRemovedDirection()
  {
    EDirection dir = removedDir;
    removedDir = null;
    return dir;
  }

  /**
   * Liefert den vorher entfernten Provider.
   * @return den Provider oder null.
   */
  public IPropertyPitProvider getRemovedProvider()
  {
    IPropertyPitProvider provider = removedProvider;
    removedProvider = null;
    return provider;
  }
}
