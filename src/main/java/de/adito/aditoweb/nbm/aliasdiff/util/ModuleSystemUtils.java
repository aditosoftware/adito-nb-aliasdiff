package de.adito.aditoweb.nbm.aliasdiff.util;

import lombok.NonNull;
import lombok.extern.java.Log;
import org.openide.modules.*;
import org.openide.util.SharedClassObject;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Utility for the module system of netbeans.
 * May contain everything that is done by reflection API in netbeans / ADITO code.
 *
 * @author w.glanzer, 28.06.2023
 */
@Log
@OnStop
public class ModuleSystemUtils implements Runnable
{
  /**
   * We need a strong reference on our replaced actions, so they won't get GCed.
   * They will only be weak referenced in NetBeans SharedObject
   */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Strong References
  private static final Set<Object> replacedSharedObjectRefs = new HashSet<>();

  @Override
  public void run()
  {
    // Dispose shared objects
    replacedSharedObjectRefs.clear();
  }

  /**
   * Adds a new declaration of the given package to the given module.
   * This has to be done, if any class of a private package gets referenced in runtime code.
   *
   * @param pModuleName  ID of the Module to add the export to
   * @param pPackageName Name of the package to export
   */
  public static void addModuleExport(@NonNull String pModuleName, @NonNull String pPackageName)
  {
    try
    {
      // separation by slash, instead of dot
      pPackageName = pPackageName.replace('.', '/');

      // has to end with a slash
      if (!pPackageName.endsWith("/"))
        pPackageName = pPackageName + "/";

      ModuleInfo module = Modules.getDefault().findCodeNameBase(pModuleName);

      Field moduleDataField = module.getClass().getSuperclass().getDeclaredField("data");
      moduleDataField.setAccessible(true);
      Object moduleData = moduleDataField.get(module);

      Field publicPackagesField = moduleData.getClass().getSuperclass().getDeclaredField("publicPackages");
      publicPackagesField.setAccessible(true);
      Object[] publicPackages = (Object[]) publicPackagesField.get(moduleData);

      Class<?> export = Class.forName("org.netbeans.Module$PackageExport", true, Modules.getDefault().findCodeNameBase("org.netbeans.bootstrap").getClassLoader());

      Object[] newPublicPackages = (Object[]) Array.newInstance(export, publicPackages.length + 1);
      System.arraycopy(publicPackages, 0, newPublicPackages, 0, publicPackages.length);
      newPublicPackages[newPublicPackages.length - 1] = export.getDeclaredConstructor(String.class, boolean.class).newInstance(pPackageName, false);

      publicPackagesField.set(moduleData, newPublicPackages);
    }
    catch (Throwable t)
    {
      log.log(Level.WARNING, "", t);
    }
  }

  /**
   * Replaces the given shared object instance with a new one, of another type
   *
   * @param pOldActionType Type that should be replaced
   * @param pNewActionType Type that replaces the old one
   */
  public static <T extends SharedClassObject> void replaceSharedObject(@NonNull Class<T> pOldActionType, @NonNull Class<? extends T> pNewActionType)
  {
    try
    {
      // instantiate and strongly reference the new object, if necessary
      replacedSharedObjectRefs.add(SharedClassObject.findObject(pNewActionType, true));

      Field valuesField = SharedClassObject.class.getDeclaredField("values");
      valuesField.setAccessible(true);

      //noinspection unchecked,rawtypes
      Map<Class, Object> values = (Map<Class, Object>) valuesField.get(null);

      values.put(pOldActionType, values.get(pNewActionType));
    }
    catch (Throwable t)
    {
      log.log(Level.WARNING, "", t);
    }
  }

}
