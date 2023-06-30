package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.filesystem.propertly.BulkModifyHierarchy;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import de.adito.aditoweb.system.crmcomponents.IDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.EntityFieldDBDataModel;
import de.adito.propertly.core.spi.*;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.sql.Types;
import java.util.*;


/**
 * Link between the display in the JTree and the internal mapping of the data model structure (AbstractPair Impl).
 * Provides access management methods for the AbstractPair implementation in addition to the TreeNode implementation.
 *
 * @author t.tasior, 01.02.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 */
public class PropertyNode extends DefaultMutableTreeNode implements IDiffNode
{
  // Data types whose size / scale is fixed by the database and is not determined by the user
  private static final Set<Integer> NON_MANAGED_DATA_TYPES = Set.of(Types.BLOB, Types.DATE, Types.TIMESTAMP, Types.INTEGER, Types.BIGINT, Types.DOUBLE,
                                                                    Types.BIT, Types.BOOLEAN, Types.FLOAT, Types.SMALLINT, Types.REAL, Types.TINYINT,
                                                                    Types.TIMESTAMP_WITH_TIMEZONE);
  private BulkModifyHierarchy<?> leftModify; //NOSONAR
  @Nullable
  private EDirection remote;
  private BulkModifyHierarchy<?> rightModify; //NOSONAR
  private EDirection readOnly;
  private final AbstractPair propPair;

  private final EnumSet<EDiff> set = EnumSet.of(EDiff.DIFFERENT, EDiff.MISSING, EDiff.DELETED);
  private final DiffStateCollector myCollector = new DiffStateCollector();


  /**
   * This constructor will be called by every root node
   *
   * @param pLeftModify  left model ald BulkModifyHierarchy
   * @param pRemote      see AbstractPropertyPitMatcher.
   * @param pRightModify right model ald BulkModifyHierarchy
   * @param pReadOnly    see AbstractPropertyPitMatcher.
   * @see PropertyPitMatcher
   */
  public PropertyNode(@Nullable BulkModifyHierarchy<?> pLeftModify, @Nullable EDirection pRemote,
                      @Nullable BulkModifyHierarchy<?> pRightModify, @Nullable EDirection pReadOnly)
  {
    leftModify = pLeftModify;
    remote = pRemote;
    rightModify = pRightModify;
    readOnly = pReadOnly;
    propPair = new ProviderPair(this);
  }

  /**
   * Accepts an IPropertyPitProvider implementation (data model) from one of the two BulkModifyHierarchy objects.
   *
   * @param pDirection the left or the right hierarchy
   * @param pProvider  the model, or null
   */
  public PropertyNode(@NonNull EDirection pDirection, @Nullable IPropertyPitProvider<?, ?, ?> pProvider)
  {
    propPair = new ProviderPair(this);
    propPair.setProvider(pDirection, pProvider);
  }

  /**
   * Accepts an IProperty (single value) from one of the two BulkModifyHierarchy objects or one of its sub-data models.
   *
   * @param pDirection the left or the right hierarchy
   * @param pProp      property or null
   */
  public PropertyNode(@NonNull EDirection pDirection, @Nullable IProperty<?, ?> pProp)
  {
    propPair = new PropertyPair(this);
    propPair.setProperty(pDirection, pProp);
  }

  @NonNull
  @Override
  public String getRootName(@NonNull EDirection pDirection)
  {
    if (leftModify != null && rightModify != null)
    {
      if (pDirection == EDirection.LEFT)
        return ((IDataModel<?, ?>) leftModify.getValue()).getName();

      if (pDirection == EDirection.RIGHT)
        return ((IDataModel<?, ?>) rightModify.getValue()).getName();

    }

    return "This is not Root!";
  }

  @Override
  public boolean isRemote(@NonNull EDirection pDirection)
  {
    return pDirection.equals(remote);
  }

  @Override
  public boolean isReadOnly(@NonNull EDirection pDirection)
  {
    return pDirection.equals(readOnly);
  }

  @Override
  public void write()
  {
    if (readOnly == EDirection.RIGHT)
      leftModify.writeBack();
    else if (readOnly == EDirection.LEFT)
      rightModify.writeBack();
    else
    {
      leftModify.writeBack();
      rightModify.writeBack();
    }
  }

  @NonNull
  @Override
  public EDiff getDiff(@NonNull EDirection pDirection)
  {
    return propPair.typeOfDiff(pDirection);
  }

  @NonNull
  @Override
  public AbstractPair getPair()
  {
    return propPair;
  }

  @Nullable
  @Override
  public String nameForDisplay(@NonNull EDirection pDirection)
  {
    return propPair.nameForDisplay(pDirection);
  }

  @Override
  public int countDifferences()
  {
    int count = 0;
    for (PropertyNode node : getChildren())
    {
      AbstractPair pair = node.getPair();
      if (isDifferent(pair))
        count++;
      count += node.countDifferences();
    }

    return count;
  }

