package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/**
 * Main page for the HUD panel.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class HUDPanel {
	@URLs(url = "/hud/gphudclient/menu")
	public static void webPanelIndex(State st, SafeMap values) {
		Form f = st.form;
		f.add("<br>");
		for (Module m : Modules.getModules()) {
			String header = "<b>" + m.getName() + " : </b>";
			for (Command c : m.getCommands(st).values()) {
				if (c.permitHUDWeb()) {
					if (c.requiresPermission().isEmpty() || st.hasPermission(c.requiresPermission())) {
						f.add(header + " <a href=\"../" + c.getFullName() + "\">[" + c.getName() + "]</a> ");
						header = "";
					}
				}
			}
			if (header.isEmpty()) { f.add("<br>"); }
		}
	}
}
