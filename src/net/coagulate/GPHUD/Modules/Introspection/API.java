package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * API Introspection.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class API {
	@URLs(url = "/introspection/api/*")
	public static void renderCommand(@Nonnull State st, SafeMap values) throws UserException, SystemException {
		String uri = st.getDebasedURL();
		if (!uri.startsWith("/introspection/api/")) { throw new SystemException("URL Misconfiguratin?"); }
		uri = uri.substring("/introspection/api/".length());

		// if we get here, we're investigating a specific command
		Form f = st.form();
		String proposedcommand = uri;
		proposedcommand = proposedcommand.replaceAll("/", ".");
		proposedcommand = proposedcommand.replaceAll("[^A-Za-z0-9\\.]", "");  // limited character set.  XSS protect etc blah blah tainted user input blah

		Command c = Modules.getCommandNullable(st, proposedcommand);
		if (c == null) {
			f.add(new TextError("COMMAND NOT FOUND!"));
			return;
		}
		f.add(new TextHeader(c.getFullName()));
		// ooh...
		f.add(new TextSubHeader("Description"));
		f.add(c.description());
		f.add(new TextSubHeader("Arguments"));
		Table args = new Table();
		for (Argument p : c.getArguments()) {
			f.add("<b>" + p.getName() + "</b>");
			f.add(" - Type: " + p.type());
			if (p.type() == ArgumentType.CHOICE) {
				String[] split = proposedcommand.split("\\.");
				StringBuilder options = new StringBuilder();
				for (String s : p.getChoices(st)) {
					if (options.length() > 0) { options.append(", "); }
					options.append(s);
				}
				f.add(" [" + options + "]");
			}
			f.add(" - Mandatory: " + p.mandatory());
			f.add(" - " + p.description());
			f.add("<br>");
		}
		if (c.requiresPermission() != null && !c.requiresPermission().isEmpty()) {
			f.add(new TextSubHeader("Permission Required to Invoke"));
			f.add(c.requiresPermission());
		}
		f.add(new TextSubHeader("Operational Context"));
		f.add(c.context().toString());
		f.add(new TextSubHeader("Interface Access"));
		if (!c.permitConsole()) { f.add(new Color("red", "Console access denied")); } else {
			f.add(new Color("green", "Accessible via console"));
		}
		f.add("<br>");
		if (!c.permitJSON()) { f.add(new Color("red", "LSL script access denied")); } else {
			f.add(new Color("green", "Accessible via LSL/JSON"));
		}
		f.add("<br>");
		if (!c.permitUserWeb()) { f.add(new Color("red", "Browser Web UI access denied")); } else {
			f.add(new Color("green", "Accessible via Browser Web UI"));
		}
		f.add("<br>");
		if (!c.permitScripting()) { f.add(new Color("red", "Scripting access denied")); } else {
			f.add(new Color("green", "Accessible via Scripting module"));
		}
		f.add("<br>");
		if (!c.permitObject()) { f.add(new Color("red", "Object access denied")); } else {
			f.add(new Color("green", "Accessible via Object API"));
		}
		f.add("<br>");
		f.add("<br>");
		f.add(new TextSubHeader("Target Method"));
		f.add(c.getFullMethodName());
	}

	@URLs(url = "/introspection/api/")
	@SideSubMenus(name = "API", priority = 1)
	public static void APIIndex(@Nonnull State st, SafeMap values) {
		Form f = st.form();
		Table t = new Table();
		//t.border(true);
		f.add(t);
		for (Module m : Modules.getModules()) {
			t.add(new HeaderRow().add(new Cell(new TextSubHeader(m.getName()), 999)));
			Map<String, Command> commands = m.getCommands(st);
			for (Map.Entry<String, Command> entry : commands.entrySet()) {
				t.openRow();
				try {
					Command c = entry.getValue();
					t.openRow();
					t.add(c.context().toString());
					t.add("<a href=\"/GPHUD/introspection/api/" + m.getName() + "/" + entry.getKey() + "\">" + c.getName() + "</a>");
					t.add(c.description());
					if (c.requiresPermission().isEmpty()) { t.add(""); } else {
						t.add(" [ " + c.requiresPermission() + " ] ");
					}
					if (c.permitConsole()) { t.add(new Color("green", "Console")); } else {
						t.add(new Color("red", "Console"));
					}
					if (c.permitJSON()) { t.add(new Color("green", "JSON")); } else { t.add(new Color("red", "JSON")); }
					if (c.permitUserWeb()) { t.add(new Color("green", "UserWeb")); } else {
						t.add(new Color("red", "UserWeb"));
					}
					if (c.permitScripting()) { t.add(new Color("green", "Scripting")); } else {
						t.add(new Color("red", "Scripting"));
					}
					if (c.permitObject()) { t.add(new Color("green", "Object")); } else {
						t.add(new Color("red", "Object"));
					}
					if (c.isGenerated()) { t.add(new Color("blue", "Instance Specific")); } else { t.add(""); }
				} catch (Exception e) { t.add("ERR:" + e.toString()); }
			}
			t.openRow();
			t.add(new Cell(new Separator(), 999));
		}
	}

}
