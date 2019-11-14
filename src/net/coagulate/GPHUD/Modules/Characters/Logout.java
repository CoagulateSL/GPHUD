package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/**
 * Page that destroys the session.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Logout {
	@URLs(url = "/logout")
	public static void logout(State st, SafeMap values) {
		st.form.add("Good Bye!");
		if (st.cookiestring != null) { Cookies.delete(st.cookiestring); }
		st.cookie = null;
		st.cookiestring = null;
		st.setAvatar(null);
		st.setCharacter(null);
		st.setInstance(null);
	}

	@Command.Commands(description = "Log out or disconnect this character",context= Command.Context.CHARACTER,permitScripting = false,permitUserWeb = false,permitConsole = false)
	public static Response logout(State st) {
		if (st.getCharacterNullable()!=null) {
			st.getCharacter().closeVisits(st);
			st.getCharacter().closeURL(st);
			st.logger().info("Logout from avatar " + st.getAvatar().getName()+" as character "+st.getCharacter().getName());
		}
		return new TerminateResponse("Logout complete");
	}
}
