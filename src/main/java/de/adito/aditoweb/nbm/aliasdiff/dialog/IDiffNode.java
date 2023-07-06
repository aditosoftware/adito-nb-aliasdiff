package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.DiffPresenter;
import de.adito.propertly.core.spi.IHierarchy;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;

/**
 * An implementation of this node can be presented via {@link DiffPresenter}
 *
 * @author T.Tasior, 27.03.2018
 * @author w.glanzer, 29.06.2023 (refactored, translated)
 * @see DiffPresenter
 */
public interface IDiffNode extends TreeNode
{
  /**
   * Determines, if the data model is readonly on the given side
   *
   * @param pDirection side to check for readonly state
   * @return true, if readonly
   */
  boolean isReadOnly(@NonNull EDirection pDirection);

  /**
   * True, if the model on the given side is a remote one
   *
   * @param pDirection side to check for remote state
   * @return true, if the model is on a remote location
   */
  boolean isRemote(@NonNull EDirection pDirection);

  /**
   * Returns the name of the root on the given side
   *
   * @param pDirection side to get the name for
   * @return Name of the root
   */
  @NonNull
  String getRootName(@NonNull EDirection pDirection);

  /**
   * Calculates the differences in the whole tree structure
   *
   * @return Value between 0 and Integer.MAX_VALUE
   */
  int countDifferences();

  /**
   * @return the root pair to manage
   */
  @NonNull
  AbstractPair getPair();

  /**
   * Returns a string that can be displayed on gui
   *
   * @param pDirection side to get the name for
   * @return the displayable string for the given side
   */
  @Nullable
  String nameForDisplay(@NonNull EDirection pDirection);

  /**
   * Collects every single statuses, recursive
   *
   * @param pParentCollector the collector of the parent or null, if we are the root
   * @return the collector with our states
   */
  @NonNull
  DiffStateCollector collectDiffStates(@Nullable DiffStateCollector pParentCollector);

  /**
   * Calculcates the type of diff on the given side
   *
   * @param pDirection side to get the diff type for
   * @return the type
   */
  @NonNull
  EDiff getDiff(@NonNull EDirection pDirection);

  /**
   * Searches the hierarchy that the node on the given side references
   *
   * @param pDirection side to get the hierarchy from
   * @return the hierarchy or null, if the node does not belong to any hierarchy
   */
  @Nullable
  IHierarchy<?> getHierarchy(@NonNull EDirection pDirection); //NOSONAR generics are allowed here

  /**
   * Writes the changed values back to the data models
   */
  void write();


}
