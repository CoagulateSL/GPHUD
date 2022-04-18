package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserConfigurationRecursionException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.JavaTools;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.GPHUD.State.Sources;
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
	static final Map<String,Module> modules=new TreeMap<>();

	// ---------- STATICS ----------

	/**
	 * Get a list of all modules registered.
	 *
	 * @return list of module objects
	 */
	@Nonnull
	public static List<Module> getModules() {
		return new ArrayList<>(modules.values());
	}

	/**
	 * Check a given module exists.
	 *
	 * @param modulename module to check for
	 *
	 * @return true if exists, false otherwise
	 */
	public static boolean exists(@Nonnull final String modulename) { return modules.containsKey(modulename.toLowerCase()); }

	/**
	 * Get a module, optionally checking if its enabled.
	 *
	 * @param st              Session state, used for enablement check, or null to skip enablement check.
	 * @param nameorreference A name of a module, or a reference (e.g. module.reference)
	 *
	 * @return the Module
	 *
	 * @throws UserException if the module doesn't exist, or isn't enabled.
	 */
	public static Module get(@Nullable final State st,
	                         @Nonnull final String nameorreference) {
		final Module m=modules.get(extractModule(nameorreference).toLowerCase());
		if (st!=null && !m.isEnabled(st)) {
			throw new UserConfigurationException("Module "+m.getName()+" is not enabled in this instance",true);
		}
		if (st==null) { return m; }
		// check dependancies
		if (m.requires(st).isEmpty()) { return m; }
		final String[] dependancies=m.requires(st).split(",");
		for (final String dependancy: dependancies) {
			final Module m2=get(null,dependancy);
			if (!m2.isEnabled(st)) {
				throw new UserConfigurationException("Module "+m.getName()+" is not enabled in this instance because it depends on "+m2.getName()+" which is disabled");
			}
		}
		return m;
	}

	/**
	 * Extracts a module name from a name or reference.
	 *
	 * @param qualified Name in format of "module" or "module.reference"
	 *
	 * @return The module section of the name
	 *
	 * @throws UserException if the module does not exist or the input is in invalid format
	 */
	public static String extractModule(@Nonnull final String qualified) {
		//System.out.println("QUALIFIED:"+qualified);
		final String[] parts=qualified.split("\\.");
		if (parts.length<1 || parts.length>2) {
			throw new UserInputValidationParseException("Invalid format, must be module or module.reference but we received "+qualified);
		}
		final String name=parts[0];
		//System.out.println("NAME:"+name);
		if (!modules.containsKey(name.toLowerCase())) {
			throw new UserInputLookupFailureException("Module '"+name+"' (from '"+qualified+"') does not exist.",true);
		}
		return name;
	}

	/**
	 * Extracts the second part of a reference.
	 *
	 * @param qualified A reference (module.reference)
	 *
	 * @return The reference part
	 *
	 * @throws UserException if the module does not exist or the input is in invalid format
	 */
	public static String extractReference(@Nonnull final String qualified) {
		final String[] parts=qualified.split("\\.");
		if (parts.length!=2) {
			throw new UserInputValidationParseException("Invalid format, must be module.reference but we received "+qualified);
		}
		extractModule(qualified); // validates the module
		return parts[1];
	}

	// validate a KV mapping exists
	public static void validateKV(final State st,
	                              @Nonnull final String key) {
		get(null,key).validateKV(st,key);
	}

	@Nonnull
	public static URL getURL(final State st,
	                         @Nonnull final String url) {
		final URL ret=getURLNullable(st,url);
		if (ret==null) { throw new UserInputLookupFailureException("404 - URL mapping was not found"); }
		return ret;
	}

	/**
	 * Gets a page by URL.
	 *
	 * @param st  Session state
	 * @param url URL
	 *
	 * @return URL object, or null (if exception==false)
	 *
	 * @throws UserException   on page not found if and only if exception is true
	 * @throws SystemException if multiple URLs match (internal error)
	 */
	@Nullable
	public static URL getURLNullable(final State st,
	                                 @Nonnull String url) {
		if (url.toLowerCase().startsWith("/gphud/")) { url=url.substring(6); }
		URL literal=null;
		URL relaxed=null;
		for (final Module mod: modules.values()) {
			final URL proposed=mod.getURL(st,url);
			if (proposed!=null) {
				if (proposed.url().endsWith("*")) {
					if (relaxed == null) {
						relaxed = proposed;
					} else {
						// break if matching prefix length, otherwise...
						if (relaxed.url().length() == proposed.url().length()) {
							throw new SystemImplementationException("Multiple relaxed matches between " + proposed.url() + " and " + relaxed.url());
						}
						if (relaxed.url().length() < proposed.url().length()) {
							relaxed = proposed;
						}  // pick longer prefix
					}
				}
				else {
					if (literal!=null) {
						throw new SystemImplementationException("Multiple literal matches between "+proposed.url()+" and "+literal.url());
					}
					literal=proposed;
				}
			}
		}
		// if there's a strict match
		if (literal!=null) { return literal; }
		// otherwise the relaxed match
		return relaxed;
		// if not then its a 404.  do we exception or return null?
	}

	public static void validatePermission(final State st,
	                                      @Nonnull final String requirespermission) {
		get(null,requirespermission).validatePermission(st,extractReference(requirespermission));
	}

	@Nullable
	public static Command getCommandNullable(final State st,
	                                         @Nonnull final String proposedcommand) {
		try {
            JavaTools.limitRecursionUserException(50);
            return get(st, proposedcommand).getCommandNullable(st, extractReference(proposedcommand));
        } catch (final UserConfigurationRecursionException e) {
            throw new UserConfigurationRecursionException(proposedcommand + " -> " + e.getLocalizedMessage(), e);
        }
	}

	@Nonnull
	public static Command getCommand(final State st,
	                                 @Nonnull final String proposedcommand) {
		final Command c=getCommandNullable(st,proposedcommand);
		if (c==null) { throw new UserInputLookupFailureException("Unable to locate command "+proposedcommand); }
		return c;
	}

	public static void getHtmlTemplate(@Nonnull final State st,
	                                   @Nonnull final String qualifiedcommandname) {
		get(st,qualifiedcommandname).getCommand(st,extractReference(qualifiedcommandname)).getHtmlTemplate(st);
	}

	@Nonnull
	public static JSONObject getJSONTemplate(@Nonnull final State st,
	                                         @Nonnull final String qualifiedcommandname) {
		final Module module=get(st,qualifiedcommandname);
		if (module==null) {
			throw new UserInputLookupFailureException("Unable to resolve module in "+qualifiedcommandname,true);
		}
		final Command command=module.getCommandNullable(st,extractReference(qualifiedcommandname));
		if (command==null) {
			throw new UserInputLookupFailureException("Unable to resolve command in "+qualifiedcommandname,true);
		}
		return command.getJSONTemplate(st);
	}

	@Nonnull
	public static Response getJSONTemplateResponse(@Nonnull final State st,
	                                               @Nonnull final String command) { return new JSONResponse(getJSONTemplate(st,command)); }

	public static Response run(@Nonnull final State st,
	                           @Nullable final String console) {
		return run(st,console,true);
	}

	@SuppressWarnings("fallthrough")
	public static Response run(@Nonnull final State st,
	                           @Nullable final String console,
	                           final boolean fromconsole) {
		if (console==null || "".equals(console)) { return new ErrorResponse("No console string supplied"); }
		final String[] words=console.split(" ");
		int i=0;
		String command=words[0].toLowerCase();
		if (command.startsWith("*")) { command=command.substring(1); }
		final Command c;
		try { c=getCommandNullable(st,command); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Unable to find command '"+command+"' - "+e.getLocalizedMessage());
		}
		if (c==null) { return new ErrorResponse("Failed to find command "+command); }
		if (fromconsole || st.source!=Sources.SYSTEM) {
			if (!c.permitConsole()) {
				return new ErrorResponse("Command '"+command+"' can not be called from the console");
			}
		}
		final SafeMap parameters=new SafeMap();
		for (final Argument arg: c.getArguments()) {
			i++;
			if (i >= words.length) { return new ErrorResponse("Not enough parameters supplied"); }
			final String argname=arg.name();
			if (words[i]==null || words[i].isEmpty() || "-".equals(words[i])) {
				parameters.put(argname,null);
			}
			else {
				boolean respectnewlines=false;
				//noinspection SwitchStatementWithTooFewBranches
				switch (arg.type()) {
					case TEXT_MULTILINE:
						respectnewlines=true;
					default:
						if (words[i].startsWith("\"")) {
							if (words[i].endsWith("\"")) { // weird but sure
								parameters.put(argname,words[i]);
							}
							else {
								// is a multi word thing
								try {
									StringBuilder string = new StringBuilder();
									while (!words[i].endsWith("\"")) {
										if (!string.isEmpty()) {
											string.append(" ");
										}
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
                                } catch (final ArrayIndexOutOfBoundsException e) {
                                    throw new UserInputValidationParseException("Failed to find closure for a string (a matching \")", e, true);
                                }
							}
						}
						else {
							// is a single word thing
							parameters.put(argname,words[i]);
						}
						break;
				}
			}
		}
		return c.run(st,parameters);
	}

	public static Response run(@Nonnull final State st,
	                           @Nonnull final String qualifiedcommandname,
	                           @Nonnull final SafeMap parametermap) {
		if ("console".equalsIgnoreCase(qualifiedcommandname)) {
			st.source=State.Sources.CONSOLE;
			return run(st,parametermap.get("console"));
		}
		final Module module=get(st,qualifiedcommandname);
		if (module==null) { throw new UserInputLookupFailureException("Unknown module in "+qualifiedcommandname,true); }
		final Command command=module.getCommandNullable(st,extractReference(qualifiedcommandname));
		if (command==null) { throw new UserInputLookupFailureException("Unknown command in "+qualifiedcommandname,true); }
		return command.run(st,parametermap);
	}

	@Nonnull
	public static Response run(@Nonnull final State st,
	                           @Nonnull final String qualifiedcommandname,
	                           @Nonnull final List<String> args) {
		return run(st,qualifiedcommandname,args.toArray(new String[]{}));
	}

	@Nonnull
	public static Response run(@Nonnull final State st,
	                           @Nonnull final String qualifiedcommandname,
	                           @Nonnull final String[] args) {
		return get(st,qualifiedcommandname).getCommand(st,extractReference(qualifiedcommandname)).run(st,args);
	}

	public static Permission getPermission(final State st,
	                                       @Nonnull final String qualifiedname) {
		return get(st,qualifiedname).getPermission(st,extractReference(qualifiedname));
	}

	@Nonnull
	public static KV getKVDefinition(@Nonnull final State st,
	                                 @Nonnull final String qualifiedname) {
		final KV kv=getKVDefinitionNullable(st,qualifiedname);
		if (kv==null) { throw new UserInputLookupFailureException("Failed to resolve KV definition "+qualifiedname,true); }
		return kv;
	}

	@Nullable
	public static KV getKVDefinitionNullable(@Nonnull final State st,
	                                         @Nonnull final String qualifiedname) {
		final KV kv;
		//if (qualifiedname == null) { throw new SystemException("Null qualified name for KV definition?"); }
		if (qualifiedname.toLowerCase().endsWith(".enabled")) {
			kv=get(null,qualifiedname).getKVDefinition(st,extractReference(qualifiedname));
		}
		else {
			kv=get(st,qualifiedname).getKVDefinition(st,extractReference(qualifiedname));
		}
		return kv;
	}

	@Nonnull
	public static Set<String> getKVList(final State st) {
		final Set<String> kvs=new TreeSet<>();
		for (final Module m: getModules()) {
			if (m.isEnabled(st)) {
				for (final String s: m.getKVDefinitions(st).keySet()) {
					kvs.add(m.getName()+"."+s);
				}
			}
		}
		return kvs;
	}

	@Nonnull
	public static Set<KV> getKVSet(final State st) {
		final Set<KV> kvs=new HashSet<>();
		for (final Module m: getModules()) {
			if (m.isEnabled(st)) {
				final Map<String,KV> getkvs=m.getKVDefinitions(st);
				kvs.addAll(getkvs.values());
			}
		}
		return kvs;
	}

	@Nonnull
	public static SafeMap getConveyances(@Nonnull final State st) {
		final SafeMap convey=new SafeMap();
		for (final Module mod: getModules()) {
			try {
				if (mod.isEnabled(st)) {
					for (final String key: mod.getKVDefinitions(st).keySet()) {
						final KV kv=mod.getKVDefinition(st,key);
						// this seems broken?
						final String value=st.getKV(kv.fullName()).value();
						if (!kv.conveyAs().isEmpty()) { convey.put(kv.conveyAs(),value); }
					}
				}
			}
			catch (@Nonnull final Exception e) {
				SL.report("Conveyance error",e,st);
				st.logger().log(SEVERE,"Exception compiling conveyance",e);
			}

		}
		return convey;
	}

	@Nullable // sad but true
	public static Pool getPoolNullable(final State st,
	                                   @Nonnull final String qualifiedname) {
		try { return getPool(st,qualifiedname); }
		catch (final UserException e) { return null; }
	}

	@Nonnull
	public static Pool getPool(final State st,
	                           @Nonnull final String qualifiedname) {
		return get(st,qualifiedname).getPool(st,extractReference(qualifiedname));
	}

	public static void simpleHtml(@Nonnull final State st,
	                              @Nonnull final String command,
	                              @Nonnull final SafeMap values) {
		final Module m=get(st,command);
		if (m==null) { throw new UserInputLookupFailureException("No such module in "+command); }
		final Command c=m.getCommandNullable(st,extractReference(command));
		if (c==null) { throw new UserInputLookupFailureException("No such command in "+command); }
		c.simpleHtml(st,values);
	}

	public static void simpleHtml(@Nonnull final State st,
								  @Nonnull final String command,
								  @Nonnull final String... params) {
        final SafeMap map = new SafeMap();
		if ((params.length % 2)==1) { throw new SystemImplementationException("Expects an even number of varargs"); }
		for (int i=0;i<params.length;i+=2) {
			map.put(params[i],params[i+1]);
		}
		simpleHtml(st,command,map);
	}

	public static void initialiseInstance(final State st) {
		for (final Module module: getModules()) {
			module.initialiseInstance(st);
		}
	}

	@Nonnull
	public static Map<String,KV> getKVAppliesTo(final State st,
	                                            final TableRow dbo) {
		final Map<String,KV> filtered=new TreeMap<>();
		for (final Module m: getModules()) {
			final Map<String,KV> fullset=m.getKVAppliesTo(st,dbo);
			filtered.putAll(fullset);
		}
		return filtered;
	}

	// ----- Internal Statics -----
	static void register(@Nonnull final Module mod) {
		final String name=mod.getName();
		if (modules.containsKey(name.toLowerCase())) {
			throw new SystemImplementationException("Duplicate Module definition for "+name);
		}
		modules.put(name.toLowerCase(),mod);
	}


}
