package de.adito.aditoweb.nbm.aliasdiff.impl.update.sql;

import de.adito.aditoweb.common.IButtonInscriptions;
import de.adito.aditoweb.core.checkpoint.exception.mechanics.AditoRuntimeException;
import de.adito.aditoweb.core.multilanguage.*;
import de.adito.aditoweb.database.general.metainfo.IColumnMetadata;
import de.adito.aditoweb.nbm.aditonetbeansutil.misc.DataObjectUtil;
import de.adito.aditoweb.nbm.designer.commonclasses.editors.IGUIConst;
import de.adito.aditoweb.nbm.designerdb.api.DatabaseAccessProvider;
import de.adito.aditoweb.nbm.entitydbeditor.dataobjects.*;
import de.adito.aditoweb.swingcommon.components.combobox.standardcombobox.StandardComboBox;
import de.adito.aditoweb.swingcommon.layout.tablelayout.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.*;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.database.*;
import de.adito.aditoweb.system.crmcomponents.datatypes.EDatabaseType;
import org.jetbrains.annotations.*;
import org.openide.*;
import org.openide.nodes.Node;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility-Klasse für das Durchführen eines SQL-Exports auf Basis des Entity-Datenmodells (Basis sind EntityNodes)
 *
 * @author s.danner, 18.06.2015
 */
public class SQLExporter
{
  private SQLExporter()
  {
  }

  /**
   * Führt einen SQL-Export (Create-Statements) durch, dh. eine Menge von selektierten EntityNodes wird in
   * SQL-Statements umgewandelt
   *
   * @param pNodes die selektierten Nodes
   * @return die Create-Statements als String
   */
  @Nullable
  public static String export(Node[] pNodes)
  {
    String dbType = _getDBType();

    if (dbType == null)
      return null;

    //Vermerken aller Zugriffspunkte auf ein Entity-Modell
    EntityGroupDBDataObject group = null;
    List<EntityDBDataModel> entities = new ArrayList<>();
    for (Node node : pNodes)
    {
      group = node.getLookup().lookup(EntityGroupDBDataObject.class);
      //Wenn die Entity-Group-Node (Parent für alle Entities im Modell) selektiert ist, sollen alle Entities exportiert werden
      if (group != null)
        break;
      else
        entities.add(node.getLookup().lookup(EntityDBDataObject.class).getPropertyPitProvider());
    }

    if (group != null)
      return _export(Objects.requireNonNull(group.getPropertyPitProvider()).getEntities(), dbType);
    else
      return _export(entities, dbType);
  }

  /**
   * Erzeugt den SQL-String für ein bestimmtes DBMS
   *
   * @param pTableName    Tabellenname
   * @param pDbName       Datenbankname
   * @param pEntityFields Spalten
   * @return Entweder einen SQL-String für die Tabelle oder eine Exception
   */
  public static String generateTableSQL(String pTableName, String pDbName, List<IEntityFieldDataModel<?>> pEntityFields, String pSchemaName)
  {
    return _exportTable(pTableName, pDbName, pEntityFields, pSchemaName);
  }

  /**
   * Erzeugt den SQL-String für ein bestimmtes DBMS
   *
   * @param pTableName   Tabellenname
   * @param pDbName      Datenbankname
   * @param pEntityField Spalte
   * @return Entweder einen SQL-String für die Tabelle oder eine Exception
   */
  public static String generateColumnSQL(String pTableName, String pDbName, EntityFieldDBDataModel pEntityField, String pSchemaName)
  {
    try
    {
      EntityFieldDBDataObject fieldObj = (EntityFieldDBDataObject) DataObjectUtil.get(pEntityField);
      IColumnMetadata metadata = fieldObj.getColumnMetadata();
      if (metadata == null)
        return "";
      return DatabaseAccessProvider.getInstance().getDDLBuilder().getCreateColumnDDL(pDbName, pSchemaName, pTableName, metadata);
    }
    catch (Exception e)
    {
      throw new AditoRuntimeException(e, 20, 856);
    }
  }

