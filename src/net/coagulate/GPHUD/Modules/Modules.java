package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static java.util.logging.Level.SEVERE;

/**
 * Static superclass that handles all the modules and delegating things around, accumulating info, etc.
 * Groups functionality into presented configuration pages.
 *
 * @author iain
 */
public abstract class Modules {
	static final Map<String, Module> modules = new TreeMap<>();

	static void register(@Nonnull Module mod) throws SystemException {
		String name = mod.getName();
		if (modules.containsKey(name.toLowerCase())) {
			throw new SystemException("Duplicate Module definition for " + name);
		}
		modules.put(name.toLowerCase(), mod);
	}

	/**
	 * Get a list of all modules registered.
	 *
	 * @return
	 */
	@Nonnull
	public static List<Module> getModules() {
		return new ArrayList<>(modules.values());
	}

	/**
	 * Check a given module exists.
	 *
	 * @param modulename
	 * @return
	 */
	public static boolean exists(@Nonnull String modulename) { return modules.containsKey(modulename.toLowerCase()); }

	/**
	 * Get a module, optionally checking if its enabled.
	 *
	 * @param st              Session state, used for enablement check, or null to skip enablement check.
	 * @param nameorreference A name of a module, or a reference (e.g. module.reference)
	 * @return the Module
	 * @throws UserException if the module doesn't exist, or isn't enabled.
	 */
	public static Module get(@Nullable State st, @Nonnull String nameorreference) throws UserException, SystemException {
		Module m = modules.get(extractModule(nameorreference).toLowerCase());
		if (st != null && !m.isEnabled(st)) {
			throw new UserException("Module " + m.getName() + " is not enabled in this instance");
		}
		if (st == null) { return m; }
		// check dependancies
		if (m.requires(st).isEmpty()) { return m; }
		String[] dependancies = m.requires(st).split(",");
		for (String dependancy : dependancies) {
			Module m2 = get(null, dependancy);
			if (!m2.isEnabled(st)) {
				throw new UserException("Module " + m.getName() + " is not enabled in this instance because it depends on " + m2.getName() + " which is disabled");
			}
		}
		return m;
	}

	/**
	 * Extracts a module name from a name or reference.
	 *
	 * @param qualified Name in format of "module" or "module.reference"
	 * @return The module section of the name
	 * @throws UserException if the module does not exist or the input is in invalid format
	 */
	public static String extractModule(@Nonnull String qualified) throws UserException {
		//System.out.println("QUALIFIED:"+qualified);
		String[] parts = qualified.split("\\.");
		if (parts.length < 1 || parts.length > 2) {
			throw new UserException("Invalid format, must be module or module.reference but we received " + qualified);
		}
		String name = parts[0];
		//System.out.println("NAME:"+name);
		if (!modules.containsKey(name.toLowerCase())) {
			throw new UserException("Module " + name + " does not exist.");
		}
		return name;
	}

	/**
	 * Extracts the second part of a reference.
	 *
	 * @param qualified A reference (module.reference)
	 * @return The reference part
	 * @throws UserException if the module does not exist or the input is in invalid format
	 */
	public static String extractReference(@Nonnull String qualified) throws UserException {
		String[] parts = qualified.split("\\.");
		if (parts.length != 2) {
			throw new UserException("Invalid format, must be module.reference but we received " + qualified);
		}
		extractModule(qualified); // validates the module
		return parts[1];
	}

	// validate a KV mapping exists
	public static void validateKV(State st, @Nonnull String key) throws UserException, SystemException {
		get(null, key).validateKV(st, key);
	}

	@Nullable
	public static URL getURL(State st, @Nonnull String url) throws UserException, SystemException {
		return getURL(st, url, true);
	}

	/**
	 * Gets a page by URL.
	 *
	 * @param st        Session state
	 * @param url       URL
	 * @param exception True to exception on unknown URL, otherwise returns null
	 * @return URL object, or null (if exception==false)
	 * @throws UserException   on page not found if and only if exception is true
	 * @throws SystemException if multiple URLs match (internal error)
	 */
	@Nullable
	public static URL getURL(State st, @Nonnull String url, boolean exception) throws UserException, SystemException {
		final boolean debug=false;
		if (url.toLowerCase().startsWith("/gphud/")) { url = url.substring(6); }
		URL literal = null;
		URL relaxed = null;
		for (Module mod : modules.values()) {
			URL proposed = mod.getURL(st, url);
			if (proposed != null) {
				if (proposed.url().endsWith("*")) {
					if (relaxed != null) {
						// break if matching prefix length, otherwise...
						if (relaxed.url().length() == proposed.url().length()) {
							throw new SystemException("Multiple relaxed matches between " + proposed.url() + " and " + relaxed.url());
						}
						if (relaxed.url().length() < proposed.url().length()) {
							relaxed = proposed;
						}  // pick longer prefix
					} else { relaxed = proposed; }
				} else {
					if (literal != null) {
						throw new SystemException("Multiple literal matches between " + proposed.url() + " and " + literal.url());
					}
					literal = proposed;
				}
			}
		}
		// if there's a strict match
		if (literal != null) { return literal; }
		// otherwise the relaxed match
		if (relaxed != null) { return relaxed; }
		// if not then its a 404.  do we exception or return null?
		if (exception) { throw new UserException("404 // Page not found [" + url + "]"); }
		return null;
	}

