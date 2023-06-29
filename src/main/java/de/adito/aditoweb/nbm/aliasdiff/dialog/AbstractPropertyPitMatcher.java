package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.filesystem.propertly.BulkModifyHierarchy;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.DefaultPropertyFilter;
import de.adito.aditoweb.system.crmcomponents.annotations.DIFF;
import de.adito.propertly.core.common.path.PropertyPath;
import de.adito.propertly.core.spi.*;
import org.jetbrains.annotations.*;

import java.util.List;

/**
 * Nimmt zwei Datenmodelle (IPropertyPitProvider) gleichen Typs entgegen und baut aus deren
 * Struktur einen Baum. Auf den Baum wird der Vergleich ausgeführt und anschliessend
 * die gleichen Elemente (Knoten) herausgefiltert.
 *
 * Der resultierende Baum kann in einer GUI angezeigt werden.
 * Um die Datenmodelle dort unterscheiden zu können, werden sie in "Links" und "Rechts" unterteilt.
 *
 * @author t.tasior, 30.01.2018
 * @see EDirection
 * @see DiffPresenter
 */
public abstract class AbstractPropertyPitMatcher<L extends IPropertyPitProvider<?, ?, ?>, R extends IPropertyPitProvider<?, ?, ?>>
{
  private final IPropertyPit leftPit;
  private final IPropertyFilter filter;
  private final IPropertyPit rightPit;
  private final EDirection readOnly;
  private final BulkModifyHierarchy<?> leftbmh;
  @Nullable
  private final EDirection remote;
  private final BulkModifyHierarchy<?> rightbmh;

  /**
   * Initialisierung mit den Datenmodellen die verglichen werden sollen,
   * sowie weiteren Informationen die für die Darstellung des Baumes relevant sind.
   *
   * @param pLeftPPP  Datenmodell das links im Baum angezeigt wird.
   * @param pFilter   filtert Objekte heraus die nicht verglichen werden sollen.
   *                  Wird null übergeben, kommt ein Filter zur Anwendung der nur
   *                  Objekte durchlässt die die Annotation @DIFF besitzen.
   * @param pRemote   falls angegeben, ist das Datenmodell auf dieser Seite
   *                  nicht lokal im Projekt des Designers verfügbar,
   *                  z.B.: Datenbanktabellen.
   * @param pRightPPP Datenmodell das rechts im Baum angezeigt wird.
   * @param pReadOnly falls angegeben, kann das Datenmodell auf dieser Seite
   *                  nur gelesen werden, aber keine Werte übernehmen. 
   * @see DIFF
   */

  public AbstractPropertyPitMatcher(@NotNull L pLeftPPP, IPropertyFilter pFilter,
                                    @Nullable EDirection pRemote,
                                    @NotNull R pRightPPP,
                                    @Nullable EDirection pReadOnly)
  {
    leftbmh = new BulkModifyHierarchy(pLeftPPP.getPit().getHierarchy());
    leftPit = ((IPropertyPitProvider) new PropertyPath(pLeftPPP).find(leftbmh).getValue()).getPit();

    filter = (pFilter != null) ? pFilter : new DefaultPropertyFilter();
    remote = pRemote;

    rightbmh = new BulkModifyHierarchy(pRightPPP.getPit().getHierarchy());
    rightPit = ((IPropertyPitProvider) new PropertyPath(pRightPPP).find(rightbmh).getValue()).getPit();
    readOnly = pReadOnly;
  }

  private boolean _isOfSameType()
  {
    Class<?> leftType = leftPit.getOwnProperty().getType();
    Class<?> rightType = rightPit.getOwnProperty().getType();
    return leftType.equals(rightType);
  }

  /**
   * Baut den Baum auf, sortiert und filtert diesen.
   * @return eine Baumstruktur, die mithilfe eines JTree angezeigt werden kann.
   */
  public PropertyNode match()
  {
    if (!_isOfSameType())
      throw new RuntimeException("Klassen ungleich");

    PropertyNode root = new PropertyNode(leftbmh, remote, rightbmh, readOnly);

    filter.reset();

    List<IProperty> properties = leftPit.getProperties();
    for (IProperty property : properties)
    {
      _buildTree(EDirection.LEFT, property, root);
    }

    filter.reset();

    properties = rightPit.getProperties();
    for (IProperty property : properties)
    {
      _buildTree(EDirection.RIGHT, property, root);
    }

    root.reorder();
    //root.debugPrint("");
    root.buildDiff();

    return root;
  }

  private void _buildTree(EDirection pDirection, IProperty pProperty, PropertyNode pNode)
  {
    if (filter.canMatch(pProperty))
    {
      PropertyNode node = pNode;
      if (IPropertyPitProvider.class.isAssignableFrom(pProperty.getType()))
      {
        IPropertyPitProvider provider = (IPropertyPitProvider) pProperty.getValue();
        if (provider != null)
        {
          node = node.addProvider(pDirection, provider);
          List<IProperty> properties = provider.getPit().getProperties();
          for (IProperty property : properties)
          {
            _buildTree(pDirection, property, node);
          }
        }
      }
      else
      {
        node.addProperty(pDirection, pProperty);
      }
    }
  }


}
