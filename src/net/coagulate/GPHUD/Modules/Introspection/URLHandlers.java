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

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.TreeMap;

/**
 * URLs Introspection.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class URLHandlers {
	// ---------- STATICS ----------
	@URLs(url="/introspection/urlhandlers",
		  requiresPermission = "User.SuperAdmin")
	@SideSubMenus(name="URL Handlers",
	              requiresPermission = "User.SuperAdmin",
	              priority=99)
	public static void createForm(@Nonnull final State st,
	                              final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("URL Handler registrations"));
		if (!st.isSuperUser()) {
			f.add(new TextError("Sorry, this information is classified."));
			return;
		}
		final Table t=new Table();
		f.add(t);
		t.add(new HeaderRow().add("URL").add("Method"));
		final Map<String,Row> output=new TreeMap<>();
		for (final Module module: Modules.getModules()) {
			for (final URL url: module.getAllContents(st)) {
				final Row writeup=new Row();
				writeup.add(url.url());
				writeup.add(url.getMethodName());
				writeup.add(url.requiresPermission());
				output.put(url.url(),writeup);
			}
		}
		for (final Row row: output.values()) {
			t.add(row);
		}
	}
}
