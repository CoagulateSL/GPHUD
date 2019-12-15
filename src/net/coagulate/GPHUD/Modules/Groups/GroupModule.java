package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.StaticSideSubMenu;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * Dynamic groups menu
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class GroupModule extends ModuleAnnotation {

	public GroupModule(final String name, final ModuleDefinition def) throws SystemException, UserException {
		super(name, def);
	}

	@Nonnull
	@Override
	public Set<SideSubMenu> getSideSubMenus(@Nonnull final State st) {
		final Set<SideSubMenu> ret = new HashSet<>();
		int pri = 1;
		for (final String submenu : st.getCharacterGroupTypes()) {
			if (!submenu.isEmpty()) {
				ret.add(new StaticSideSubMenu(submenu, pri++, "/groups/type/" + submenu, ""));
			}
		}
		ret.add(new StaticSideSubMenu("Other", pri++, "/groups/type/BLANK", ""));
		return ret;
	}

}
