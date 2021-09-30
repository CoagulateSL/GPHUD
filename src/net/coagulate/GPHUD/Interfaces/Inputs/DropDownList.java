package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.PermissionsGroup;
import net.coagulate.GPHUD.Data.Script;
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
 * A drop down list choice.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class DropDownList extends Input {
	final String name;
	final Map<String,String> choices=new TreeMap<>();
	boolean submitonchange;

	public DropDownList(final String name) {
		this.name=name;
		add("");
	}

	// ---------- STATICS ----------
	@Nonnull
	public static DropDownList getCommandsList(final State st,
	                                           final String name) { return getCommandsList(st,name,true); }

	@Nonnull
	public static DropDownList getCommandsList(final State st,
	                                           final String name,
	                                           final boolean allowgenerated) {
		final DropDownList commands=new DropDownList(name);
		for (final Module mod: Modules.getModules()) {
			for (final Command c: mod.getCommands(st).values()) {
				if (allowgenerated || !c.isGenerated()) {
					commands.add(c.getFullName(),c.getFullName()+" - "+c.description());
				}
			}
		}
		return commands;
	}

	public static DropDownList getPermissionsList(final State st,
	                                              final String name) {
		final DropDownList permissions=new DropDownList(name);
		for (final Module mod: Modules.getModules()) {
			for (final Permission p: mod.getPermissions(st).values()) {
				try { permissions.add(mod.getName()+"."+p.name(),mod.getName()+"."+p.name()); }
				catch (NoDataException ignored) {} // the attribute went away
			}
		}
		return permissions;
	}

	public static DropDownList getPermissionsGroups(final State st,
	                                                final String name) {
		final DropDownList permissions=new DropDownList(name);
		for (final Module mod: Modules.getModules()) {
			for (final PermissionsGroup p: PermissionsGroup.getPermissionsGroups(st)) {
				permissions.add(p.getName(),p.getName());
			}
		}
		return permissions;
	}

	public static DropDownList getCharacterGroups(final State st,
	                                              final String name) {
		final DropDownList charactergroups=new DropDownList(name);
		for (final Module mod: Modules.getModules()) {
			for (final CharacterGroup cg: st.getInstance().getCharacterGroups()) {
				charactergroups.add(cg.getName(),cg.getName());
			}
		}
		return charactergroups;
	}

    public static DropDownList getScriptsList(State st, String name) {
		final DropDownList scriptsList=new DropDownList(name);
		Set<Script> list = Script.getScripts(st.getInstance());
		for (Script script:list) {
			scriptsList.add(script.getName(),script.getName());
		}
		return scriptsList;
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
		if (submitonchange) { r.append("onchange=\"this.form.submit()\" "); }
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

	@Nonnull
	public DropDownList submitOnChange() {
		submitonchange=true;
		return this;
	}
	String onchange="";
	@Nonnull public DropDownList javascriptOnChange(@Nonnull final String javascript) {
		onchange=javascript;
		return this;
	}
	String id="";
	@Nonnull public DropDownList id(String newid) { this.id=newid; return this; }
}
