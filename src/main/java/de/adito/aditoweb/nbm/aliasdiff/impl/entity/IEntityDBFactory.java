package de.adito.aditoweb.nbm.aliasdiff.impl.entity;

import de.adito.aditoweb.database.IAliasConfigInfo;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.EntityGroupDBDataModel;
import lombok.NonNull;

import java.util.Set;

/**
 * Factory to create entity structures from database systems
 *
 * @author w.glanzer, 12.07.2022
 */
public interface IEntityDBFactory
{

  /**
   * Creates a new {@link EntityGroupDBDataModel} based on the database of the given config.
   * Will extract all tables!
   *
   * @param pConfig Alias to read from
   * @return the entity model with all tables
   * @throws EntityDBModelCreationException if something failed during creation
   */
  @NonNull
  EntityGroupDBDataModel create(@NonNull IAliasConfigInfo pConfig) throws EntityDBModelCreationException;

  /**
   * Creates a new {@link EntityGroupDBDataModel} based on the database of the given config.
   * Will extract only tables that are identified in the given set.
   *
   * @param pConfig Alias to read from
   * @param pTables Tables that should be read.
   *                If a table does not exist in the database, then it will be ignored and not written in the resulting model
   * @return the entity model with some tables
   * @throws EntityDBModelCreationException if something failed during creation
   */
  @NonNull
  EntityGroupDBDataModel create(@NonNull IAliasConfigInfo pConfig, @NonNull Set<String> pTables) throws EntityDBModelCreationException;

}