  @NotNull
  private static String _exportTable(String pTableName, String pDbName, List<IEntityFieldDataModel<?>> pEntityFields, String pSchemaName)
  {
    try
    {
      List<IColumnMetadata> columns = new LinkedList<>();
      List<IColumnMetadata> pkColumns = new LinkedList<>();
      List<IColumnMetadata> idxColumns = new LinkedList<>();

      //Spalten erzeugen und mit Eigenschaften versehen
      for (IEntityFieldDataModel<?> entityField : pEntityFields)
      {
        //SQL-Export nur bei Datenbank-Entity-Modell möglich
        EntityFieldDBDataModel fieldModel = (EntityFieldDBDataModel) entityField;
        EntityFieldDBDataObject fieldObj = (EntityFieldDBDataObject) DataObjectUtil.get(entityField);
        IColumnMetadata metadata = fieldObj.getColumnMetadata();
        columns.add(metadata);
        if (Boolean.TRUE == fieldModel.getPrimaryKey())
          pkColumns.add(metadata);
        if (Boolean.TRUE == fieldModel.getIndex())
          idxColumns.add(metadata);
      }

      return "-- table " + pTableName + "\n" + DatabaseAccessProvider.getInstance().getDDLBuilder()
          .getCreateTableDDL(pDbName, pSchemaName, pTableName, columns, pkColumns, idxColumns);
    }
    catch (Exception e)
    {
      throw new AditoRuntimeException(e, 20, 692);
    }
  }

  /**
   * Wandelt eine Menge von Entity-Modellen in Create-Statements um
   *
   * @param pEntities die gewählten Entities
   * @param pDbName   der Datenbank-Produkt-Name
   * @return die Statements als String
   */
  private static String _export(@NotNull List<EntityDBDataModel> pEntities, String pDbName)
  {
    String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    StringBuilder sql = new StringBuilder();
    sql.append("-- SQL-Script generated on ").append(date).append("\n\n");
    for (IEntityDataModel<?, ?> entity : pEntities)
      sql.append(_exportTable(entity.getName(), pDbName, entity.getEntityFields(), "")).append("\n");

    return sql.toString();
  }

  /**
   * Erzeugt einen Dialog zum Wählen des Datenbank-Typen, welcher für den Export verwendet werden soll
   *
   * @return der Datenbank-Produkt-Name, der im Dialog ausgewählt wurde
   */
  private static String _getDBType()
  {
    ConvenienceTranslator ct = new ConvenienceTranslator(20);
    final String save = ct.translate(IStaticResources.TEXT_SAVE);
    final String cancel = IButtonInscriptions.CANCEL;

    Object[] buttons = {save, cancel};
    _DBSelectionPanel panel = new _DBSelectionPanel();
    DialogDescriptor descriptor = new DialogDescriptor(panel, ct.translate(IStaticResources.TITLE_DATABASE_TYPE), true,
                                                       buttons, save, DialogDescriptor.BOTTOM_ALIGN, null, null);

    Object result = DialogDisplayer.getDefault().notify(descriptor);
    if (result.equals(save))
      return panel.getSelectedValue().getDatabaseProductName();
    return null;
  }

  /**
   * Panel zum Auswählen des Datenbank-Typen
   */
  private static class _DBSelectionPanel extends JPanel
  {
    private final StandardComboBox<EDatabaseType> type;

    public _DBSelectionPanel()
    {
      double fill = TableLayout.FILL;
      double pref = TableLayout.PREFERRED;
      double gap = IGUIConst.GAP;

      double[] cols = {gap, pref, gap, fill, gap};
      double[] rows = {gap,
                       pref,
                       gap,
                       pref,
                       gap,
                       pref,
                       fill};

      setLayout(new TableLayout(cols, rows));
      TableLayoutUtil tlu = new TableLayoutUtil(this);

      type = new StandardComboBox<>();
      type.setModel(new DefaultComboBoxModel<>(EDatabaseType.subSet(EDatabaseType.UNKNOWN, EDatabaseType.NO_DATABASE)));

      ConvenienceTranslator ct = new ConvenienceTranslator(20);
      tlu.add(1, 1, new JLabel(ct.translate(IStaticResources.TEXT_CHOOSE_TYPE)));
      tlu.add(3, 1, type);
    }

    public EDatabaseType getSelectedValue()
    {
      return type.getSelectedItem();
    }
  }
}
