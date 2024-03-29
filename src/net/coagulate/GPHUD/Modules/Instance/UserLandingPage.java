package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Characters.View;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Set;


/**
 * Because we need an index for the User Interface.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class UserLandingPage {
	// ---------- STATICS ----------
	@URLs(url="/")
	public static void index(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Welcome to GPHUD"));
		if (st.getInstanceNullable()==null) {
			SessionSwitch.switchInstance(st,values);
			return;
		}
		if (st.getCharacterNullable()==null) {
			final Set<Char> chars=Char.getCharacters(st.getInstance(),st.getAvatar());
			if (chars.size()==1) {
				st.setCharacter(chars.iterator().next());
				st.cookie().setCharacter(st.getCharacter());
			} else {
				SessionSwitch.switchCharacter(st,values);
				return;
			}
		}
		View.viewCharacter(st,values,st.getCharacter(),true);
	}
}
