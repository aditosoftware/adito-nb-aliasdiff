package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.filesystem.propertly.BulkModifyHierarchy;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.DefaultPropertyFilter;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.DiffPresenter;
import de.adito.aditoweb.system.crmcomponents.annotations.DIFF;
import de.adito.propertly.core.common.path.PropertyPath;
import de.adito.propertly.core.spi.*;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Accepts two data models (IPropertyPitProvider) of the same type and builds a tree from their structure.
 * The comparison is executed on the tree and then the same elements (nodes) are filtered out.
 * <p>
 * The resulting tree can be displayed in a GUI.
 * In order to be able to distinguish the data models there, they are divided into "left" and "right".
 *
 * @author t.tasior, 30.01.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 * @see EDirection
 * @see DiffPresenter
 */
public abstract class AbstractPropertyPitMatcher<L extends IPropertyPitProvider<?, ?, ?>, R extends IPropertyPitProvider<?, ?, ?>>
{
  private final IPropertyPit<?, ?, ?> leftPit;
  private final IPropertyFilter filter;
  private final IPropertyPit<?, ?, ?> rightPit;
  private final EDirection readOnly;
  private final BulkModifyHierarchy<?> leftbmh;
  private final EDirection remote;
  private final BulkModifyHierarchy<?> rightbmh;

  /**
   * Initialization with the data models to be compared,
   * as well as other information relevant to the representation of the tree.
   *
   * @param pLeftPPP  left side data model
   * @param pFilter   filters out objects that should not be compared.
   *                  If null is passed, a filter is applied that only allows objects that have the annotation @DIFF pass.
   * @param pRemote   determines the side, that is not locally available and therefor is remote.
   *                  Null, if both sides are locally available.
   * @param pRightPPP right side data model
   * @param pReadOnly determines the side that is read only.
   *                  Null, if both sides can be written
   * @see DIFF
   */
  protected AbstractPropertyPitMatcher(@NonNull L pLeftPPP, @Nullable IPropertyFilter pFilter, @Nullable EDirection pRemote, @NonNull R pRightPPP,
                                       @Nullable EDirection pReadOnly)
  {
    leftbmh = new BulkModifyHierarchy<>(pLeftPPP.getPit().getHierarchy());
    //noinspection rawtypes
    leftPit = ((IPropertyPitProvider) Objects.requireNonNull(Objects.requireNonNull(new PropertyPath(pLeftPPP).find(leftbmh)).getValue())).getPit();

    filter = (pFilter != null) ? pFilter : new DefaultPropertyFilter();
    remote = pRemote;

    rightbmh = new BulkModifyHierarchy<>(pRightPPP.getPit().getHierarchy());
    //noinspection rawtypes
    rightPit = ((IPropertyPitProvider) Objects.requireNonNull(Objects.requireNonNull(new PropertyPath(pRightPPP).find(rightbmh)).getValue())).getPit();
    readOnly = pReadOnly;
  }

  /**
   * Creates the tree, sorts it and returns its root node
   *
   * @return the root node that can be displayed inside a JTree
   */
  public PropertyNode match()
  {
    if (!(leftPit.getOwnProperty().getType().equals(rightPit.getOwnProperty().getType())))
      throw new IllegalStateException(leftPit.getOwnProperty().getType().getName() + " could not be compared with " +
                                          rightPit.getOwnProperty().getType().getName());

    PropertyNode root = new PropertyNode(leftbmh, remote, rightbmh, readOnly);

    filter.reset();
    leftPit.getProperties()
        .forEach(pProp -> buildTree(EDirection.LEFT, pProp, root));

    filter.reset();
    rightPit.getProperties()
        .forEach(pProp -> buildTree(EDirection.RIGHT, pProp, root));

    root.reorder();
    root.buildDiff();
    return root;
  }

  /**
   * Appends the given property (and all of its children) to the tree, if possible
   *
   * @param pDirection Current side to append to
   * @param pProperty  Property to append
   * @param pNode      Node to append
   */
  private void buildTree(@NonNull EDirection pDirection, @NonNull IProperty<?, ?> pProperty, @NonNull PropertyNode pNode)
  {
    if (filter.test(pProperty))
    {
      if (IPropertyPitProvider.class.isAssignableFrom(pProperty.getType()))
      {
        IPropertyPitProvider<?, ?, ?> provider = (IPropertyPitProvider<?, ?, ?>) pProperty.getValue();
        if (provider != null)
        {
          PropertyNode node = pNode.addProvider(pDirection, provider);
          provider.getPit().getProperties()
              .forEach(pProp -> buildTree(pDirection, pProp, node));
        }
      }
      else
        pNode.addProperty(pDirection, pProperty);
    }
  }


}
