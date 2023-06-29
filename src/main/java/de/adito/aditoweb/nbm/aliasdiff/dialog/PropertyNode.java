package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.filesystem.propertly.BulkModifyHierarchy;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import de.adito.aditoweb.system.crmcomponents.IDataModel;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.EntityFieldDBDataModel;
import de.adito.propertly.core.spi.*;
import org.jetbrains.annotations.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.sql.Types;
import java.util.*;


/**
 * Bindeglied zwischen der Anzeige im JTree und der internen
 * Abbildung der Datenmodellstruktur (AbstractPair Impl).
 * Bietet neben der TreeNode Implementierung Zugriffs/Verwaltungsmethoden
 * für die AbstractPair Implementierung.
 *
 * @author t.tasior, 01.02.2018
 */
public class PropertyNode extends DefaultMutableTreeNode implements IDiffNode
{
  // Data types, deren size/scale fest von der Datenbank vorgegeben ist und nicht vom Benutzer bestimmt wird
  private static final Set<Integer> NON_MANAGED_DATA_TYPES = Set.of(Types.BLOB, Types.DATE, Types.TIMESTAMP, Types.INTEGER, Types.BIGINT, Types.DOUBLE,
                                                                    Types.BIT, Types.BOOLEAN, Types.FLOAT, Types.SMALLINT, Types.REAL, Types.TINYINT,
                                                                    Types.TIMESTAMP_WITH_TIMEZONE);
  private BulkModifyHierarchy<?> leftModify;
  @Nullable
  private EDirection remote;
  private BulkModifyHierarchy<?> rightModify;
  private EDirection readOnly;
  private final AbstractPair propPair;

  private final EnumSet<EDiff> set = EnumSet.of(EDiff.DIFFERENT, EDiff.MISSING, EDiff.DELETED);
  private final DiffStateCollector myCollector = new DiffStateCollector();
  private final List<PropertyNode> differences = new ArrayList<>();
  private int listIndex = -1;
  private int childIndex = -1;
  private static final int PREVIOUS = -1;
  private static final int NEXT = 1;
  private int actualDirection = 0;


  /**
   * Dieser Konstruktor wird für den Wurzelknoten aufgerufen.
   *
   * @param pLeftModify  das linke Datenmodell als BulkModifyHierarchy.
   * @param pRemote      siehe AbstractPropertyPitMatcher.
   * @param pRightModify das rechte Datenmodell als BulkModifyHierarchy.
   * @param pReadOnly    siehe AbstractPropertyPitMatcher.
   * @see AbstractPropertyPitMatcher
   */
  public PropertyNode(BulkModifyHierarchy<?> pLeftModify,
                      @Nullable EDirection pRemote,
                      BulkModifyHierarchy<?> pRightModify, EDirection pReadOnly)
  {
    leftModify = pLeftModify;
    remote = pRemote;
    rightModify = pRightModify;
    readOnly = pReadOnly;
    propPair = new ProviderPair(this);
  }

  /**
   * Nimmt eine IPropertyPitProvider Implementierung (Datenmodell) aus
   * einem der beiden BulkModifyHierarchy Objekten entgegen.
   *
   * @param pDirection die linke oder rechte Hierarchy
   * @param pProvider  das Datenmodell, oder null.
   */
  public PropertyNode(@NotNull EDirection pDirection, IPropertyPitProvider<?, ?, ?> pProvider)
  {
    propPair = new ProviderPair(this);
    propPair.setProvider(pDirection, pProvider);
  }

  /**
   * Nimmt eine IProperty (einfacher Wert) aus
   * einem der beiden BulkModifyHierarchy Objekten
   * oder seinen Sub-Datenmodellen entgegen.
   *
   * @param pDirection pDirection die linke oder rechte Hierarchy
   *                   bzw. Sub-Datenmodell
   * @param pProp      beispielsweise ein Integer Property, oder auch null
   */
  public PropertyNode(@NotNull EDirection pDirection, IProperty<?, ?> pProp)
  {
    propPair = new PropertyPair(this);
    propPair.setProperty(pDirection, pProp);
  }


  public String getRootName(EDirection pDirection)
  {
    if ((leftModify != null) && (rightModify != null))
    {
      if (pDirection == EDirection.LEFT)
        return ((IDataModel<?, ?>) leftModify.getValue()).getName();

      if (pDirection == EDirection.RIGHT)
        return ((IDataModel<?, ?>) rightModify.getValue()).getName();

    }
    return "This is not Root!";
  }

  public boolean isRemote(@NotNull EDirection pDirection)
  {
    return pDirection.equals(remote);
  }


  public boolean isReadOnly(@NotNull EDirection pDirection)
  {
    return pDirection.equals(readOnly);
  }

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

  private List<PropertyNode> _getChildren()
  {
    return (List) Collections.list(children());
  }

