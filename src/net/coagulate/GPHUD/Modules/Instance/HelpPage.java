package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;


/**  Because we need an index for the User Interface.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class HelpPage {
    @URLs(url = "/Docs")
    public static void index(State st,SafeMap values) throws UserException, SystemException {
        Form f=st.form;
        f.add("<iframe width=100% height=100% frameborder=0 src=\"https://sl.coagulate.net/Docs/GPHUD/index.php/Main_Page.html\"></iframe>");
    }
}
