package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * API Introspection.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class LevelCurve {
	@URLs(url = "/introspection/levelcurve")
	@SideSubMenus(name = "Level Curve", priority = 1)
	public static void createForm(@Nonnull State st, SafeMap values) throws UserException, SystemException {
		Form f = st.form();
		Table t = new Table();
		f.add(t);
		t.border(true);
		for (int row = 0; row < 1000; row += 10) {
			t.openRow();
			for (int column = 0; column < 10; column++) {
				int xp = row + column;
				t.add(xp + "xp = Lvl " + Experience.toLevel(st, xp));
			}
		}
	}

}
