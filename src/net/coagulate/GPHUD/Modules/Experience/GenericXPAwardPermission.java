package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.State;

/**
 * @author Iain Price
 */
public class GenericXPAwardPermission extends Permission {

	private final String myname;

	public GenericXPAwardPermission(String name) { myname = name; }

	@Override
	public Module getModule(State st) {
		return Modules.get(st,"Experience");
	}

	@Override
	public boolean isGenerated() { return true; }

	@Override
	public String name() {
		return "award" + myname + "XP";
	}

	@Override
	public String description() {
		return "Allow this user to award " + myname + " XP, up to the weekly limits";
	}

	@Override
	public POWER power() {
		return POWER.LOW;
	}

	@Override
	public boolean grantable() {
		return true;
	}

}
