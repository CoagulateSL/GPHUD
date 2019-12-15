package net.coagulate.GPHUD;

import net.coagulate.Core.Tools.ClassTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
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

	private static final boolean LOGREGISTERS = false;
	@Nullable
	private static Logger log=null;

	// Start recursing through the elements in the classpath.  So many exceptions returned!
	static void initialise() throws SystemException, UserException {
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

	private static String getModuleName(@Nonnull Class<? extends Object> c) {
		String name = c.getName().substring("net.coagulate.GPHUD.Modules.".length());
		String[] split = name.split("\\.");
		return split[0];
	}

	private static void loadModules() {
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(ModuleDefinition.class)) {
			try {
				ModuleDefinition a = c.getAnnotation(ModuleDefinition.class);
				String implementation = a.implementation();
				String modulename = getModuleName(c);
				if (LOGREGISTERS)
					log().config("Registering module " + modulename + (implementation.isEmpty() ? "" : " [" + implementation + "]"));
				String creatingclass = "net.coagulate.GPHUD.Modules.ModuleAnnotation";
				if (!implementation.isEmpty()) { creatingclass = implementation; }
				Class<?> target = Class.forName(creatingclass);
				Constructor<?> cons = target.getConstructor(String.class, ModuleDefinition.class);
				cons.newInstance(modulename, a);
			} catch (@Nonnull ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				throw new SystemException("Instansiating module failed: " + ex.toString(), ex);
			}
		}
	}

	private static void loadPermissions() {
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(Permissions.class)) {
			String modulename = getModuleName(c);
			for (Annotation a : c.getAnnotationsByType(Permissions.class)) {
				if (LOGREGISTERS) log().config("Registering permission " + modulename + "/" + ((Permissions) a).name());
				((ModuleAnnotation) Modules.get(null, modulename)).registerPermission(new PermissionAnnotation((Permissions) a,modulename));
			}
		}
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(Permissionss.class)) {
			String modulename = getModuleName(c);
			for (Annotation as : c.getAnnotationsByType(Permissionss.class)) {
				for (Permissions a : ((Permissionss) as).value()) {
					if (LOGREGISTERS)
						log().config("Registering permissions " + modulename + "/" + a.name());
					((ModuleAnnotation) Modules.get(null, modulename)).registerPermission(new PermissionAnnotation(a,modulename));
				}
			}
		}
	}

	private static void loadKVMaps() {
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(KVS.class)) {
			String modulename = getModuleName(c);
			KVS a = c.getAnnotation(KVS.class);
			if (LOGREGISTERS) log().config("Registering KV " + modulename + "/" + a.name());
			((ModuleAnnotation) Modules.get(null, modulename)).registerKV(new KVAnnotation(Modules.get(null, modulename), a));
		}
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(KVSS.class)) {
			String modulename = getModuleName(c);
			KVSS a = c.getAnnotation(KVSS.class);
			for (KVS element : a.value()) {
				if (LOGREGISTERS) log().config("Registering KVS " + modulename + "/" + element.name());
				((ModuleAnnotation) Modules.get(null, modulename)).registerKV(new KVAnnotation(Modules.get(null, modulename), element));
			}
		}
	}

	private static void loadPools() {
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(Pools.class)) {
			String modulename = getModuleName(c);
			Pools a = c.getAnnotation(Pools.class);
			if (LOGREGISTERS) log().config("Registering pool " + modulename + "/" + a.name());
			((ModuleAnnotation) Modules.get(null, modulename)).registerPool(new PoolAnnotation(Modules.get(null, modulename), a));
		}
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(Poolss.class)) {
			String modulename = getModuleName(c);
			Poolss a = c.getAnnotation(Poolss.class);
			for (Pools element : a.value()) {
				if (LOGREGISTERS) log().config("Registering pools " + modulename + "/" + ((Pools) a).name());
				((ModuleAnnotation) Modules.get(null, modulename)).registerPool(new PoolAnnotation(Modules.get(null, modulename), (Pools) a));
			}
		}
	}

	private static void loadCommands() {
		for (Method m : ClassTools.getAnnotatedMethods(Commands.class)) {
			String modulename = getModuleName(m.getDeclaringClass());
			Annotation a = m.getAnnotation(Commands.class);
			if (LOGREGISTERS) log().config("Registering command " + modulename + "/" + m.getName());
			((ModuleAnnotation) Modules.get(null, modulename)).registerCommand(new CommandAnnotation(Modules.get(null, modulename), m));
			// validate command argument annotations
			boolean firstparam = true;
			for (Parameter p : m.getParameters()) {
				if (firstparam) { firstparam = false; } else {
					Argument.Arguments[] annotations = p.getAnnotationsByType(Argument.Arguments.class);
					if (annotations.length != 1) {
						throw new SystemException("Command " + modulename + "/" + m.getName() + " parameter " + p.getName() + " has no Arguments annotation");
					}
					Argument.Arguments annotation = annotations[0];
					boolean requiresmax;
					switch (annotation.type()) {
						case TEXT_CLEAN:
						case TEXT_ONELINE:
						case TEXT_INTERNAL_NAME:
						case TEXT_MULTILINE:
							requiresmax = true;
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
						case ATTRIBUTE:
						case ATTRIBUTE_WRITABLE:
							requiresmax = false;
							break;
						default:
							throw new SystemException("Unchecked argument type " + annotation.type().name());

					}
					if (requiresmax && annotation.max() < 0) {
						throw new SystemException("Missing MAX parameter on argument annotation in [" + modulename + "]" + m.getClass().getSimpleName() + "/" + m.getName() + "/" + p.getName());
					}
				}
			}
		}
	}

	private static void loadURLs() {
		for (Method m : ClassTools.getAnnotatedMethods(URLs.class)) {
			if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) {
				String modulename = getModuleName(m.getDeclaringClass());
				Annotation a = m.getAnnotation(URLs.class);
				if (LOGREGISTERS) log().config("Registering URL handler " + modulename + "/" + m.getName());
				((ModuleAnnotation) Modules.get(null, modulename)).registerContent(new URLAnnotation(Modules.get(null, modulename), m));
			}
		}
	}

	private static void loadSideMenus() {
		for (Class<? extends Object> c : ClassTools.getAnnotatedClasses(SideMenus.class)) {
			String modulename = getModuleName(c);
			SideMenus a = c.getAnnotation(SideMenus.class);
			if (LOGREGISTERS) log().config("Registering side menu " + modulename + "/" + a.name());
			((ModuleAnnotation) Modules.get(null, modulename)).setSideMenu(null, new SideMenuAnnotation(a));
		}
		for (Method m : ClassTools.getAnnotatedMethods(SideSubMenus.class)) {
			if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) {
				String modulename = getModuleName(m.getDeclaringClass());
				SideSubMenus a = m.getAnnotation(SideSubMenus.class);
				if (LOGREGISTERS)
					log().config("Registering side sub menu " + modulename + "/" + a.name());
				((ModuleAnnotation) Modules.get(null, modulename)).registerSideSubMenu(null, new SideSubMenuAnnotation(m));
			}
		}
	}

	private static void loadTemplates() {
		for (Method m : ClassTools.getAnnotatedMethods(Template.class)) {
			if (m.getDeclaringClass().getName().startsWith("net.coagulate.GPHUD.Modules")) {
				String modulename = getModuleName(m.getDeclaringClass());
				Template a = m.getAnnotation(Template.class);
				if (LOGREGISTERS) log().config("Registering template " + a.name());
				Templater.register(a, m);
			}
		}
	}

	private static void loadGSFunctions() {
		for (Method m : ClassTools.getAnnotatedMethods(GSFunctions.GSFunction.class)) {
			Annotation a = m.getAnnotation(GSFunctions.GSFunction.class);
			if (LOGREGISTERS) log().config("Registering gsFunction " + m.getName());
			GSFunctions.register(m.getName(),m);
		}
	}


	@Nonnull
	private static Logger log() {
		if (log==null) { log=GPHUD.getLogger("Classes"); }
		return log;
	}
}
