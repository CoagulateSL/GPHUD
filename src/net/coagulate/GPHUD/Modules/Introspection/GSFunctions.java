package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
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
	public static void renderCommand(@Nonnull State st, SafeMap values) throws UserException, SystemException {
		String uri = st.getDebasedURL();
		if (!uri.startsWith("/introspection/gsfunctions/")) { throw new SystemException("URL Misconfiguratin?"); }
		uri = uri.substring("/introspection/gsfunctions/".length());

		// if we get here, we're investigating a specific command
		Form f = st.form();
		String proposedcommand = uri;
		proposedcommand = proposedcommand.replaceAll("/", ".");
		proposedcommand = proposedcommand.replaceAll("[^A-Za-z0-9\\.]", "");  // limited character set.  XSS protect etc blah blah tainted user input blah

		Method method = net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.get(proposedcommand);
		if (method == null) {
			f.add(new TextError("FUNCTION NOT FOUND!"));
			return;
		}
		net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction meta=method.getAnnotation(net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction.class);
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
	public static void APIIndex(@Nonnull State st, SafeMap values) {
		Form f = st.form();
		Table t = new Table();
		//t.border(true);
		f.add(t);
		for (String functionname: net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.getAll().keySet()) {
			t.openRow();
			try {
				Method function= net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.get(functionname);
				t.openRow();
				net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction meta=function.getAnnotation(net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction.class);
				t.add("<a href=\"/GPHUD/introspection/gsfunctions/" + functionname + "\">" + functionname + "</a>");
				t.add(meta.description());

			} catch (Exception e) { t.add("ERR:" + e.toString()); }
		}
	}

}
