package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import de.adito.propertly.core.spi.IHierarchy;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.*;
import java.util.*;

/**
 * Result of filtering, the node contains the original
 * node and passes various method calls to it.
 *
 * @author T.Tasior, 27.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public class DiffFilterNode extends DefaultMutableTreeNode implements IDiffNode
{
  private final transient IDiffNode node;

  public DiffFilterNode(@NonNull MutableTreeNode pNode, @NonNull ITreeNodeFilter pFilter)
  {
    super(pNode);
    node = (IDiffNode) pNode;

    setUserObject(pNode);

    ArrayList<MutableTreeNode> collector = new ArrayList<>();
    for (int i = 0; i < pNode.getChildCount(); i++)
    {
      MutableTreeNode child = (MutableTreeNode) pNode.getChildAt(i);
      List<MutableTreeNode> children = pFilter.filterChild(child);
      collector.addAll(children);
    }

    for (MutableTreeNode child : collector)
      add(child);
  }

  @Override
  public boolean isReadOnly(@NonNull EDirection pDirection)
  {
    return node.isReadOnly(pDirection);
  }

  @NonNull
  @Override
  public String getRootName(@NonNull EDirection pDirection)
  {
    return node.getRootName(pDirection);
  }

  @Override
  public boolean isRemote(@NonNull EDirection pDirection)
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

  @Nullable
  @Override
  public String nameForDisplay(@NonNull EDirection pDirection)
  {
    return node.nameForDisplay(pDirection);
  }

  @NonNull
  @Override
  public DiffStateCollector collectDiffStates(@Nullable DiffStateCollector pParentCollector)
  {
    return node.collectDiffStates(pParentCollector);
  }

  @NonNull
  @Override
  public EDiff getDiff(@NonNull EDirection pDirection)
  {
    return node.getDiff(pDirection);
  }

  @Nullable
  @Override
  public IHierarchy<?> getHierarchy(@NonNull EDirection pDirection)
  {
    return node.getHierarchy(pDirection);
  }

  @NonNull
  @Override
  public AbstractPair getPair()
  {
    return node.getPair();
  }

  @Override
  public boolean getAllowsChildren()
  {
    return getChildCount() > 0;
  }
}
