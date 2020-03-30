package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
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

	protected final Map<String,Pool> poolmap=new TreeMap<>();
	protected final Map<String,KV> kvmap=new TreeMap<>();
	final Map<String,Permission> permissions=new TreeMap<>();
	final Set<SideSubMenu> sidemenus=new HashSet<>();
	final Map<String,Command> commands=new TreeMap<>();
	final Set<URL> contents=new HashSet<>();
	private final boolean generated;
	@Nullable
	SideMenu sidemenu;

	public ModuleAnnotation(final String name,
	                        final ModuleDefinition def) {
		super(name,def);
		if (canDisable()) {
			registerKV(new KVEnabled(this,defaultDisable()?"false":"true"));
		}

		generated=false;
	}

	@Nonnull
	static Object assertNotNull(@Nullable final Object o,
	                            final String value,
	                            final String type) {
		if (o==null) {
			throw new UserInputLookupFailureException("Unable to resolve '"+value+"' to a "+type);
		}
		return o;
	}

	protected static void checkPublicStatic(@Nonnull final Method m) {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemImplementationException("Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemImplementationException("Method "+m.getDeclaringClass().getName()+"/"+m.getName()+" must be public");
		}
	}

	public boolean isGenerated() { return generated; }

	@Nonnull
	Response run(@Nonnull final State st,
	             @Nonnull final String commandname,
	             @Nonnull final String[] args) {
		final Command command=getCommandNullable(st,commandname);
		return command.run(st,args);
	}

	@Nullable
	public Set<SideSubMenu> getSideSubMenus(final State st) {
		return sidemenus;
	}

	@Nullable
	public URL getURL(final State st,
	                  @Nonnull final String url) {
		final boolean debug=false;
		URL liberalmatch=null;
		for (final URL m: contents) {
			if (m.url().toLowerCase().equals(url.toLowerCase())) {
				return m;
			}
			if (m.url().endsWith("*")) {
				String compareto=m.url().toLowerCase();
				compareto=compareto.substring(0,compareto.length()-1);
				if (url.toLowerCase().startsWith(compareto)) {
					if (liberalmatch!=null) {
						if (liberalmatch.url().length()==m.url().length()) {
							throw new SystemImplementationException("Multiple liberal matches for "+url+" - "+m.getFullName()+" conflicts with "+liberalmatch.getFullName());
						}
						if (m.url().length()>liberalmatch.url().length()) { liberalmatch=m; }
					}
					else {
						liberalmatch=m;
					}
				}
			}
		}
		return liberalmatch;
	}

	@Nonnull
	public Map<String,KV> getKVDefinitions(final State st) {
		return kvmap;
	}

	public KV getKVDefinition(final State st,
	                          @Nonnull final String qualifiedname) {
		final KV ret=getKVDefinitionNullable(st,qualifiedname);
		if (ret==null) { throw new SystemImplementationException("Invalid KV "+qualifiedname+" in module "+getName()); }
		return ret;
	}

	@Nullable
	public KV getKVDefinitionNullable(final State st,
	                          @Nonnull final String qualifiedname) {
		//for (String s:kvmap.keySet()) { System.out.println(s); }
		if (!kvmap.containsKey(qualifiedname.toLowerCase())) {
			return null;
		}
		return kvmap.get(qualifiedname.toLowerCase());
	}


	@Nonnull
	public Command getCommandNullable(final State st,
	                                  @Nonnull final String commandname) {
		final Command c=commands.get(commandname.toLowerCase());
		if (c==null) { throw new UserInputLookupFailureException("No such command "+commandname+" in module "+name); }
		return c;
	}

	public void registerPool(@Nonnull final Pool element) {
		if (poolmap.containsKey(element.name().toLowerCase())) {
			throw new SystemImplementationException("Attempt to register duplicate pool map "+element.name()+" in module "+getName());
		}
		poolmap.put(element.name().toLowerCase(),element);
	}

	@Nonnull
	public Pool getPool(final State st,
	                    @Nonnull final String itemname) {
		final Pool p=poolmap.get(itemname.toLowerCase());
		if (p==null) {
			throw new UserInputLookupFailureException("There is no pool named "+itemname+" in module "+getName());
		}
		return p;
	}

	public Permission getPermission(final State st,
	                                @Nonnull final String itemname) {
		return permissions.get(itemname.toLowerCase());
	}

	public void registerCommand(@Nonnull final Command m) {
		commands.put(m.getName().toLowerCase(),m);
	}


	@Nonnull
	public Map<String,Pool> getPoolMap(final State st) {
		return poolmap;
	}

	/* this function is garbage and unused, poolmap doesn't map to "Pools" but "Pool"...
	public boolean hasPool(final State st, final Pools p) {
		return poolmap.containsValue(p);
	}*/

	@Nonnull
	public Map<String,Command> getCommands(final State st) {
		return commands;
	}

	public void setSideMenu(final State st,
	                        @Nonnull final SideMenu a) {
		if (!a.requiresPermission().isEmpty()) {
			Modules.validatePermission(st,a.requiresPermission());
		}
		if (Modules.getURLNullable(null,a.url())==null) {
			throw new SystemImplementationException("Side menu definition "+a.name()+" references url "+a.url()+" which can not be found");
		}
		if (sidemenu!=null) {
			throw new SystemImplementationException("Attempt to replace side menu detected - is "+sidemenu.name()+" but replacing with "+a.name());
		}
		sidemenu=a;
	}

	public void registerSideSubMenu(final State st,
	                                @Nonnull final SideSubMenu m) {
		if (!m.requiresPermission().isEmpty()) {
			Modules.validatePermission(st,m.requiresPermission());
		}
		// things like check public static + args will be checked by @URLs processing, which we check will (have) happen(ed) here
		sidemenus.add(m);
	}

	public Map<String,Permission> getPermissions(final State st) {
		return permissions;
	}

	@Nullable
	public SideMenu getSideMenu(final State st) {
		return sidemenu;
	}

	public void registerPermission(@Nonnull final Permission a) {
		if (permissions.containsKey(a.name().toLowerCase())) {
			throw new SystemImplementationException("Attempt to redefine permission "+a.name()+" in module "+name);
		}
		permissions.put(a.name().toLowerCase(),a);
	}

	public void registerContent(final URL m) {
		//System.out.println("Module "+name+" gets URL "+m.url()+" mapping to "+m.getMethodName());
		contents.add(m);
	}

	@Nonnull
	public Set<URL> getAllContents(final State st) {
		return contents;
	}

	public void registerKV(@Nonnull final KV a) {
		if (kvmap.containsKey(a.name().toLowerCase())) {
			throw new SystemImplementationException("Attempt to redefine KV entry "+a.name()+" in module "+name);
		}
		kvmap.put(a.name().toLowerCase(),a);
	}

	public void validateKV(@Nonnull final State st,
	                       @Nonnull final String key) {
		if (Modules.getKVDefinitionNullable(st,key)==null) {
			throw new UserInputLookupFailureException("KV key "+key+" in module "+getName()+" does not exist");
		}
	}

	public void validateCommand(final State st,
	                            @Nonnull final String command) {
		if (!commands.containsKey(command.toLowerCase())) {
			throw new SystemImplementationException("Command "+command+" does not exist in module "+getName());
		}
	}


	protected void initialiseInstance(final State st) {
		//no-op by default
	}

}
