package de.adito.aditoweb.nbm.aliasdiff.dialog.diffimpl;

import de.adito.aditoweb.nbm.aliasdiff.dialog.*;
import de.adito.aditoweb.system.crmcomponents.DataModelFactory;
import de.adito.propertly.core.spi.*;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Verwaltet ein Paar von IPropertyPitProvider Objekten,
 * oder pProperty Objekten. Ist eingebettet in einen PropertyNode.
 * Die Objekte können auch null sein.
 *
 * Die konkreten Implementierungen sind auf jeweils einen Typ
 * spezialisiert.
 *
 * Welches der beiden Objekte angesprochen werden soll, wird
 * durch eine Richtungsangabe abgebildet.
 *
 * @author t.tasior, 07.02.2018
 * @see EDirection
 * @see PropertyNode
 */
public abstract class AbstractPair
{
  public final static DataModelFactory FACTORY = new DataModelFactory();
  private final PropertyNode host;

  /**
   * Wird vom PropertyNode aufgerufen.
   *
   * @param pHost PropertyNode der dieses Objekt
   */
  public AbstractPair(PropertyNode pHost)
  {
    host = pHost;
  }

  /**
   * Liefert den PropertyNode der dieses Objekt verwaltet
   *
   * @return nimals null.
   */
  PropertyNode getHost()
  {
    return host;
  }

  /**
   * Nur eine Hilfsmethode für die Weiterentwicklung/Fehlersuche.
   *
   * @return einen aussagekräftigen Namen.
   */
  public abstract String nameForDebugPrint();

  /**
   * Liefert den Namen einer IDataModel Implementierung,
   * oder den Namen eines IProperty.
   *
   * @return den Namen der ein Objekt in der Datenmodellhierarchie
   * eindeutig identifiziert.
   */
  public abstract String nameForIdentification();

  /**
   * Liefert einen Namen der auf der GUI angezeigt wird.
   *
   * @param pDirection entscheidet auf welcher Seite im Dialog
   *                   der Name angezeigt wird.
   * @return einen String, oder null.
   */
  public abstract String nameForDisplay(@NotNull EDirection pDirection);

  /**
   * Liefert für die jeweilige Seite die Art der Differenz.
   *
   * @param pDirection entscheidet für die Darstellung im Baum.
   * @return eine EDiff Konstante.
   */
  public abstract EDiff typeOfDiff(@NotNull EDirection pDirection);

  /**
   * Liefert true, wenn die zu überprüfenden Objekte als gleich betrachtet werden.
   *
   * @return true bei Gleichheit.
   */
  public abstract boolean isEqual();
  
  /**
   * Ein Objekt soll erzeugt werden und in das übergebenene Parentobjekt eingehängt werden.
   *
   * @param pDirection gibt an für welches der beiden verglichenen Datenmodelle
   *                   das Objekt angelegt werden soll.
   * @param pParent    ein Datenmodell dem ein neuer Wert (Objekt) gesetzt wird:
   */
  public abstract void createDown(@NotNull EDirection pDirection, IPropertyPitProvider pParent);

  /**
   * Ein Knoten signalisiert seinen Kindern ihre Werte zu löschen.
   *
   * @param pDirection gibt an aus welchem der beiden Datenmodelle gelöscht werden soll.
   */
  public abstract void deleteDown(@NotNull EDirection pDirection);

  /**
   * Die konkrete Implementierung übernimmt hier das zu verwaltende IPropertyPitProvider
   * Objekt.
   *
   * @param pDirection die jeweilige Seite des Datenmodells.
   * @param pProvider  der zu verwaltende Provider, oder null wenn es in dem analysierten
   *                   Datenmodell keinen gibt.
   * @throws UnsupportedOperationException in der abstrakten Implementierung.
   */
  public void setProvider(@NotNull EDirection pDirection, IPropertyPitProvider pProvider)
  {
    throw new UnsupportedOperationException();

  }

  /**
   * Die konkrete Implementierung übernimmt hier das zu verwaltende IProperty
   * Objekt.
   *
   * @param pDirection die jeweilige Seite des Datenmodells.
   * @param pProperty  das zu verwaltende IProperty, oder null wenn es in dem analysierten
   *                   Datenmodell keines gibt.
   */
  public void setProperty(@NotNull EDirection pDirection, IProperty pProperty)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Liefert true, wenn das Property bereits vorhanden ist.
   * In welchem der beiden Datenmodelle ist egal.
   *
   * @param pProp dient zum Vergleich.
   * @return true, wenn es schon existiert.
   */
  public boolean containsProperty(@NotNull IProperty pProp)
  {
    return false;
  }

  /**
   * Liefert true, wenn der IPropertyPitProvider bereits vorhanden ist.
   * In welchem der beiden Datenmodelle ist egal.
   *
   * @param pProvider dient zum Vergleich.
   * @return true, wenn der Provider bereits existiert.
   */
  public boolean containsProvider(IPropertyPitProvider pProvider)
  {
    return false;
  }

  /**
   * Veranlasst das Setzen oder Entfernen eines Wertes im jeweiligen Datenmodell.
   *
   * @param pDirection gibt an in welchem der verglichenen Datenmodelle
   *                   ein Wert geschrieben wird.
   */
  public abstract void update(@NotNull EDirection pDirection);

  /**
   * Wird aufgerufen um den Ursprungszustand im Datenmodell wiederherzustellen.
   */
  public abstract void restore();

  /**
   * Hilfsmethode zum anlegen eines Datenmodells.
   * @param pDirection gibt an auf welcher Seite das Datenmodell erzeugt werden soll.
   * @param pRef transportiert das erzeugte Datenmodell zum Aufrufer.
   */
  protected abstract void create(@NotNull EDirection pDirection, AtomicReference pRef);

  /**
   * Liefert das durch diese Implementierung verwaltete Propertly.
   *
   * @param pDirection bestimmt die Seite für die das Propertly geliefert werden soll.
   * @return ein IProperty oder IPropertyPitProvider, oder null.
   */
  public abstract Object getManagedObject(@NotNull EDirection pDirection);

  
}
