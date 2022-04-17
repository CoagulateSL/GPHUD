package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A dropdown list choice.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class DropDownList extends Input {
	String name;
	final Map<String,String> choices=new TreeMap<>();
	boolean submitOnChange;

	@Nonnull
    private DropDownList clone(final String newName) {
        final DropDownList newList = new DropDownList(newName);
        newList.choices.putAll(choices);
        return newList;
    }

	public DropDownList(final String name) {
		this.name=name;
		add("");
	}

	// ---------- STATICS ----------
	@Nonnull
	public static DropDownList getCommandsList(final State st,
	                                           final String name) { return getCommandsList(st,name,true); }

	private static final Cache<Instance,DropDownList> generatedCommandsCache=Cache.getCache("gphud/gencmddropdown", CacheConfig.SHORT);
	private static final Cache<Instance,DropDownList> nongeneratedCommandsCache=Cache.getCache("gphud/nongencmddropdown", CacheConfig.SHORT);
	@Nonnull
	public static DropDownList getCommandsList(final State st,
	                                           final String name,
	                                           final boolean allowgenerated) {
		Cache<Instance,DropDownList> targetCache=nongeneratedCommandsCache;
		if (allowgenerated) { targetCache=generatedCommandsCache; }
		return targetCache.get(st.getInstance(),()->{
			final DropDownList commands=new DropDownList(name);
			for (final Module mod: Modules.getModules()) {
				for (final Command c: mod.getCommands(st).values()) {
					if (allowgenerated || !c.isGenerated()) {
						commands.add(c.getFullName(),c.getFullName()+" - "+c.description());
					}
				}
			}
			return commands;
		}).clone(name);
	}

	private static final Cache<Instance,DropDownList> permissionsCache=Cache.getCache("gphud/permissionsdropdown",CacheConfig.SHORT);
	public static DropDownList getPermissionsList(final State st,
	                                              final String name) {
		return permissionsCache.get(st.getInstance(),()-> {
			final DropDownList permissions = new DropDownList(name);
			for (final Module mod : Modules.getModules()) {
				for (final Permission p : mod.getPermissions(st).values()) {
                    try {
                        permissions.add(mod.getName() + "." + p.name(), mod.getName() + "." + p.name());
                    } catch (final NoDataException ignored) {
                    } // the attribute went away
				}
			}
			return permissions;
		}).clone(name);
	}

	private static final Cache<Instance,DropDownList> permissionsGroupsCache=Cache.getCache("gphud/permissionsgroupsdropdown",CacheConfig.SHORT);
	public static DropDownList getPermissionsGroups(final State st,
	                                                final String name) {
		return permissionsGroupsCache.get(st.getInstance(),()-> {
			final DropDownList permissions = new DropDownList(name);
			for (final Module mod : Modules.getModules()) {
				for (final PermissionsGroup p : PermissionsGroup.getPermissionsGroups(st)) {
					permissions.add(p.getName(), p.getName());
				}
			}
			return permissions;
		}).clone(name);
	}

	private static final Cache<Instance,DropDownList> charGroupsCache=Cache.getCache("gphud/chargroupsdropdown",CacheConfig.SHORT);
	public static DropDownList getCharacterGroups(final State st,
	                                              final String name) {
		return charGroupsCache.get(st.getInstance(),()-> {
			final DropDownList charactergroups = new DropDownList(name);
			for (final Module mod : Modules.getModules()) {
				for (final CharacterGroup cg : st.getInstance().getCharacterGroups()) {
					charactergroups.add(cg.getName(), cg.getName());
				}
			}
			return charactergroups;
		}).clone(name);
	}

	private static final Cache<Instance,DropDownList> scriptsCache=Cache.getCache("gphud/scriptsdropdown",CacheConfig.SHORT);

    public static DropDownList getScriptsList(final State st, final String name) {
        return scriptsCache.get(st.getInstance(), () -> {
            final DropDownList scriptsList = new DropDownList(name);
            final Set<Script> list = Script.getScripts(st.getInstance());
            for (final Script script : list) {
                scriptsList.add(script.getName(), script.getName());
            }
            return scriptsList;
        }).clone(name);
    }

    // ---------- INSTANCE ----------
	public void add(final String choice) { choices.put(choice,choice); }

	public void add(final String choice,
	                final String label) { choices.put(choice,label); }

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		final StringBuilder r=new StringBuilder();
		r.append("<select name=\"").append(name).append("\"");
		if (submitOnChange) { r.append("onchange=\"this.form.submit()\" "); }
		if (!onchange.isBlank()) { r.append("onchange=\"").append(onchange).append("\" "); }
		if (!id.isBlank()) { r.append("id=\"").append(id).append("\" "); }
		r.append(">");
		for (final Map.Entry<String,String> entry: choices.entrySet()) {
			final String option=entry.getKey();
			r.append("<option value=\"").append(option).append("\"");
			if (option.equalsIgnoreCase(value)) { r.append(" selected"); }
			r.append(">").append(entry.getValue()).append("</option>");
		}
		r.append("</select>");
		return r.toString();
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	public void name(@Nonnull final String newName) { name=newName; }

	@Nonnull
	public DropDownList submitOnChange() {
		submitOnChange =true;
		return this;
	}
	String onchange="";
	@Nonnull public DropDownList javascriptOnChange(@Nonnull final String javascript) {
		onchange=javascript;
		return this;
	}
	String id="";

    @Nonnull
    public DropDownList id(final String newId) {
        this.id = newId;
        return this;
    }
}
