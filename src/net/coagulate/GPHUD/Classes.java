package net.coagulate.GPHUD;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSS;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.Permission.Permissionss;
import net.coagulate.GPHUD.Modules.Pool.Pools;
import net.coagulate.GPHUD.Modules.Pool.Poolss;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.Modules.URL.URLs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.logging.Logger;

/**
 * Provides dynamic loading for classes.
 * Scans the classpath (not JARs) and examines all classes found.
 * Classes that extend an appropriate interface are initialised.
 * Specifically Commands, Page, and Annotations classes are all registered with their subsystems.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Classes {

	private static final boolean LOGREGISTERS=false;
	@Nullable
	private static Logger log;

	// ----- Internal Statics -----
	// Start recursing through the elements in the classpath.  So many exceptions returned!
	static void initialise() {
		ClassTools.initialise();
		loadModules();
		loadPermissions();
		loadKVMaps();
		loadPools();
		loadCommands();
		loadURLs();
		loadSideMenus();
		loadTemplates();
		loadGSFunctions();
	}

	private static String getModuleName(@Nonnull final Class<?> c) {
		final String name=c.getName().substring("net.coagulate.GPHUD.Modules.".length());
		final String[] split=name.split("\\.");
		return split[0];
	}

	private static void loadModules() {
		for (final Class<?> c: ClassTools.getAnnotatedClasses(ModuleDefinition.class)) {
			try {
				final ModuleDefinition a=c.getAnnotation(ModuleDefinition.class);
				final String implementation=a.implementation();
				final String modulename=getModuleName(c);
				if (LOGREGISTERS) { log().config("Registering module "+modulename+(implementation.isEmpty()?"":" ["+implementation+"]")); }
				String creatingclass="net.coagulate.GPHUD.Modules.ModuleAnnotation";
				if (!implementation.isEmpty()) { creatingclass=implementation; }
				final Class<?> target=Class.forName(creatingclass);
				final Constructor<?> cons=target.getConstructor(String.class,ModuleDefinition.class);
				cons.newInstance(modulename,a);
			}
			catch (@Nonnull
			final ClassNotFoundException|NoSuchMethodException|SecurityException|InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException ex) {
				throw new SystemImplementationException("Instansiating module failed: "+ex,ex);
			}
		}
	}

	private static void loadPermissions() {
		for (final Class<?> c: ClassTools.getAnnotatedClasses(Permissions.class)) {
			final String modulename=getModuleName(c);
			for (final Annotation a: c.getAnnotationsByType(Permissions.class)) {
				if (LOGREGISTERS) { log().config("Registering permission "+modulename+"/"+((Permissions) a).name()); }
				((ModuleAnnotation) Modules.get(null,modulename)).registerPermission(new PermissionAnnotation((Permissions) a,modulename));
			}
		}
		for (final Class<?> c: ClassTools.getAnnotatedClasses(Permissionss.class)) {
			final String modulename=getModuleName(c);
			for (final Annotation as: c.getAnnotationsByType(Permissionss.class)) {
				for (final Permissions a: ((Permissionss) as).value()) {
					if (LOGREGISTERS) { log().config("Registering permissions "+modulename+"/"+a.name()); }
					((ModuleAnnotation) Modules.get(null,modulename)).registerPermission(new PermissionAnnotation(a,modulename));
				}
			}
		}
	}

	private static void loadKVMaps() {
		for (final Class<?> c: ClassTools.getAnnotatedClasses(KVS.class)) {
			final String modulename=getModuleName(c);
			final KVS a=c.getAnnotation(KVS.class);
			if (LOGREGISTERS) { log().config("Registering KV "+modulename+"/"+a.name()); }
			((ModuleAnnotation) Modules.get(null,modulename)).registerKV(new KVAnnotation(Modules.get(null,modulename),a));
		}
		for (final Class<?> c: ClassTools.getAnnotatedClasses(KVSS.class)) {
			final String modulename=getModuleName(c);
			final KVSS a=c.getAnnotation(KVSS.class);
			for (final KVS element: a.value()) {
				if (LOGREGISTERS) { log().config("Registering KVS "+modulename+"/"+element.name()); }
				((ModuleAnnotation) Modules.get(null,modulename)).registerKV(new KVAnnotation(Modules.get(null,modulename),element));
			}
		}
	}

	private static void loadPools() {
		for (final Class<?> c: ClassTools.getAnnotatedClasses(Pools.class)) {
			final String modulename=getModuleName(c);
			final Pools a=c.getAnnotation(Pools.class);
			if (LOGREGISTERS) { log().config("Registering pool "+modulename+"/"+a.name()); }
			((ModuleAnnotation) Modules.get(null,modulename)).registerPool(new PoolAnnotation(Modules.get(null,modulename),a));
		}
		for (final Class<?> c: ClassTools.getAnnotatedClasses(Poolss.class)) {
			final String modulename=getModuleName(c);
			final Poolss a=c.getAnnotation(Poolss.class);
			for (final Pools element: a.value()) {
				if (LOGREGISTERS) { log().config("Registering pools "+modulename+"/"+((Pools) a).name()); }
				((ModuleAnnotation) Modules.get(null,modulename)).registerPool(new PoolAnnotation(Modules.get(null,modulename),(Pools) a));
			}
		}
	}

	private static void loadCommands() {
		for (final Method m: ClassTools.getAnnotatedMethods(Commands.class)) {
			final String modulename=getModuleName(m.getDeclaringClass());
			final Annotation a=m.getAnnotation(Commands.class);
			if (LOGREGISTERS) { log().config("Registering command "+modulename+"/"+m.getName()); }
			((ModuleAnnotation) Modules.get(null,modulename)).registerCommand(new CommandAnnotation(Modules.get(null,modulename),m));
			// validate command argument annotations
			boolean firstparam=true;
			for (final Parameter p: m.getParameters()) {
				if (firstparam) { firstparam=false; }
				else {
					final Argument.Arguments[] annotations=p.getAnnotationsByType(Argument.Arguments.class);
					if (annotations.length!=1) {
						throw new SystemImplementationException("Command "+modulename+"/"+m.getName()+" parameter "+p.getName()+" has no Arguments annotation");
					}
					final Argument.Arguments annotation=annotations[0];
					final boolean requiresmax;
					switch (annotation.type()) {
						case TEXT_CLEAN:
						case TEXT_ONELINE:
						case TEXT_INTERNAL_NAME:
						case TEXT_MULTILINE:
							requiresmax=true;
							break;
						case PASSWORD:
						case INTEGER:
						case FLOAT:
						case BOOLEAN:
						case CHOICE:
						case CHARACTER:
						case CHARACTER_PLAYABLE:
						case CHARACTER_NEAR:
						case CHARACTER_FACTION:
						case AVATAR:
						case AVATAR_NEAR:
						case PERMISSIONSGROUP:
						case PERMISSION:
						case CHARACTERGROUP:
						case KVLIST:
						case MODULE:
						case REGION:
						case ZONE:
						case COORDINATES:
						case EVENT:
						case EFFECT:
						case ATTRIBUTE:
						case ATTRIBUTE_WRITABLE:
						case CURRENCY:
						case INVENTORY:
						case SET:
						case ITEM:
							requiresmax=false;
							break;
						default:
							throw new SystemImplementationException("Unchecked argument type "+annotation.type().name());

					}
					if (requiresmax && annotation.max()<0) {
						throw new SystemImplementationException("Missing MAX parameter on argument annotation in ["+modulename+"]"+m.getClass()
						                                                                                                            .getSimpleName()+"/"+m.getName()+"/"+p.getName());
					}
				}
			}
		}
	}

	private static void loadURLs() {
		for (final Method m: ClassTools.getAnnotatedMethods(URLs.class)) {
			if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) {
				final String modulename=getModuleName(m.getDeclaringClass());
				final Annotation a=m.getAnnotation(URLs.class);
				if (LOGREGISTERS) { log().config("Registering URL handler "+modulename+"/"+m.getName()); }
				((ModuleAnnotation) Modules.get(null,modulename)).registerContent(new URLAnnotation(Modules.get(null,modulename),m));
			}
		}
	}

	private static void loadSideMenus() {
		for (final Class<?> c: ClassTools.getAnnotatedClasses(SideMenus.class)) {
			final String modulename=getModuleName(c);
			final SideMenus a=c.getAnnotation(SideMenus.class);
			if (LOGREGISTERS) { log().config("Registering side menu "+modulename+"/"+a.name()); }
			((ModuleAnnotation) Modules.get(null,modulename)).setSideMenu(null,new SideMenuAnnotation(a));
		}
		for (final Method m: ClassTools.getAnnotatedMethods(SideSubMenus.class)) {
			if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) {
				final String modulename=getModuleName(m.getDeclaringClass());
				final SideSubMenus a=m.getAnnotation(SideSubMenus.class);
				if (LOGREGISTERS) { log().config("Registering side sub menu "+modulename+"/"+a.name()); }
				((ModuleAnnotation) Modules.get(null,modulename)).registerSideSubMenu(null,new SideSubMenuAnnotation(m));
			}
		}
	}

	private static void loadTemplates() {
		for (final Method m: ClassTools.getAnnotatedMethods(Template.class)) {
			if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) {
				final String modulename=getModuleName(m.getDeclaringClass());
				final Template a=m.getAnnotation(Template.class);
				if (LOGREGISTERS) { log().config("Registering template "+a.name()); }
				Templater.register(a,m);
			}
		}
	}

	private static void loadGSFunctions() {
		for (final Method m: ClassTools.getAnnotatedMethods(GSFunctions.GSFunction.class)) {
			final Annotation a=m.getAnnotation(GSFunctions.GSFunction.class);
			if (LOGREGISTERS) { log().config("Registering gsFunction "+m.getName()); }
			GSFunctions.register(m.getName(),m);
		}
	}


	@Nonnull
	private static Logger log() {
		if (log==null) { log=GPHUD.getLogger("Classes"); }
		return log;
	}
}
