package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Pool.Pools;
import net.coagulate.GPHUD.State;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents a module.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ModuleAnnotation extends Module {

	protected final Map<String, Pool> poolmap = new TreeMap<>();
	protected final Map<String, KV> kvmap = new TreeMap<>();
	SideMenu sidemenu = null;
	final Map<String, Permission> permissions = new TreeMap<>();
	final Set<SideSubMenu> sidemenus = new HashSet<>();
	final Map<String, Command> commands = new TreeMap<>();
	final Set<URL> contents = new HashSet<>();
	private boolean generated = true;
	public ModuleAnnotation(String name, ModuleDefinition def) throws SystemException, UserException {
		super(name, def);
		if (canDisable()) {
			registerKV(new KVEnabled(this, defaultDisable() ? "false" : "true"));
		}

		generated = false;
	}

	static Object assertNotNull(Object o, String value, String type) throws UserException {
		if (o == null) {
			throw new UserException("Unable to resolve '" + value + "' to a " + type);
		}
		return o;
	}

	protected static void checkPublicStatic(Method m) throws SystemException {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be public");
		}
	}

	public boolean isGenerated() { return generated; }

	Response run(State st, String commandname, String[] args) throws UserException, SystemException {
		Command command = getCommand(st, commandname);
		return command.run(st, args);
	}

	public Set<SideSubMenu> getSideSubMenus(State st) {
		return sidemenus;
	}

	public URL getURL(State st, String url) throws UserException, SystemException {
		final boolean debug=false;
		URL liberalmatch = null;
		for (URL m : contents) {
			if (debug) { System.out.println("Comparing "+m.url().toLowerCase()+" to "+url.toLowerCase()); }
			if (m.url().toLowerCase().equals(url.toLowerCase())) {
				return m;
			}
			if (m.url().endsWith("*")) {
				String compareto = m.url().toLowerCase();
				compareto = compareto.substring(0, compareto.length() - 1);
				if (url.toLowerCase().startsWith(compareto)) {
					if (liberalmatch != null) {
						if (liberalmatch.url().length() == m.url().length()) {
							throw new SystemException("Multiple liberal matches for " + url + " - " + m.getFullName() + " conflicts with " + liberalmatch.getFullName());
						}
						if (m.url().length() > liberalmatch.url().length()) { liberalmatch = m; }
					} else {
						liberalmatch = m;
					}
				}
			}
		}
		return liberalmatch;
	}

	public Map<String, KV> getKVDefinitions(State st) {
		return kvmap;
	}

	public KV getKVDefinition(State st, String qualifiedname) throws SystemException {
		//for (String s:kvmap.keySet()) { System.out.println(s); }
		if (!kvmap.containsKey(qualifiedname.toLowerCase())) {
			throw new SystemException("Invalid KV " + qualifiedname + " in module " + getName());
		}
		return kvmap.get(qualifiedname.toLowerCase());
	}

	public Command getCommand(State st, String commandname) {
		Command c = commands.get(commandname.toLowerCase());
		if (c == null) { throw new UserException("No such command " + commandname + " in module " + this.name); }
		return c;
	}

	public void registerPool(Pool element) {
		if (poolmap.containsKey(element.name().toLowerCase())) {
			throw new SystemException("Attempt to register duplicate pool map " + element.name() + " in module " + getName());
		}
		poolmap.put(element.name().toLowerCase(), element);
	}

	public Pool getPool(State st, String itemname) {
		return poolmap.get(itemname.toLowerCase());
	}

	public Permission getPermission(State st, String itemname) {
		return permissions.get(itemname.toLowerCase());
	}

	public void registerCommand(Command m) throws SystemException, UserException {
		commands.put(m.getName().toLowerCase(), m);
	}


	public Map<String, Pool> getPoolMap(State st) {
		return poolmap;
	}

	public boolean hasPool(State st, Pools p) {
		if (poolmap.containsValue(p)) {
			return true;
		}
		return false;
	}

	public Map<String, Command> getCommands(State st) throws UserException, SystemException {
		return commands;
	}

	public void setSideMenu(State st, SideMenu a) throws UserException, SystemException {
		if (!a.requiresPermission().isEmpty()) {
			Modules.validatePermission(st, a.requiresPermission());
		}
		if (Modules.getURL(null, a.url()) == null) {
			throw new SystemException("Side menu definition " + a.name() + " references url " + a.url() + " which can not be found");
		}
		if (sidemenu != null) {
			throw new SystemException("Attempt to replace side menu detected - is " + sidemenu.name() + " but replacing with " + a.name());
		}
		sidemenu = a;
	}

	public void registerSideSubMenu(State st, SideSubMenu m) throws SystemException, UserException {
		if (!m.requiresPermission().isEmpty()) {
			Modules.validatePermission(st, m.requiresPermission());
		}
		// things like check public static + args will be checked by @URLs processing, which we check will (have) happen(ed) here
		sidemenus.add(m);
	}

	public Map<String, Permission> getPermissions(State st) {
		return permissions;
	}

	public SideMenu getSideMenu(State st) {
		return sidemenu;
	}

	public void registerPermission(Permission a) {
		if (permissions.containsKey(a.name().toLowerCase())) {
			throw new SystemException("Attempt to redefine permission " + a.name() + " in module " + name);
		}
		permissions.put(a.name().toLowerCase(), a);
	}

	public void registerContent(URL m) throws UserException, SystemException {
		//System.out.println("Module "+name+" gets URL "+m.url()+" mapping to "+m.getMethodName());
		contents.add(m);
	}

	public Set<URL> getAllContents(State st) {
		return contents;
	}

	public void registerKV(KV a) throws UserException {
		if (kvmap.containsKey(a.name().toLowerCase())) {
			throw new SystemException("Attempt to redefine KV entry " + a.name() + " in module " + name);
		}
		kvmap.put(a.name().toLowerCase(), a);
	}

	public void validateKV(State st, String key) throws SystemException {
		if (Modules.getKVDefinition(st, key) == null) {
			throw new SystemException("KV key " + key + " in module " + getName() + " does not exist");
		}
	}

	public void validateCommand(State st, String command) throws SystemException {
		if (!commands.containsKey(command.toLowerCase())) {
			throw new SystemException("Command " + command + " does not exist in module " + getName());
		}
	}


	protected void initialiseInstance(State st) {
		//no-op by default
	}

}
