package de.adito.aditoweb.nbm.aliasdiff.actions;

import de.adito.aditoweb.nbm.entitydbeditor.actions.DiffWithDBAction;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 * Starts the "Diff Alias <> DB Tables" Workflow for a whole alias
 *
 * @author w.glanzer, 28.06.2023
 * @see DiffWithDBAction
 */
public class DiffWithDBActionExt extends DiffWithDBAction
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
    return NbBundle.getMessage(DiffWithDBActionExt.class, "NAME_DiffWithDBAction");
  }

}
