package net.coagulate.GPHUD;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.CommandAnnotation;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSS;
import net.coagulate.GPHUD.Modules.KVAnnotation;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.Permission.Permissionss;
import net.coagulate.GPHUD.Modules.PermissionAnnotation;
import net.coagulate.GPHUD.Modules.Pool.Pools;
import net.coagulate.GPHUD.Modules.Pool.Poolss;
import net.coagulate.GPHUD.Modules.PoolAnnotation;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;
import net.coagulate.GPHUD.Modules.SideMenuAnnotation;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.SideSubMenuAnnotation;
import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.Modules.URLAnnotation;

/** Provides dynamic loading for classes.
 * Scans the classpath (not JARs) and examines all classes found.
 Classes that extend an appropriate interface are initialised.
 Specifically Commands, Page, and Annotations classes are all registered with their subsystems.
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Classes {
    
    private static boolean LOGREGISTERS=false;
    private static enum BOOTPHASE {MODULES,PERMISSIONS,KVMAP,POOLS,COMMANDS,CONTENTS,SIDEMENUS,TEMPLATES}
    private static Logger log=null;
    // Start recursing through the elements in the classpath.  So many exceptions returned!
    static void initialise() throws SystemException, UserException {
        log=GPHUD.getLogger("Classes");
        ClassTools.initialise();
        loadModules();
        loadPermissions();
        loadKVMaps();
        loadPools();
        loadCommands();
        loadURLs();
        loadSideMenus();
        loadTemplates();
    }

    private static String getModuleName(Class c) {
        String name=c.getName().substring("net.coagulate.GPHUD.Modules.".length());
        String[] split=name.split("\\.");
        return split[0];
    }
    
    @SuppressWarnings("unchecked")
    private static void loadModules() {
        for (Class c:ClassTools.getAnnotatedClasses(ModuleDefinition.class)) {
            try {
                Annotation a = c.getAnnotation(ModuleDefinition.class);
                String implementation=((ModuleDefinition)a).implementation();
                String modulename=getModuleName(c);
                if (LOGREGISTERS) log.config("Registering module "+modulename+(implementation.isEmpty()?"":" ["+implementation+"]"));
                String creatingclass="net.coagulate.GPHUD.Modules.ModuleAnnotation";
                if (!implementation.isEmpty()) { creatingclass=implementation; }
                Class<?> target=Class.forName(creatingclass);
                Constructor<?> cons=target.getConstructor(String.class,ModuleDefinition.class);
                cons.newInstance(modulename,a);
            } catch (ClassNotFoundException|NoSuchMethodException|SecurityException|InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
                throw new SystemException("Instansiating module failed: "+ex.toString(),ex);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void loadPermissions() {
        for (Class c:ClassTools.getAnnotatedClasses(Permissions.class)) {
            String modulename=getModuleName(c);
            for (Annotation a:c.getAnnotationsByType(Permissions.class)) {
                if (LOGREGISTERS) log.config("Registering permission "+modulename+"/"+((Permissions)a).name());
                ((ModuleAnnotation)Modules.get(null,modulename)).registerPermission(new PermissionAnnotation((Permissions)a));
            }            
        }
        for (Class c:ClassTools.getAnnotatedClasses(Permissionss.class)) {
            String modulename=getModuleName(c);
            for (Annotation as:c.getAnnotationsByType(Permissionss.class)) {
                for (Permissions a:((Permissionss)as).value()) {
                    if (LOGREGISTERS) log.config("Registering permissions "+modulename+"/"+((Permissions)a).name());
                    ((ModuleAnnotation)Modules.get(null,modulename)).registerPermission(new PermissionAnnotation((Permissions)a));
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void loadKVMaps() {
        for (Class c:ClassTools.getAnnotatedClasses(KVS.class)) {
            String modulename=getModuleName(c);
            Annotation a=c.getAnnotation(KVS.class);
            if (LOGREGISTERS) log.config("Registering KV "+modulename+"/"+((KVS)a).name());
            ((ModuleAnnotation)Modules.get(null,modulename)).registerKV(new KVAnnotation(Modules.get(null,modulename),(KVS) a));
        }
        for (Class c:ClassTools.getAnnotatedClasses(KVSS.class)) {
            String modulename=getModuleName(c);
            Annotation a=c.getAnnotation(KVSS.class);
            for (KVS element:((KVSS)a).value()) {
                if (LOGREGISTERS) log.config("Registering KVS "+modulename+"/"+element.name());
                ((ModuleAnnotation)Modules.get(null,modulename)).registerKV(new KVAnnotation(Modules.get(null,modulename),element));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void loadPools() {
        for (Class c:ClassTools.getAnnotatedClasses(Pools.class)) {
            String modulename=getModuleName(c);
            Annotation a=c.getAnnotation(Pools.class);
            if (LOGREGISTERS) log.config("Registering pool "+modulename+"/"+((Pools)a).name());
            ((ModuleAnnotation)Modules.get(null,modulename)).registerPool(new PoolAnnotation(Modules.get(null,modulename),(Pools)a));
        }
        for (Class c:ClassTools.getAnnotatedClasses(Poolss.class)) {
            String modulename=getModuleName(c);
            Annotation a=c.getAnnotation(Poolss.class);
            for (Pools element:((Poolss)a).value()) {
                if (LOGREGISTERS) log.config("Registering pools "+modulename+"/"+((Pools)a).name());
                ((ModuleAnnotation)Modules.get(null,modulename)).registerPool(new PoolAnnotation(Modules.get(null,modulename),(Pools)a));
            }
        }
    }
        
    @SuppressWarnings("unchecked")
    private static void loadCommands() {
        for (Method m:ClassTools.getAnnotatedMethods(Commands.class)) {
            String modulename=getModuleName(m.getDeclaringClass());
            Annotation a=m.getAnnotation(Commands.class);
            if (LOGREGISTERS) log.config("Registering command "+modulename+"/"+m.getName());
            ((ModuleAnnotation)Modules.get(null,modulename)).registerCommand(new CommandAnnotation(Modules.get(null,modulename),m));
        }
    }    
    @SuppressWarnings("unchecked")
    private static void loadURLs() {
        for (Method m:ClassTools.getAnnotatedMethods(URLs.class)) {
            if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) { 
                String modulename=getModuleName(m.getDeclaringClass());
                Annotation a=m.getAnnotation(URLs.class);
                if (LOGREGISTERS) log.config("Registering URL handler "+modulename+"/"+m.getName());
                ((ModuleAnnotation)Modules.get(null,modulename)).registerContent(new URLAnnotation(Modules.get(null,modulename),m));
            }
        }
    }    
    
    @SuppressWarnings("unchecked")
    private static void loadSideMenus() {
        for (Class c:ClassTools.getAnnotatedClasses(SideMenus.class)) {
            String modulename=getModuleName(c);
            Annotation a=c.getAnnotation(SideMenus.class);
            if (LOGREGISTERS) log.config("Registering side menu "+modulename+"/"+((SideMenus)a).name());
            ((ModuleAnnotation)Modules.get(null,modulename)).setSideMenu(null,new SideMenuAnnotation((SideMenus)a));
        }
        for (Method m:ClassTools.getAnnotatedMethods(SideSubMenus.class)) {
            if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) { 
                String modulename=getModuleName(m.getDeclaringClass());
                Annotation a=m.getAnnotation(SideSubMenus.class);
                if (LOGREGISTERS) log.config("Registering side sub menu "+modulename+"/"+((SideSubMenus)a).name());
                ((ModuleAnnotation)Modules.get(null,modulename)).registerSideSubMenu(null,new SideSubMenuAnnotation(m));
            }
        }
    }    
    @SuppressWarnings("unchecked")
    private static void loadTemplates() {
        for (Method m:ClassTools.getAnnotatedMethods(Template.class)) {
            if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) { 
                String modulename=getModuleName(m.getDeclaringClass());
                Annotation a=m.getAnnotation(Template.class);
                if (LOGREGISTERS) log.config("Registering template "+((Template)a).name());
                Templater.register((Template)a,m);
            }
        }
    }    
}
