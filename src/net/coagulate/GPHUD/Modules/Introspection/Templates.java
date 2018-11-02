package net.coagulate.GPHUD.Modules.Introspection;

import java.lang.reflect.Method;
import java.util.Map;
import static java.util.logging.Level.FINE;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.UserException;

/** Introspection of the template annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Templates {
    @URLs(url = "/introspection/templates")
    @SideSubMenus(name = "Templates",priority = 25)
    public static void listTemplates(State st,SafeMap values) {
        Form f=st.form;
        f.add(new TextHeader("Templates available"));
        Table t=new Table(); f.add(t);
        t.add(new HeaderRow().add("Template Keyword").add("Description").add("Provider").add("Current value"));
        Map<String, String> templates = Templater.getTemplates(st);
        for (String template:templates.keySet()) {
            //System.out.println(template);
            Method m=Templater.getMethod(st,template);
            t.openRow();
            t.add(template);
            t.add(templates.get(template));
            if (m==null) { t.add("<i>NULL</i>"); } else { t.add(m.getDeclaringClass().getName()+"."+m.getName()+"()"); }
            String value="WEIRD";
            try { value=Templater.getValue(st, template, false,false); }
            catch (UserException e) { value="ERROR:"+e.getMessage(); st.logger().log(FINE,"Template gave user exception (not unexpected)",e); }
            t.add(value);
        }
    }
}
