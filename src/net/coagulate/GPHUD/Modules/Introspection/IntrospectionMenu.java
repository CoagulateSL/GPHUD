package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * @author iain
 */
public class IntrospectionMenu {

	@URLs(url="/introspection/")
	public static void menu(@Nonnull final State st,
	                        final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("GPHUD Introspection"));
		f.add(new TextSubHeader("Global definitions"));
		f.p("This is a developer feature that allows you to look around the internals of GPHUD.");
		f.p("Please see the documentation site at <a href=\"/Docs/GPHUD/index.php/Main_Page.html\">https://sl.coagulate.net/Docs/GPHUD/index.php/Main_Page.html</a> for more.");
		f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/api/\">API</a>"));
		f.p("The API documentation provides an overview of callable functions provided by GPHUD and further details about individual commands.");
		f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/kvmap\">Keyvalue Mappings</a>"));
		f.p("Keywords registered to the system (configuration names, attributes, etc).");
		f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/gsfunctions\">GSFunctions</a>"));
		f.p("Functions available to the scripting language GPHUDScript.");
		f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/permissions\">Permissions</a>"));
		f.p("Permission tokens registered to the system.");
		f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/templates\">Templates</a>"));
		f.p("Expandable template items registered in the system");
		if (st.isSuperUser()) {
			f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/urlhandlers\">URL Handlers</a>"));
			f.p("Classes that are hooking into URLs.");
			f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/sql\">SQL Audit</a>"));
			f.p("SQL statement count and execution times.");

		}
		f.add(new TextSubHeader("Instance specific"));
		if (st.hasModule("Experience")) {
			f.add(new TextSubHeader("<a href=\"/GPHUD/introspection/levelcurve\">Level curve</a>"));
			f.p("Shows the mapping of XP to levels currently employed at this instance");
		}
	}

}
