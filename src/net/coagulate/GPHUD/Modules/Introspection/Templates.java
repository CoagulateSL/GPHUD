package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Map;

import static java.util.logging.Level.FINE;

/**
 * Introspection of the template annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Templates {
	@URLs(url="/introspection/templates")
	@SideSubMenus(name="Templates", priority=25)
	public static void listTemplates(@Nonnull final State st,
	                                 final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Templates available"));
		final Table t=new Table();
		f.add(t);
		t.add(new HeaderRow().add("Template Keyword").add("Description").add("Provider").add("Current value"));
		final Map<String,String> templates=Templater.getTemplates(st,true);
		for (final Map.Entry<String,String> entry: templates.entrySet()) {
			final String template=entry.getKey();
			//System.out.println(template);
			final Method m=Templater.getMethod(st,template);
			t.openRow();
			t.add(template);
			t.add(entry.getValue());
			if (m==null) { t.add("<i>NULL</i>"); }
			else {
				t.add(m.getDeclaringClass().getName()+"."+m.getName()+"()");
			}
			String value;
			try { value=Templater.getValue(st,template,false,false); }
			catch (@Nonnull final UserException e) {
				value="ERROR:"+e.getMessage();
				st.logger().log(FINE,"Template gave user exception (not unexpected)",e);
			}
			value=StringEscapeUtils.escapeHtml4(value);
			t.add(value);
		}
	}
}
