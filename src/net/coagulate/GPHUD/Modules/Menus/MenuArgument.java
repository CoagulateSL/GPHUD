package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the argument of a menu (aka the CHOICE element)
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MenuArgument extends Argument {
	final Command command;
	final JSONObject meta;
	@Nullable
	String override;

	public MenuArgument(final Command command, final JSONObject definition) {
		super();
		this.command = command;
		meta = definition;
	}

	@Nonnull
	public List<String> getChoices(final State st) {
		final List<String> options = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			if (meta.has("button" + i)) {
				options.add(meta.getString("button" + i));
			}
		}
		return options;
	}

	@Nonnull
	@Override
	public String description() {
		if (override != null) { return override; }
		return "Choice of menu item";
	}

	@Nonnull
	@Override
	public String getName() {
		return "choice";
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public boolean mandatory() {
		return true;
	}

	@Nonnull
	@Override
	public ArgumentType type() {
		return ArgumentType.CHOICE;
	}

	@Nonnull
	@Override
	public Class<String> objectType() {
		return String.class;
	}

	@Override
	public boolean delayTemplating() {
		return false;
	}

	@Override
	public void overrideDescription(final String n) {
		override = n;
	}

	@Override
	public int max() {
		return 24; // i think
	}


}
