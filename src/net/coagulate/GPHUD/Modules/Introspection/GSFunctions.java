package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class GSFunctions {
	private GSFunctions() {}

	// ---------- STATICS ----------
	@URL.URLs(url="/introspection/gsfunctions/*")
	public static void renderFunction(@Nonnull final State st,
	                                  final SafeMap values) {
		String uri=st.getDebasedURL();
		if (!uri.startsWith("/introspection/gsfunctions/")) {
			throw new SystemImplementationException("URL Misconfiguratin?");
		}
		uri=uri.substring("/introspection/gsfunctions/".length());

		// if we get here, we're investigating a specific command
		final Form f=st.form();
		String proposedcommand=uri;
		proposedcommand=proposedcommand.replaceAll("/",".");
		proposedcommand=proposedcommand.replaceAll("[^A-Za-z0-9.]","");  // limited character set.  XSS protect etc blah blah tainted user input blah

		final Method method=net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.get(proposedcommand);
		if (method==null) {
			f.add(new TextError("FUNCTION NOT FOUND!"));
			return;
		}
		final net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction meta=method.getAnnotation(net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction.class);
		f.add(new TextHeader("("+meta.category()+") : "+method.getName()));
		// ooh...
		f.add(new TextSubHeader("Return Type"));
		f.add(meta.returns());
		f.add(new TextSubHeader("Arguments"));
		f.add(meta.parameters());
		f.add(new TextSubHeader("Description"));
		f.add(meta.description());
		f.add(new TextSubHeader("Notes"));
		if (meta.privileged()) { f.add("<p><i>This is a privileged function call</i></p>"); }
		f.add(meta.notes());
	}

	@URL.URLs(url="/introspection/gsfunctions")
	@SideSubMenu.SideSubMenus(name="GSFunctions",
	                          priority=15)
	public static void GSFunctionsIndex(@Nonnull final State st,
	                                    final SafeMap values) {
		final Form f=st.form();
		final Table table=new Table();
		final Map<String,Map<String,String>> output=new TreeMap<>();
		f.add(table);
		for (final String functionname: net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.getAll().keySet()) {
			String category="";
			String line="";
			try {
				final Method function=net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.get(functionname);
				final net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction meta=function.getAnnotation(net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction.class);
				category=meta.category().toString();
				line+="<td><a href=\"/GPHUD/introspection/gsfunctions/"+functionname+"\">"+functionname+"</a></td>";
				if (meta.privileged()) {
					line+="<td><i>Privileged</i>&nbsp;&nbsp;&nbsp;</td>";
				}
				else {
					line+="<td></td>";
				}
				line+="<td>"+meta.description()+"</td>";

			}
			catch (@Nonnull final Exception e) { line+="<td>ERR:"+e+"</td>"; }
			if (!output.containsKey(category)) { output.put(category,new TreeMap<>()); }
			output.get(category).put(functionname,line);
		}
		for (final Entry<String,Map<String,String>> catset: output.entrySet()) {
			table.add(new Row("<td colspan=9999><table width=100%><tr width=100%><td width=50%><hr></td><td><span style=\"display: inline-block; white-space: nowrap;\"><b>"+catset
					.getKey()+"</b></span"+"></td><td width=50%><hr></td></tr></table></td></tr><tr>"));
			for (final String rowoutput: catset.getValue().values()) {
				table.add(new Row(rowoutput));
			}

		}
	}

}