  @NonNull
  @Override
  public DiffStateCollector collectDiffStates(@Nullable DiffStateCollector pParentCollector)
  {
    final DiffStateCollector theCollector;

    if (pParentCollector == null)
    {
      theCollector = myCollector;
      myCollector.reset();
    }
    else
      theCollector = pParentCollector;

    AbstractPair pair = getPair();

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.EQUAL || pair.typeOfDiff(EDirection.RIGHT) == EDiff.EQUAL)
      theCollector.equal = EDiff.EQUAL;

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.DIFFERENT || pair.typeOfDiff(EDirection.RIGHT) == EDiff.DIFFERENT)
      theCollector.different = EDiff.DIFFERENT;

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.MISSING || pair.typeOfDiff(EDirection.RIGHT) == EDiff.MISSING)
      theCollector.missing = EDiff.MISSING;

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.DELETED || pair.typeOfDiff(EDirection.RIGHT) == EDiff.DELETED)
      theCollector.deleted = EDiff.DELETED;

    for (PropertyNode node : getChildren())
      node.collectDiffStates(theCollector);

    return theCollector;
  }

  @Override
  public String toString()
  {
    if (propPair != null)
      return propPair.nameForIdentification();
    return "__Root__";
  }

  /**
   * Schedules an update of all children with the given direction
   *
   * @param pDirection Specifies in which of the compared data models a value is written.
   */
  public void postUpdateDown(@NonNull EDirection pDirection)
  {
    for (PropertyNode child : getChildren())
      child.getPair().update(pDirection);
  }

  /**
   * Schedules that all children should be stored to its defaults
   */
  public void restoreDown()
  {
    for (PropertyNode child : getChildren())
      child.getPair().restore();
  }

  /**
   * An object should be created and appended into the parent object
   *
   * @param pDirection determines where the object should be created
   * @param pParent    a data model to which a new value (object) is set
   */
  public void createDown(@NonNull EDirection pDirection, @NonNull IPropertyPitProvider<?, ?, ?> pParent)
  {
    for (PropertyNode node : getChildren())
      node.getPair().createDown(pDirection, pParent);
  }

  /**
   * A node fires, that the children should be deleted
   *
   * @param pDirection direction where the children should be deleted
   */
  public void deleteDown(@NonNull EDirection pDirection)
  {
    for (PropertyNode node : getChildren())
      node.getPair().deleteDown(pDirection);
  }

  /**
   * Adds the property as an opposite partner of an existing property,
   * or creates a new PropertyNode as a child of this node.
   *
   * @param pDirection side to add
   * @param pProp      a simple value that should be added
   */
  public void addProperty(@NonNull EDirection pDirection, @NonNull IProperty<?, ?> pProp)
  {
    boolean needsNewNode = true;
    for (PropertyNode node : getChildren())
    {
      AbstractPair nodePair = node.propPair;
      if (nodePair.containsProperty(pProp))
      {
        nodePair.setProperty(pDirection, pProp);
        needsNewNode = false;
        break;
      }
    }

    if (needsNewNode)
      add(new PropertyNode(pDirection, pProp));
  }

  /**
   * Adds the provider as an opposing partner of an existing provider,
   * or creates a new PropertyNode as a child of this node.
   *
   * @param pDirection side to add
   * @param pProvider  Provider to add
   * @return the newly added node
   */
  @NonNull
  public PropertyNode addProvider(EDirection pDirection, IPropertyPitProvider<?, ?, ?> pProvider)
  {
    for (PropertyNode node : getChildren())
    {
      AbstractPair nodePair = node.propPair;
      if (nodePair.containsProvider(pProvider))
      {
        nodePair.setProvider(pDirection, pProvider);
        return node;
      }
    }

    PropertyNode node = new PropertyNode(pDirection, pProvider);
    add(node);
    return node;
  }

  /**
   * Compares all objects for equality and removes them from the tree structure.
   * What remains are data models and values that are different.
   */
  public void buildDiff()
  {
    for (PropertyNode child : new ArrayList<>(getChildren()))
      child.buildDiff();

    Object managedObject = getPair().getManagedObject(EDirection.RIGHT);
    if (managedObject instanceof EntityFieldDBDataModel &&
        getChildren().stream().noneMatch(pChild -> pChild.toString().equals("columnType")) &&
        NON_MANAGED_DATA_TYPES.contains(((EntityFieldDBDataModel) managedObject).getColumnType()))
    {
      List<PropertyNode> childNodes = getChildren();
      for (PropertyNode treeNode : childNodes)
      {
        String nodeName = treeNode.getPair().nameForIdentification();
        if (nodeName.equalsIgnoreCase("size") || nodeName.equalsIgnoreCase("scale"))
          remove(treeNode);
      }
    }

    if (propPair.isEqual() && (parent != null) && (getChildren().isEmpty()))
      parent.remove(this);
  }

  /**
   * @return our parent, or null if we are the root node
   */
  @Nullable
  public PropertyNode parent()
  {
    return (PropertyNode) getParent();
  }

  /**
   * Reorder all children, so they remain in alphabetical order
   */
  void reorder()
  {
    getChildren().sort(Comparator.comparing(pO -> pO.getPair().nameForIdentification().toUpperCase()));
    getChildren().forEach(PropertyNode::reorder);
  }

  /**
   * @return all of our children
   */
  @NonNull
  private List<PropertyNode> getChildren()
  {
    //noinspection unchecked,rawtypes
    return (List) Collections.list(children());
  }

  /**
   * Determines, if the given pair is in a "different" state
   *
   * @param pPair Pair to check
   * @return true, if it is in a "different" state
   */
  private boolean isDifferent(@Nullable AbstractPair pPair)
  {
    if (pPair != null)
      return set.contains(pPair.typeOfDiff(EDirection.LEFT)) ||
          set.contains(pPair.typeOfDiff(EDirection.RIGHT));

    return false;
  }

}
