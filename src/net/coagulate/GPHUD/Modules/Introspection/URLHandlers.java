package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.util.Map;
import java.util.TreeMap;

/**
 * URLs Introspection.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class URLHandlers {
	@URLs(url = "/introspection/urlhandlers")
	@SideSubMenus(name = "URL Handlers", priority = 99)
	public static void createForm(State st, SafeMap values) {
		Form f = st.form;
		f.add(new TextHeader("URL Handler registrations"));
		if (!st.isSuperUser()) {
			f.add(new TextError("Sorry, this information is classified."));
			return;
		}
		Table t = new Table();
		f.add(t);
		t.add(new HeaderRow().add("URL").add("Method"));
		Map<String, Row> output = new TreeMap<String, Row>();
		for (Module module : Modules.getModules()) {
			for (URL url : module.getAllContents(st)) {
				Row writeup = new Row();
				writeup.add(url.url());
				writeup.add(url.getMethodName());
				writeup.add(url.requiresPermission());
				output.put(url.url(), writeup);
			}
		}
		for (String url : output.keySet()) {
			t.add(output.get(url));
		}
	}
}
