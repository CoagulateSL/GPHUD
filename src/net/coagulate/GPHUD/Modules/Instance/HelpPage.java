package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;


/**  Because we need an index for the User Interface.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class HelpPage {
    @URLs(url = "/help")
    public static void index(State st,SafeMap values) throws UserException, SystemException {
        Form f=st.form;
        f.noForm();
        f.add("<table width=100% height=100%><tr width=100% height=100%><td width=100% height=100% valign=top><iframe width=100% height=100% frameborder=0 src=\"https://sl.coagulate.net/Docs/GPHUD/index.php/Main_Page.html\"></iframe></td></tr></table>");
    }
}
