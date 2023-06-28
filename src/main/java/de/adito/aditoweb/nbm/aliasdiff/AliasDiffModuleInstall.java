package de.adito.aditoweb.nbm.aliasdiff;

import de.adito.aditoweb.nbm.aliasdiff.actions.*;
import de.adito.aditoweb.nbm.entitydbeditor.actions.*;
import org.openide.modules.ModuleInstall;

import static de.adito.aditoweb.nbm.aliasdiff.util.ModuleSystemUtils.*;

/**
 * ModuleInstall of our module to provide startup functionality
 * during module validation and restore
 *
 * @author w.glanzer, 28.06.2023
 */
@SuppressWarnings("unused") // manifest.mf
public class AliasDiffModuleInstall extends ModuleInstall
{

  @Override
  public void validate() throws IllegalStateException
  {
    // add the export of the given package as early as possible to prevent any ClassNotFoundErrors
    addModuleExport("de.adito.designer.netbeans.EntityDBEditor", "de.adito.aditoweb.nbm.entitydbeditor.actions");
  }

  @Override
  public void restored()
  {
    // Replace the current actions with our ones
    replaceSharedObject(DiffWithDBTablesAction.class, DiffWithDBTablesActionExt.class);
    replaceSharedObject(DiffWithDBAction.class, DiffWithDBActionExt.class);
  }

}
