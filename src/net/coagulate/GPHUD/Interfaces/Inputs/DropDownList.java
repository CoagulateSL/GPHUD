package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
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
	final Map<String, String> choices = new TreeMap<>();

	public DropDownList(final String name) {
		this.name = name;
		add("");
	}

	@Nonnull
	public static DropDownList getCommandsList(final State st, final String name) { return getCommandsList(st, name, true); }

	@Nonnull
	public static DropDownList getCommandsList(final State st, final String name, final boolean allowgenerated) {
		final DropDownList commands = new DropDownList(name);
		for (final Module mod : Modules.getModules()) {
			for (final Command c : mod.getCommands(st).values()) {
				if (allowgenerated || !c.isGenerated()) {
					commands.add(c.getFullName(), c.getFullName() + " - " + c.description());
				}
			}
		}
		return commands;
	}

	public void add(final String choice) { choices.put(choice, choice); }

	public void add(final String choice, final String label) { choices.put(choice, label); }

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		final StringBuilder r = new StringBuilder();
		r.append("<select name=\"").append(name).append("\"");
		if (submitonchange) { r.append("onchange=\"this.form.submit()\""); }
		r.append(">");
		for (final Map.Entry<String, String> entry : choices.entrySet()) {
			final String option = entry.getKey();
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

	boolean submitonchange;
	@Nonnull
	public DropDownList submitOnChange() { submitonchange=true; return this;}
}