	public static void validatePermission(State st, @Nonnull String requirespermission) throws UserException, SystemException {
		get(null, requirespermission).validatePermission(st, extractReference(requirespermission));
	}

	@Nullable
	public static Command getCommandNullable(State st, @Nonnull String proposedcommand) throws UserException, SystemException {
		return get(st, proposedcommand).getCommand(st, extractReference(proposedcommand));
	}

	@Nonnull
	public static Command getCommand(State st, @Nonnull String proposedcommand) throws UserException, SystemException {
		Command c=getCommandNullable(st,proposedcommand);
		if (c==null) { throw new UserException("Unable to locate command "+proposedcommand); }
		return c;
	}

	public static void getHtmlTemplate(@Nonnull State st, @Nonnull String qualifiedcommandname) throws UserException, SystemException {
		get(st, qualifiedcommandname).getCommand(st, extractReference(qualifiedcommandname)).getHtmlTemplate(st);
	}

	@Nonnull
	public static JSONObject getJSONTemplate(@Nonnull State st, @Nonnull String qualifiedcommandname) throws UserException, SystemException {
		Module module = get(st, qualifiedcommandname);
		if (module == null) { throw new UserException("Unable to resolve module in " + qualifiedcommandname); }
		Command command = module.getCommand(st, extractReference(qualifiedcommandname));
		if (command == null) { throw new UserException("Unable to resolve command in " + qualifiedcommandname); }
		return command.getJSONTemplate(st);
	}

	@Nonnull
	public static Response getJSONTemplateResponse(@Nonnull State st, @Nonnull String command) throws UserException, SystemException { return new JSONResponse(getJSONTemplate(st, command)); }

	@SuppressWarnings("fallthrough")
	public static Response run(@Nonnull State st, @Nullable String console) throws UserException, SystemException {
		if (console == null || "".equals(console)) { return new ErrorResponse("No console string supplied"); }
		String[] words = console.split(" ");
		int i = 0;
		String command = words[0].toLowerCase();
		if (command.startsWith("*")) { command = command.substring(1); }
		Command c = null;
		try { c = getCommandNullable(st, command); } catch (UserException e) {
			return new ErrorResponse("Unable to find command '" + command + "' - " + e.getLocalizedMessage());
		}
		if (c == null) { return new ErrorResponse("Failed to find command " + command); }
		if (!c.permitConsole()) {
			return new ErrorResponse("Command '" + command + "' can not be called from the console");
		}
		SafeMap parameters = new SafeMap();
		for (Argument arg : c.getArguments()) {
			i++;
			if (i >= words.length) { return new ErrorResponse("Not enough parameters supplied"); }
			String argname = arg.getName();
			if (words[i] == null || words[i].isEmpty() || "-".equals(words[i])) {
				parameters.put(argname, null);
			} else {
				boolean respectnewlines = false;
				switch (arg.type()) {
					case TEXT_MULTILINE:
						respectnewlines = true;
					default:
						if (words[i].startsWith("\"")) {
							if (words[i].endsWith("\"")) { // weird but sure
								parameters.put(argname, words[i]);
							} else {
								// is a multi word thing
								StringBuilder string = new StringBuilder();
								while (!words[i].endsWith("\"")) {
									if (string.length() > 0) { string.append(" "); }
									string.append(words[i]);
									i++;
								}
								string.append(" ").append(words[i]);
								if (!respectnewlines) {
									string = new StringBuilder(string.toString().replaceAll("\n", ""));
									string = new StringBuilder(string.toString().replaceAll("\r", ""));
								}
								string = new StringBuilder(string.toString().replaceFirst("^\"", ""));
								string = new StringBuilder(string.toString().replaceAll("\"$", ""));
								//System.out.println(string);
								parameters.put(argname, string.toString());
							}
						} else {
							// is a single word thing
							parameters.put(argname, words[i]);
						}
						break;
				}
			}
		}
		return c.run(st, parameters);
	}