  /**
   * Fügt das Property als gegenüberliegenden Partner eines bestehenden
   * Property hinzu, oder legt einen neuen PropertyNode als Kind dieses Nodes an.
   *
   * @param pDirection gibt an auf welcher Seite eingefügt werden soll.
   * @param pProp      ein einfacher Wert der eingefügt wird.
   */
  public void addProperty(EDirection pDirection, IProperty<?, ?> pProp)
  {
    boolean needsNewNode = true;
    for (PropertyNode node : _getChildren())
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
   * Fügt den Provider als gegenüberliegenden Partner eines bestehenden
   * Providers hinzu, oder legt einen neuen PropertyNode als Kind dieses Nodes an.
   *
   * @param pDirection gibt an auf welcher Seite eingefügt werden soll.
   * @param pProvider  dieser Provider (Datenmodell) wird eingefügt.
   * @return einen Node um den Baum weiter aufzubauen.
   */
  public PropertyNode addProvider(EDirection pDirection, IPropertyPitProvider<?, ?, ?> pProvider)
  {
    for (PropertyNode node : _getChildren())
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
    //_install(node);

    return node;
  }


  public EDiff getDiff(@NotNull EDirection pDir)
  {
    return propPair.typeOfDiff(pDir);
  }

  public AbstractPair getPair()
  {
    return propPair;
  }

  /**
   * Leitet die Parameter an die gleichnamige Methode von AbstractPair weiter.
   *
   * @param pDirection betrifft die linke oder rechte Seite.
   * @param pProvider  ein Datenmodell.
   * @see AbstractPair#createDown(EDirection, IPropertyPitProvider)
   */
  public void createDown(@NotNull EDirection pDirection, IPropertyPitProvider<?, ?, ?> pProvider)
  {
    for (PropertyNode node : _getChildren())
    {
      node.getPair().createDown(pDirection, pProvider);
    }
  }

  /**
   * Leitet den Parameter an die gleichnamige Methode von AbstractPair weiter.
   *
   * @param pDirection betrifft die linke oder rechte Seite.
   * @see AbstractPair#deleteDown(EDirection)
   */
  public void deleteDown(@NotNull EDirection pDirection)
  {
    for (PropertyNode node : _getChildren())
    {
      node.getPair().deleteDown(pDirection);
    }
  }


  public String nameForDisplay(EDirection pDirection)
  {
    return propPair.nameForDisplay(pDirection);
  }

  /**
   * Vergleicht alle Objekte auf Gleichheit und entfernt diese
   * aus der Baumstruktur. Übrig bleiben Datenmodelle und Werte die
   * different sind.
   */
  @SuppressWarnings({"squid:S1066"}) // Drei Bedingungen in einem if sind nicht übersichtlicher
  public void buildDiff()
  {
    for (PropertyNode child : new ArrayList<>(_getChildren()))
    {
      child.buildDiff();
    }
    Object managedObject = getPair().getManagedObject(EDirection.RIGHT);
    if (managedObject instanceof EntityFieldDBDataModel)
    {
      if (_getChildren().stream().noneMatch(pChild -> pChild.toString().equals("columnType"))
          && NON_MANAGED_DATA_TYPES.contains(((EntityFieldDBDataModel) managedObject).getColumnType()))
      {
        List<PropertyNode> childNodes = _getChildren();
        for (PropertyNode treeNode : childNodes)
        {
          String nodeName = treeNode.getPair().nameForIdentification();
          if (nodeName.equalsIgnoreCase("size") || nodeName.equalsIgnoreCase("scale"))
          {
            remove(treeNode);
          }
        }
      }
    }

    if (propPair.isEqual() && (parent != null) && (_getChildren().isEmpty()))
    {
      parent.remove(this);
    }
  }


  /**
   * Alphabetische Sortierung.
   */
  void reorder()
  {
    _getChildren().sort(Comparator.comparing(pO -> pO.getPair().nameForIdentification().toUpperCase()));
    _getChildren().forEach(PropertyNode::reorder);
  }

  public int countDifferences()
  {
    int count = 0;
    for (PropertyNode node : _getChildren())
    {
      AbstractPair pair = node.getPair();
      if (_isDifferent(pair))
        count++;
      count += node.countDifferences();
    }

    return count;
  }

  public DiffStateCollector collectDiffStates(@Nullable DiffStateCollector pParentCollector)
  {
    final DiffStateCollector theCollector;

    if (pParentCollector == null)
      theCollector = myCollector.reset();
    else
      theCollector = pParentCollector;

    AbstractPair pair = getPair();

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.EQUAL
        | pair.typeOfDiff(EDirection.RIGHT) == EDiff.EQUAL)
    {
      theCollector.equal = EDiff.EQUAL;
    }

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.DIFFERENT
        | pair.typeOfDiff(EDirection.RIGHT) == EDiff.DIFFERENT)
    {
      theCollector.different = EDiff.DIFFERENT;
    }

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.MISSING
        | pair.typeOfDiff(EDirection.RIGHT) == EDiff.MISSING)
    {
      theCollector.missing = EDiff.MISSING;
    }

    if (pair.typeOfDiff(EDirection.LEFT) == EDiff.DELETED
        | pair.typeOfDiff(EDirection.RIGHT) == EDiff.DELETED)
    {
      theCollector.deleted = EDiff.DELETED;
    }

    for (PropertyNode node : _getChildren())
    {
      node.collectDiffStates(theCollector);
    }

    return theCollector;
  }


  private boolean _isDifferent(AbstractPair pPair)
  {
    if (pPair != null)
      return set.contains(pPair.typeOfDiff(EDirection.LEFT)) | set.contains(pPair.typeOfDiff(EDirection.RIGHT));

    return false;
  }

  //wird in der nächsten Iteration umgebaut
  protected List<PropertyNode> collectDifferentNodes()
  {
    List<PropertyNode> helper = new ArrayList<>();
    for (PropertyNode child : _getChildren())
    {
      if (_isDifferent(child.getPair()))
        helper.add(child);

      helper.addAll(child.collectDifferentNodes());

    }

    return helper;
  }

  private int _getIndexByChild(IDiffNode pStart, List<PropertyNode> pChildNodes, int pFallback)
  {
    if (pStart != null)
    {
      int index = pChildNodes.indexOf(pStart);
      return Math.max(index, 0);

    }
    return pFallback;
  }

  //wird in der nächsten Iteration umgebaut
  public PropertyNode previous(IDiffNode pChild, List<IDiffNode> pToClose)
  {
    if ((actualDirection == NEXT) & (pChild == null))
    {
      listIndex -= 2;
    }

    actualDirection = PREVIOUS;

    if (!differences.isEmpty() && _indexInRange() && pChild == null)
    {
      return differences.get(listIndex--);
    }

    listIndex = -1;
    childIndex--;
    pToClose.clear();
    pToClose.addAll(differences);

    List<PropertyNode> childNodes = _getChildren();
    if (pChild != null)
      childIndex = _getIndexByChild(pChild, childNodes, childIndex) - 1;

    if ((childIndex >= childNodes.size()) | (childIndex < 0))
      childIndex = childNodes.size() - 1;

    for (int i = childIndex; i >= 0; i--)
    {
      childIndex = i;
      List<PropertyNode> list = childNodes.get(i).collectDifferentNodes();
      if (!list.isEmpty())
      {
        differences.clear();
        differences.addAll(list);
        listIndex = differences.size() - 1;
        // childIndex = i;
        break;
      }
    }

    if (!differences.isEmpty() && _indexInRange())
    {
      return differences.get(listIndex--);
    }

    return null;
  }

  //wird in der nächsten Iteration umgebaut
  public PropertyNode next(IDiffNode pChild, List<IDiffNode> pToClose)
  {
    if (actualDirection == PREVIOUS & pChild == null)
    {
      listIndex += 2;
    }

    actualDirection = NEXT;

    if (!differences.isEmpty() && _indexInRange() && pChild == null)
    {
      return differences.get(listIndex++);
    }

    listIndex = -1;
    childIndex++;
    pToClose.clear();
    pToClose.addAll(differences);

    List<PropertyNode> childNodes = _getChildren();
    if (pChild != null)
      childIndex = _getIndexByChild(pChild, childNodes, childIndex);

    if (childIndex >= childNodes.size() | childIndex < 0)
      childIndex = 0;

    for (int i = childIndex, n = childNodes.size(); i < n; i++)
    {
      childIndex = i;
      List<PropertyNode> list = childNodes.get(i).collectDifferentNodes();
      if (!list.isEmpty())
      {
        differences.clear();
        differences.addAll(list);
        listIndex = 0;
        //childIndex = i;
        break;
      }
    }
    if (!differences.isEmpty() && _indexInRange())
    {
      return differences.get(listIndex++);
    }

    return null;
  }


  private boolean _indexInRange()
  {
    if (listIndex < 0)
      return false;

    return listIndex < differences.size();
  }

  public void debugPrint(String pSpace)
  {
    System.err.println(pSpace + propPair.nameForDebugPrint());

    for (PropertyNode child : _getChildren())
    {
      child.debugPrint(pSpace + "   ");
    }

  }


  public PropertyNode parent()
  {
    return (PropertyNode) getParent();
  }


  @Override
  public String toString()
  {
    if (propPair != null)
    {
      return propPair.nameForIdentification();
    }
    return "__Root__";
  }

  public void postUpdateDown(EDirection pDirection)
  {
    for (PropertyNode child : _getChildren())
    {
      child.getPair().update(pDirection);
    }
  }

  public void restoreDown()
  {
    for (PropertyNode child : _getChildren())
    {
      child.getPair().restore();
    }
  }
}
