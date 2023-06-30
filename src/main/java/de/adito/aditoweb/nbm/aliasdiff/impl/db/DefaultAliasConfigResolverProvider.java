package de.adito.aditoweb.nbm.aliasdiff.impl.db;

import de.adito.aditoweb.filesystem.datamodelfs.misc.IContextualAliasConfigResolver;
import de.adito.aditoweb.nbm.designer.commonclasses.SystemDefinitionAliasConfigResolver;
import de.adito.aditoweb.nbm.designer.commongui.components.systemselection.ServerSystemSelectionPanel;
import de.adito.aditoweb.nbm.designer.commoninterface.services.editorcontext.IEditorContext;
import de.adito.aditoweb.system.crmcomponents.majordatamodels.SystemDataModel;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.project.Project;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

import java.util.concurrent.atomic.AtomicReference;

/**
 * AliasConfigResolverProvider der die Abfrage auf den User delegiert
 *
 * @author w.glanzer, 14.07.2022
 */
@ServiceProvider(service = IAliasConfigResolverProvider.class)
public class DefaultAliasConfigResolverProvider implements IAliasConfigResolverProvider
{

  @Nullable
  @Override
  public IContextualAliasConfigResolver getResolver(@NonNull Project pProject)
  {
    AtomicReference<IContextualAliasConfigResolver> resolverRef = new AtomicReference<>();

    //System-Config auswählen
    ServerSystemSelectionPanel panel = new ServerSystemSelectionPanel(pProject);
    panel.showInDialog(NbBundle.getMessage(DefaultAliasConfigResolverProvider.class, "TITLE_SelectSystem"), e -> {
      if (e.getSource() == DialogDescriptor.OK_OPTION)
      {
        IEditorContext<SystemDataModel> selection = panel.getAliasField().getSelectedItem();
        if (selection != null && selection.isValid())
          resolverRef.set(new SystemDefinitionAliasConfigResolver(selection));
      }
    });

    return resolverRef.get();
  }

}
