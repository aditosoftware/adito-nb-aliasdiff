package de.adito.aditoweb.nbm.aliasdiff.actions;

import de.adito.aditoweb.nbm.entitydbeditor.actions.DiffWithDBTablesAction;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 * Starts the "Diff Alias <> DB Tables" Workflow for specific tables
 *
 * @author w.glanzer, 28.06.2023
 * @see DiffWithDBTablesAction
 */
public class DiffWithDBTablesActionExt extends DiffWithDBTablesAction
{

  private final DiffWithDBActionExecutor executor = new DiffWithDBActionExecutor();

  @Override
  protected void performAction(Node[] pActivatedNodes)
  {
    executor.performAction(pActivatedNodes);
  }

  @Override
  public String getName()
  {
    return NbBundle.getMessage(DiffWithDBTablesActionExt.class, "NAME_DiffWithDBTablesAction");
  }

}
