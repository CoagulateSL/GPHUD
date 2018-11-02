package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/** Page that destroys the session.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Logout {
    @URLs(url = "/logout")
    public static void logout(State st,SafeMap values) { 
        st.form.add("Good Bye!");
        if (st.cookiestring!=null) { Cookies.delete(st.cookiestring); }
        st.cookie=null;
        st.cookiestring=null;
        st.user=null;
        st.setAvatar(null);
        st.setCharacter(null);
        st.setInstance(null);
    }    
}
