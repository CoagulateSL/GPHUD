package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Characters.View;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.util.Set;


/**
 * Because we need an index for the User Interface.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class UserLandingPage {
	@URLs(url = "/")
	public static void index(State st, SafeMap values) throws UserException, SystemException {
		Form f = st.form;
		f.add(new TextHeader("Welcome to GPHUD"));
		if (st.getInstanceNullable() == null) {
			SessionSwitch.switchInstance(st, values);
			return;
		}
		if (st.getCharacterNullable() == null) {
			Set<Char> chars = Char.getCharacters(st.getInstance(), st.avatar());
			if (chars.size() == 1) {
				st.setCharacter(chars.iterator().next());
				st.cookie.setCharacter(st.getCharacter());
			} else {
				SessionSwitch.switchCharacter(st, values);
				return;
			}
		}
		View.viewCharacter(st, values, st.getCharacter(), true);
	}
}