	public static Response run(@Nullable State st, @Nullable String qualifiedcommandname, @Nonnull SafeMap parametermap) throws UserException, SystemException {
		if (st == null) { throw new SystemException("Null state"); }
		if (qualifiedcommandname == null) { throw new SystemException("Null command"); }
		if ("console".equalsIgnoreCase(qualifiedcommandname)) { st.source= State.Sources.CONSOLE; return run(st, parametermap.get("console")); }
		Module module = get(st, qualifiedcommandname);
		if (module == null) { throw new UserException("Unknown module in " + qualifiedcommandname); }
		Command command = module.getCommand(st, extractReference(qualifiedcommandname));
		if (command == null) { throw new UserException("Unknown command in " + qualifiedcommandname); }
		return command.run(st, parametermap);
	}

	@Nonnull
	public static Response run(@Nonnull State st, @Nonnull String qualifiedcommandname, @Nonnull List<String> args) throws UserException, SystemException {
		return run(st, qualifiedcommandname, args.toArray(new String[]{}));
	}

	@Nonnull
	public static Response run(@Nonnull State st, @Nonnull String qualifiedcommandname, @Nonnull String[] args) throws UserException, SystemException {
		return get(st, qualifiedcommandname).getCommand(st, extractReference(qualifiedcommandname)).run(st, args);
	}

	public static Permission getPermission(State st, @Nonnull String qualifiedname) throws UserException, SystemException {
		return get(st, qualifiedname).getPermission(st, extractReference(qualifiedname));
	}

	@Nonnull
	public static KV getKVDefinition(@Nonnull State st, @Nonnull String qualifiedname) throws UserException, SystemException {
		KV kv = null;
		//if (qualifiedname == null) { throw new SystemException("Null qualified name for KV definition?"); }
		if (qualifiedname.toLowerCase().endsWith(".enabled")) {
			kv = get(null, qualifiedname).getKVDefinition(st, extractReference(qualifiedname));
		} else {
			kv = get(st, qualifiedname).getKVDefinition(st, extractReference(qualifiedname));
		}
		if (kv == null) { throw new SystemException("Failed to resolve KV definition " + qualifiedname); }
		return kv;
	}

	@Nonnull
	public static Set<String> getKVList(State st) {
		Set<String> kvs = new TreeSet<>();
		for (Module m : getModules()) {
			if (m.isEnabled(st)) {
				for (String s : m.getKVDefinitions(st).keySet()) {
					kvs.add(m.getName() + "." + s);
				}
			}
		}
		return kvs;
	}

	@Nonnull
	public static Set<KV> getKVSet(State st) {
		Set<KV> kvs = new HashSet<>();
		for (Module m : getModules()) {
			if (m.isEnabled(st)) {
				Map<String, KV> getkvs = m.getKVDefinitions(st);
				kvs.addAll(getkvs.values());
			}
		}
		return kvs;
	}

	@Nonnull
	public static SafeMap getConveyances(@Nonnull State st) {
		SafeMap convey = new SafeMap();
		for (Module mod : getModules()) {
			try {
				if (mod.isEnabled(st)) {
					for (String key : mod.getKVDefinitions(st).keySet()) {
						KV kv = mod.getKVDefinition(st, key);
						// this seems broken?
						String value = st.getKV(kv.fullname()).value();
						if (!kv.conveyas().isEmpty()) { convey.put(kv.conveyas(), value); }
					}
				}
			} catch (Exception e) {
				SL.report("Conveyance error", e, st);
				st.logger().log(SEVERE, "Exception compiling conveyance", e);
			}

		}
		return convey;
	}

	public static Pool getPool(State st, @Nonnull String qualifiedname) throws UserException, SystemException {
		return get(st, qualifiedname).getPool(st, extractReference(qualifiedname));
	}

	public static Pool getPoolNotNull(State st, @Nonnull String qualifiedname) throws UserException, SystemException {
		Pool pool = getPool(st, qualifiedname);
		if (pool == null) { throw new UserException("Unable to find pool " + qualifiedname); }
		return pool;
	}

	public static void simpleHtml(@Nullable State st, @Nullable String command, @Nullable SafeMap values) throws UserException, SystemException {
		if (command == null) { throw new SystemException("Null command"); }
		if (values == null) { throw new SystemException("Null values"); }
		if (st == null) { throw new SystemException("Null state"); }
		Module m = get(st, command);
		if (m == null) { throw new UserException("No such module in " + command); }
		Command c = m.getCommand(st, extractReference(command));
		if (c == null) { throw new UserException("No such command in " + command); }
		c.simpleHtml(st, values);
	}

	public static void initialiseInstance(State st) {
		for (Module module : getModules()) {
			module.initialiseInstance(st);
		}
	}

	@Nonnull
	public static Map<String, KV> getKVAppliesTo(State st, TableRow dbo) {
		Map<String, KV> filtered = new TreeMap<>();
		for (Module m : getModules()) {
			Map<String, KV> fullset = m.getKVAppliesTo(st, dbo);
			filtered.putAll(fullset);
		}
		return filtered;
	}


}
