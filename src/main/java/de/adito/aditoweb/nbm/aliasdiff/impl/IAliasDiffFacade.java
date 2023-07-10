package de.adito.aditoweb.nbm.aliasdiff.impl;

import de.adito.aditoweb.designer.dataobjects.data.db.*;
import lombok.NonNull;

import java.util.Set;

/**
 * Facade to start alias definition diff actions
 *
 * @author w.glanzer, 12.07.2022
 */
public interface IAliasDiffFacade
{

  /**
   * Executes the diff action on the whole given alias
   *
   * @param pEntityGroup Group to extract all tables from
   */
  void executeDatabaseDiffWithGUI(@NonNull IEntityGroupDBDataObject pEntityGroup);

  /**
   * Executes the diff action on the given (remote) db tables
   *
   * @param pEntityGroup      Group to select the correct alias to diff
   * @param pTableDataObjects tables that should be compared
   */
  void executeDatabaseDiffWithGUI(@NonNull IEntityGroupDBDataObject pEntityGroup, @NonNull Set<IEntityDBDataObject<?>> pTableDataObjects);

}
