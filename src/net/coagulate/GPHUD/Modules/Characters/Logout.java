package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Cookie;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Page that destroys the session.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Logout {
	// ---------- STATICS ----------
	@URLs(url="/logout")
	public static void logout(@Nonnull final State st,
	                          final SafeMap values) {
		st.form().add("Good Bye!");
		if (st.cookiestring!=null) { Cookie.delete(st.cookiestring); }
		st.cookie(null);
		st.cookiestring=null;
		st.setAvatar(null);
		st.setCharacter(null);
		st.setInstance(null);
	}

}
