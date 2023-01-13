package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
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
	// ---------- STATICS ----------
	@URLs(url="/introspection/api/*")
	public static void renderCommand(@Nonnull final State st,final SafeMap values) {
		String uri=st.getDebasedURL();
		if (!uri.startsWith("/introspection/api/")) {
			throw new SystemImplementationException("URL Misconfiguratin?");
		}
		uri=uri.substring("/introspection/api/".length());
		
		// if we get here, we're investigating a specific command
		final Form f=st.form();
		String proposedcommand=uri;
		proposedcommand=proposedcommand.replaceAll("/",".");
		proposedcommand=proposedcommand.replaceAll("[^A-Za-z\\d.]",
		                                           "");  // limited character set.  XSS protect etc blah blah tainted user input blah
		
		final Command c=Modules.getCommandNullable(st,proposedcommand);
		if (c==null) {
			f.add(new TextError("COMMAND NOT FOUND!"));
			return;
		}
		f.add(new TextHeader(c.getFullName()));
		// ooh...
		f.add(new TextSubHeader("Description"));
		f.add(new Paragraph(c.description()));
		if (!c.notes().isEmpty()) {
			f.add(new Paragraph(c.notes()));
		}
		f.add(new TextSubHeader("Arguments"));
		final Table args=new Table();
		for (final Argument p: c.getArguments()) {
			f.add("<b>"+p.name()+"</b>");
			f.add(" - Type: "+p.type());
			if (p.type()==ArgumentType.CHOICE) {
				final String[] split=proposedcommand.split("\\.");
				final StringBuilder options=new StringBuilder();
				for (final String s: p.getChoices(st)) {
					if (!options.isEmpty()) {
						options.append(", ");
					}
					options.append(s);
				}
				f.add(" ["+options+"]");
			}
			f.add(" - Mandatory: "+p.mandatory());
			f.add(" - "+p.description());
			f.add("<br>");
		}
		if (c.requiresPermission()!=null&&!c.requiresPermission().isEmpty()) {
			f.add(new TextSubHeader("Permission Required to Invoke"));
			f.add(c.requiresPermission());
		}
		f.add(new TextSubHeader("Operational Context"));
		f.add(c.context().toString());
		f.add(new TextSubHeader("Interface Access"));
		if (c.permitConsole()) {
			f.add(new Color("green","Accessible via console"));
		} else {
			f.add(new Color("red","Console access denied"));
		}
		f.add("<br>");
		if (c.permitHUD()) {
			f.add(new Color("green","Accessible via HUD"));
		} else {
			f.add(new Color("red","HUD access denied"));
		}
		f.add("<br>");
		if (c.permitWeb()) {
			f.add(new Color("green","Accessible via Web"));
		} else {
			f.add(new Color("red","Web access denied"));
		}
		f.add("<br>");
		if (c.permitScripting()) {
			f.add(new Color("green","Accessible via Scripting module"));
		} else {
			f.add(new Color("red","Scripting access denied"));
		}
		f.add("<br>");
		if (c.permitObject()) {
			f.add(new Color("green","Accessible via Object API"));
		} else {
			f.add(new Color("red","Object access denied"));
		}
		f.add("<br>");
		if (c.permitExternal()) {
			f.add(new Color("green","Accessible via External API"));
		} else {
			f.add(new Color("red","External access denied"));
		}
		f.add("<br>");
		f.add("<br>");
	}
	
	@URLs(url="/introspection/api/")
	@SideSubMenus(name="API", priority=1)
	public static void APIIndex(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		final Table t=new Table();
		//t.border(true);
		f.add(t);
		for (final Module m: Modules.getModules()) {
			t.add(new HeaderRow().add(new Cell(new TextSubHeader(m.getName()),999)));
			final Map<String,Command> commands=m.getCommands(st);
			for (final Map.Entry<String,Command> entry: commands.entrySet()) {
				t.openRow();
				try {
					final Command c=entry.getValue();
					t.openRow();
					t.add(c.context().toString());
					t.add("<a href=\"/GPHUD/introspection/api/"+m.getName()+"/"+entry.getKey()+"\">"+c.getName()+
					      "</a>");
					t.add(c.description());
					if (c.requiresPermission().isEmpty()) {
						t.add("");
					} else {
						t.add(" [ "+c.requiresPermission()+" ] ");
					}
					if (c.permitConsole()) {
						t.add(new Color("green","Console"));
					} else {
						t.add(new Color("red","Console"));
					}
					if (c.permitHUD()) {
						t.add(new Color("green","HUD"));
					} else {
						t.add(new Color("red","HUD"));
					}
					if (c.permitWeb()) {
						t.add(new Color("green","Web"));
					} else {
						t.add(new Color("red","Web"));
					}
					if (c.permitScripting()) {
						t.add(new Color("green","Scripting"));
					} else {
						t.add(new Color("red","Scripting"));
					}
					if (c.permitObject()) {
						t.add(new Color("green","Object"));
					} else {
						t.add(new Color("red","Object"));
					}
					if (c.permitExternal()) {
						t.add(new Color("green","External"));
					} else {
						t.add(new Color("red","External"));
					}
					if (c.isGenerated()) {
						t.add(new Color("blue","Generated"));
					} else {
						t.add("");
					}
				} catch (@Nonnull final Exception e) {
					t.add("ERR:"+e);
				}
			}
			t.openRow();
			t.add(new Cell(new Separator(),999));
		}
	}
	
}
