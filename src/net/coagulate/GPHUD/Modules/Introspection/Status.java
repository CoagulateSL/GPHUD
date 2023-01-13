package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Obj;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Visit;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.SideSubMenu.SideSubMenus;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Status {
	// ---------- STATICS ----------
	@URLs(url="/introspection/status", requiresPermission="Introspection.ViewStatus")
	@SideSubMenus(name="Status", priority=99, requiresPermission="Introspection.ViewStatus")
	public static void createForm(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Connectivity status, instance "+st.getInstance().getName()+"#"+st.getInstance().getId()));
		f.add(new Paragraph(
				"Various data from key tables showing real-time status of interactive objects and connections (rather than static configuration data)"));
		f.add(new Paragraph(
				"Please note that 'last activity' timers are always approximate and are only updated in the database once their difference from the old value crosses a triggering threshold, in order to prevent excessive database writes (increasing cluster database updates and locking issues)"));
		f.add(new TextSubHeader("Region Status"));
		f.add(Region.statusDump(st));
		f.add(new TextSubHeader("Active Character Status"));
		f.add(Char.statusDump(st));
		f.add(new TextSubHeader("Visits Status"));
		f.add(Visit.statusDump(st));
		/*f.add(new TextSubHeader("Event Status"));
		f.add(Event.statusDump(st));
		f.add(new TextSubHeader("Event Visit Status"));
		f.add(EventVisit.statusDump(st));*/
		f.add(new TextSubHeader("Objects Status"));
		f.add(Obj.statusDump(st));
	}
}
