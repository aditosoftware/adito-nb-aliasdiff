package de.adito.aditoweb.nbm.aliasdiff.impl.gui;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.IEntityDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.provider.IEntityProvider;
import lombok.NonNull;

import javax.swing.tree.MutableTreeNode;
import java.util.*;

/**
 * Filters the "entities" and "entityFields" nodes out of the hierarchy and takes their children to the upper level.
 * Specifically, the tables move directly below the root, and the columns directly below the respective tables.
 *
 * @author w.glanzer, 14.07.2022
 */
class EntityTreeNodeFilter implements ITreeNodeFilter
{

  @NonNull
  @Override
  public MutableTreeNode filterNode(@NonNull MutableTreeNode pNode)
  {
    return new DiffFilterNode(pNode, this);
  }

  @NonNull
  @Override
  public ArrayList<MutableTreeNode> filterChild(@NonNull MutableTreeNode pChild)
  {
    ArrayList<MutableTreeNode> list = new ArrayList<>();

    String entities = IEntityProvider.entities.getName();
    String entityFields = IEntityDataModel.entityFields.getName();

    if (pChild.toString().equals(entities) || pChild.toString().equals(entityFields))
    {
      for (int i = 0; i < pChild.getChildCount(); i++)
        list.add(new DiffFilterNode((MutableTreeNode) pChild.getChildAt(i), EntityTreeNodeFilter.this));

      // Sort alphabetically, to improve readability
      list.sort(Comparator.comparing(pNode -> {
        if (pNode instanceof DiffFilterNode)
          return ((DiffFilterNode) pNode).nameForDisplay(EDirection.LEFT);
        return pNode.toString();
      }, String.CASE_INSENSITIVE_ORDER));
    }
    else
      list.add(new DiffFilterNode(pChild, EntityTreeNodeFilter.this));

    return list;
  }

}
