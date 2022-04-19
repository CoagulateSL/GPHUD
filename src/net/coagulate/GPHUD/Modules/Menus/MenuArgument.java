package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.PermissionsGroup;
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

	public MenuArgument(final Command command,
	                    final JSONObject definition) {
        this.command = command;
		meta = definition;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public ArgumentType type() {
		return ArgumentType.CHOICE;
	}

	@Nonnull
	@Override
	public String description() {
		if (override!=null) { return override; }
		return "Choice of menu item";
	}

	@Override
	public boolean mandatory() {
		return true;
	}

	@Nonnull
	@Override
	public Class<String> objectType() {
		return String.class;
	}

	@Nonnull
	@Override
	public String name() {
		return "choice";
	}

	@Override
	public boolean delayTemplating() {
		return false;
	}

	@Override
	public int max() {
		return 24; // i think
	}

	@Override
	public void overrideDescription(final String n) {
		override=n;
	}

	@Nonnull
	public List<String> getChoices(final State st) {
		final List<String> options=new ArrayList<>();
		for (int i = 1; i<=MenuModule.MAX_BUTTONS; i++) {

			if (meta.has("button"+i)) {
				boolean haspermissions=false;
				boolean passespermission=false;
				if (meta.has("permission"+i) && st.getAvatarNullable()!=null) {
					haspermissions=true;
					if (st.hasPermission(meta.getString("permission"+i))) { passespermission=true; }
				}
				if (meta.has("permissiongroup"+i)) {
					haspermissions=true;
					final PermissionsGroup pg=PermissionsGroup.resolveNullable(st,meta.getString("permissiongroup"+i));
					if (pg!=null && st.getAvatarNullable()!=null) {
						if (pg.hasMember(st.getAvatar())) { passespermission=true; }
					}
				}

				if (meta.has("charactergroup"+i)) {
					haspermissions=true;
					final CharacterGroup cg=CharacterGroup.resolve(st,meta.getString("charactergroup"+i));
					if (cg!=null) {
						if (cg.hasMember(st.getCharacter())) { passespermission=true; }
					}
				}
				if (!haspermissions || passespermission) { options.add(meta.getString("button"+i)); }
			}
		}
		return options;
	}


}
