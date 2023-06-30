package de.adito.aditoweb.nbm.aliasdiff.impl.db;

import de.adito.aditoweb.filesystem.datamodelfs.misc.IContextualAliasConfigResolver;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.project.Project;

/**
 * Stellt AliasConfigResolver bereit
 *
 * @author w.glanzer, 12.07.2022
 */
public interface IAliasConfigResolverProvider
{

  /**
   * Liefert einen AliasConfigResolver für das übergebene Projekt
   *
   * @param pProject Projekt für das der Resolver benötigt wird
   * @return der Resolver der verwendet werden soll
   */
  @Nullable
  IContextualAliasConfigResolver getResolver(@NonNull Project pProject);

}
