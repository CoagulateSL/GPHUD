package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextError;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public class GSFunctions {
	@URL.URLs(url = "/introspection/gsfunctions/*")
	public static void renderCommand(@Nonnull final State st, final SafeMap values) {
		String uri = st.getDebasedURL();
		if (!uri.startsWith("/introspection/gsfunctions/")) { throw new SystemImplementationException("URL Misconfiguratin?"); }
		uri = uri.substring("/introspection/gsfunctions/".length());

		// if we get here, we're investigating a specific command
		final Form f = st.form();
		String proposedcommand = uri;
		proposedcommand = proposedcommand.replaceAll("/", ".");
		proposedcommand = proposedcommand.replaceAll("[^A-Za-z0-9.]", "");  // limited character set.  XSS protect etc blah blah tainted user input blah

		final Method method = net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.get(proposedcommand);
		if (method == null) {
			f.add(new TextError("FUNCTION NOT FOUND!"));
			return;
		}
		final net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction meta=method.getAnnotation(net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction.class);
		f.add(new TextHeader(method.getName()));
		// ooh...
		f.add(new TextSubHeader("Return Type"));
		f.add(meta.returns());
		f.add(new TextSubHeader("Arguments"));
		f.add(meta.parameters());
		f.add(new TextSubHeader("Description"));
		f.add(meta.description());
		f.add(new TextSubHeader("Notes"));
		f.add(meta.notes());
	}

	@URL.URLs(url = "/introspection/gsfunctions")
	@SideSubMenu.SideSubMenus(name = "GSFunctions", priority = 15)
	public static void APIIndex(@Nonnull final State st, final SafeMap values) {
		final Form f = st.form();
		final Table t = new Table();
		//t.border(true);
		f.add(t);
		for (final String functionname: net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.getAll().keySet()) {
			t.openRow();
			try {
				final Method function= net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.get(functionname);
				t.openRow();
				final net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction meta=function.getAnnotation(net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction.class);
				t.add("<a href=\"/GPHUD/introspection/gsfunctions/" + functionname + "\">" + functionname + "</a>");
				t.add(meta.description());

			} catch (@Nonnull final Exception e) { t.add("ERR:" + e); }
		}
	}

}
