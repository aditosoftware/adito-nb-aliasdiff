package de.adito.aditoweb.nbm.aliasdiff.dialog;

import de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl.*;
import org.jetbrains.annotations.*;

import javax.swing.tree.TreeNode;

/**
 * Eine Implementierung kann im DiffPresenter dargestellt werden.
 * @see de.adito.aditoweb.nbm.aliasdiff.dialog.diffpresenter.DiffPresenter
 * @author T.Tasior, 27.03.2018
 */
public interface IDiffNode extends TreeNode
{
  /**
   * Liefert true wenn das Datenmodell auf der angegebenen Seite nur
   * gelesen werden kann
   *
   * @param pDirection das linke oder rechte Datenmodell wird befragt.
   * @return false wenn in das angegebene Datenmodell ein Wert geschrieben
   * werden kann.
   */
   boolean isReadOnly(@NotNull EDirection pDirection);

  /**
   * Liefert den Namen eines der beiden Datenmodelle.
   *
   * @param pDirection das linke bzw. rechte Datenmodell wird befragt.
   * @return normalerweise den Namen der im Designer für dieses Datenmodell
   * präsentiert wird.
   */
   String getRootName(EDirection pDirection);

  /**
   * Liefert true wenn das Datenmodell über einen Netzwerkzugriff
   * angefordert wurde.
   *
   * @param pDirection das linke oder rechte Datenmodell wird befragt.
   * @return false, wenn das Datenmodell nicht über einen
   * Netzwerkzugriff bereitgestellt wurde.
   */
   boolean isRemote(@NotNull EDirection pDirection);

  /**
   * Liefert die gesamte Anzahl an Differenzen in der Baumstruktur.
   *
   * @return Wert zwischen 0 und Integer.MAX_VALUE.
   */
   int countDifferences();


  /**
   * Liefert eine AbstractPair Implementierung die Werte bzw
   * Datenmodelle verwaltet.
   *
   * @return AbstractPair Implementierung dieses Nodes.
   */
   AbstractPair getPair();

  /**
   * Liefert einen String für die Anzeige in der GUI.
   *
   * @param pDirection betrifft die linke oder rechte Seite.
   * @return immer einen String.
   */
   String nameForDisplay(EDirection pDirection);

  /**
   * Sammelt die Diff Stati von sich und seinen Kindern.
   *
   * @param pParentCollector sammelt die Stati der einzelnen Knoten.
   * @return den Kollektor mit den Stati dieses Teilbaums.
   */
   DiffStateCollector collectDiffStates(@Nullable DiffStateCollector pParentCollector);

  /**
   * Ermittelt die Art der Wertedifferenz für eine bestimmte Seite
   *
   * @param pDir links, oder rechts.
   * @return eine der EDiff Konstanten.
   */
   EDiff getDiff(@NotNull EDirection pDir);
   
  /**
   * Schreibt die geänderten Werte zurück ins Datenmodell.
   */
   void write();

  
}
