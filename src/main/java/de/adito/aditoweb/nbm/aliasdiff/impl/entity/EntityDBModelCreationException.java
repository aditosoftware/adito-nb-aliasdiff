package de.adito.aditoweb.nbm.aliasdiff.impl.entity;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception that is thrown during the {@link IEntityDBFactory} workflow
 *
 * @author w.glanzer, 30.06.2023
 */
public class EntityDBModelCreationException extends Exception
{
  public EntityDBModelCreationException(@Nullable String message, @NonNull Throwable cause)
  {
    super(message, cause);
  }
}
