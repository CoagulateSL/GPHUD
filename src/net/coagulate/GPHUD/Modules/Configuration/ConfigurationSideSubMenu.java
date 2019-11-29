package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.SideSubMenu;

/**
 * Config submenu elements.
 *
 * @author Iain Price <gphuid@predestined.net>
 */
public class ConfigurationSideSubMenu extends SideSubMenu {
	int priority = 999;
	final Module m;

	public ConfigurationSideSubMenu(Module m) {
		this.m = m;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public String name() {
		return m.getName();
	}

	@Override
	public int priority() {
		return priority;
	}

	@Override
	public String requiresPermission() {
		return "";
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String getURL() {
		return "/configuration/" + m.getName();
	}


}
