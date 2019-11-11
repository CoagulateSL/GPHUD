package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import java.util.Map;

/**
 * API Introspection.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ManagePermissions {
	@URLs(url = "/introspection/permissions")
	@SideSubMenus(name = "Permissions", priority = 20)
	public static void createForm(State st, SafeMap values) {
		Form f = st.form;
		f.add(new TextHeader("Permission registrations"));
		Table t = new Table();
		f.add(t);
		for (Module m : Modules.getModules()) {
			Map<String, Permission> permissions = m.getPermissions(st);
			if (!permissions.isEmpty()) {
				t.add(new HeaderRow().add(new Cell(new TextSubHeader(m.getName()), 999)));
				for (String permission : permissions.keySet()) {
					Row r = new Row();
					t.add(r);
					r.setbgcolor(permissions.get(permission).getColor());
					t.add(permission);
					t.add(permissions.get(permission).description());
					boolean bump=true;
					if (permissions.get(permission).isGenerated()) { t.add("<i>Generated</i>"); bump=false; }
					if (permissions.get(permission).grantable() == false) {
						if (bump) { t.add(""); }
						t.add(new Color("red", "Ungrantable"));
					}
				}
				t.add(new Row(new Cell(new Separator(), 999)));
			}
		}
	}

}
