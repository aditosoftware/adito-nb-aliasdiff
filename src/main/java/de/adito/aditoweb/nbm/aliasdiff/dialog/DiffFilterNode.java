package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import org.jetbrains.annotations.*;

import javax.swing.tree.MutableTreeNode;

/**
 * Ergebnis einer Filterung, der Node enthält den originalen Node
 * und reicht diverse Methodenaufrufe an diesen weiter.
 * @author T.Tasior, 27.03.2018
 */
public class DiffFilterNode extends FilterNode implements IDiffNode
{
  private IDiffNode node;

  public DiffFilterNode(MutableTreeNode pNode, ITreeNodeFilter pFilter)
  {
    super(pNode, pFilter);
    node = (IDiffNode) pNode;
  }

  @Override
  public boolean isReadOnly(@NotNull EDirection pDirection)
  {
    return node.isReadOnly(pDirection);
  }

  @Override
  public String getRootName(EDirection pDirection)
  {
    return node.getRootName(pDirection);
  }

  @Override
  public boolean isRemote(@NotNull EDirection pDirection)
  {
    return node.isRemote(pDirection);
  }

  @Override
  public int countDifferences()
  {
    return node.countDifferences();
  }

  @Override
  public void write()
  {
    node.write();
  }

  @Override
  public String nameForDisplay(EDirection pDirection)
  {
    return node.nameForDisplay(pDirection);
  }

  @Override
  public DiffStateCollector collectDiffStates(@Nullable DiffStateCollector pParentCollector)
  {
    return node.collectDiffStates(pParentCollector);
  }

  @Override
  public EDiff getDiff(@NotNull EDirection pDir)
  {
    return node.getDiff(pDir);
  }


  @Override
  public AbstractPair getPair()
  {
    return node.getPair();
  }

  @Override
  public boolean getAllowsChildren()
  {
    return getChildCount()>0;
    
  }
}
