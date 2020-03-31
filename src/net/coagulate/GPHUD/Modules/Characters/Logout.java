package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
import net.coagulate.GPHUD.Modules.Command;
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
		if (st.cookiestring!=null) { Cookies.delete(st.cookiestring); }
		st.cookie(null);
		st.cookiestring=null;
		st.setAvatar(null);
		st.setCharacter(null);
		st.setInstance(null);
	}

	@Nonnull
	@Command.Commands(description="Log out or disconnect this character",
	                  context=Command.Context.CHARACTER,
	                  permitScripting=false,
	                  permitUserWeb=false,
	                  permitConsole=false,
	                  permitObject=false)
	public static Response logout(@Nonnull final State st) {
		if (st.getCharacterNullable()!=null) {
			st.getCharacter().closeVisits(st);
			st.getCharacter().closeURL(st);
			st.logger().info("Logout from avatar "+st.getAvatar().getName()+" as character "+st.getCharacter().getName());
		}
		return new TerminateResponse("Logout complete");
	}
}
