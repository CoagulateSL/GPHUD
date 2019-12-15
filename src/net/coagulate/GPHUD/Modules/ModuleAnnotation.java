package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Pool.Pools;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
	@Nullable
	SideMenu sidemenu;
	final Map<String, Permission> permissions = new TreeMap<>();
	final Set<SideSubMenu> sidemenus = new HashSet<>();
	final Map<String, Command> commands = new TreeMap<>();
	final Set<URL> contents = new HashSet<>();
	private boolean generated = true;
	public ModuleAnnotation(final String name, final ModuleDefinition def) throws SystemException, UserException {
		super(name, def);
		if (canDisable()) {
			registerKV(new KVEnabled(this, defaultDisable() ? "false" : "true"));
		}

		generated = false;
	}

	@Nullable
	static Object assertNotNull(@Nullable final Object o, final String value, final String type) throws UserException {
		if (o == null) {
			throw new UserException("Unable to resolve '" + value + "' to a " + type);
		}
		return o;
	}

	protected static void checkPublicStatic(@Nonnull final Method m) throws SystemException {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be public");
		}
	}

	public boolean isGenerated() { return generated; }

	@Nonnull
	Response run(@Nonnull final State st, @Nonnull final String commandname, @Nonnull final String[] args) throws UserException, SystemException {
		final Command command = getCommand(st, commandname);
		return command.run(st, args);
	}

	@Nullable
	public Set<SideSubMenu> getSideSubMenus(final State st) {
		return sidemenus;
	}

	@Nullable
	public URL getURL(final State st, @Nonnull final String url) throws UserException, SystemException {
		final boolean debug=false;
		URL liberalmatch = null;
		for (final URL m : contents) {
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

	@Nonnull
	public Map<String, KV> getKVDefinitions(final State st) {
		return kvmap;
	}

	public KV getKVDefinition(final State st, @Nonnull final String qualifiedname) throws SystemException {
		//for (String s:kvmap.keySet()) { System.out.println(s); }
		if (!kvmap.containsKey(qualifiedname.toLowerCase())) {
			throw new SystemException("Invalid KV " + qualifiedname + " in module " + getName());
		}
		return kvmap.get(qualifiedname.toLowerCase());
	}

	@Nullable
	public Command getCommand(final State st, @Nonnull final String commandname) {
		final Command c = commands.get(commandname.toLowerCase());
		if (c == null) { throw new UserException("No such command " + commandname + " in module " + name); }
		return c;
	}

	public void registerPool(@Nonnull final Pool element) {
		if (poolmap.containsKey(element.name().toLowerCase())) {
			throw new SystemException("Attempt to register duplicate pool map " + element.name() + " in module " + getName());
		}
		poolmap.put(element.name().toLowerCase(), element);
	}

	public Pool getPool(final State st, @Nonnull final String itemname) {
		return poolmap.get(itemname.toLowerCase());
	}

	public Permission getPermission(final State st, @Nonnull final String itemname) {
		return permissions.get(itemname.toLowerCase());
	}

	public void registerCommand(@Nonnull final Command m) throws SystemException, UserException {
		commands.put(m.getName().toLowerCase(), m);
	}


	@Nonnull
	public Map<String, Pool> getPoolMap(final State st) {
		return poolmap;
	}

	public boolean hasPool(final State st, final Pools p) {
		return poolmap.containsValue(p);
	}

	@Nonnull
	public Map<String, Command> getCommands(final State st) throws UserException, SystemException {
		return commands;
	}

	public void setSideMenu(final State st, @Nonnull final SideMenu a) throws UserException, SystemException {
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

	public void registerSideSubMenu(final State st, @Nonnull final SideSubMenu m) throws SystemException, UserException {
		if (!m.requiresPermission().isEmpty()) {
			Modules.validatePermission(st, m.requiresPermission());
		}
		// things like check public static + args will be checked by @URLs processing, which we check will (have) happen(ed) here
		sidemenus.add(m);
	}

	public Map<String, Permission> getPermissions(final State st) {
		return permissions;
	}

	@Nullable
	public SideMenu getSideMenu(final State st) {
		return sidemenu;
	}

	public void registerPermission(@Nonnull final Permission a) {
		if (permissions.containsKey(a.name().toLowerCase())) {
			throw new SystemException("Attempt to redefine permission " + a.name() + " in module " + name);
		}
		permissions.put(a.name().toLowerCase(), a);
	}

	public void registerContent(final URL m) throws UserException, SystemException {
		//System.out.println("Module "+name+" gets URL "+m.url()+" mapping to "+m.getMethodName());
		contents.add(m);
	}

	@Nonnull
	public Set<URL> getAllContents(final State st) {
		return contents;
	}

	public void registerKV(@Nonnull final KV a) throws UserException {
		if (kvmap.containsKey(a.name().toLowerCase())) {
			throw new SystemException("Attempt to redefine KV entry " + a.name() + " in module " + name);
		}
		kvmap.put(a.name().toLowerCase(), a);
	}

	public void validateKV(final State st, @Nonnull final String key) throws SystemException {
		if (Modules.getKVDefinition(st, key) == null) {
			throw new SystemException("KV key " + key + " in module " + getName() + " does not exist");
		}
	}

	public void validateCommand(final State st, @Nonnull final String command) throws SystemException {
		if (!commands.containsKey(command.toLowerCase())) {
			throw new SystemException("Command " + command + " does not exist in module " + getName());
		}
	}


	protected void initialiseInstance(final State st) {
		//no-op by default
	}

}
