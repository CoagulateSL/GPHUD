package net.coagulate.GPHUD.Modules.Zoning;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Data.ZoneArea;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/**
 * Zone management pages.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ZonePages {

	@URLs(url = "/configuration/zoning")
	public static void listZones(State st, SafeMap values) {
		Form f = st.form;
		f.noForm();
		f.p(new TextHeader("Zoning configuration"));
		for (Zone zone:st.getInstance().getZones()) {
			f.add(new Link(zone.getName(), "./zoning/view/" + zone.getId())).add("<br>");
		}
		f.add("<br>");
		if (st.hasPermission("Zoning.config")) {
			f.add(new Form(st, true, "./zoning/create", "Create Zone"));
		}
		Modules.get(st, "Zoning").kvConfigPage(st);
	}

	@URLs(url = "/configuration/zoning/create")
	public static void createZone(State st, SafeMap values) {
		Modules.simpleHtml(st, "zoning.create", values);
	}

	@URLs(url = "/configuration/zoning/view/*")
	public static void viewZone(State st, SafeMap values) throws UserException, SystemException {
		String split[] = st.getDebasedURL().split("/");
		String id = split[split.length - 1];
		Zone z = Zone.get(Integer.parseInt(id));
		viewZone(st, values, z, false);
	}

	public static void viewZone(State st, SafeMap values, Zone z, boolean brief) throws UserException, SystemException {
		boolean full = false;
		boolean admin = false;
		if (st.hasPermission("zoning.config")) { admin = true; }
		Form f = st.form;
		f.noForm();
		f.add(new TextHeader("Zone: " + z.getName()));
		Table t = new Table();
		f.add(t);
		t.border(false);
		for (ZoneArea a : z.getZoneAreas()) {
			t.openRow().add("Location").add(a.getRegion(true).getName() + ", " + a.getVectors()[0] + " - " + a.getVectors()[1]);
			if (admin) { t.add(new Form(st, true, "../deletearea", "Delete Area", "zoneareaid", a.getId() + "")); }
		}
		if (admin) { t.openRow().add("").add(new Form(st, true, "../addarea", "Add Area", "zone", z.getName())); }
		f.add(new TextSubHeader("Influenced KVs"));
		GenericConfiguration.page(st, values, z, st.simulate(st.getCharacter()));
	}

	@URLs(url = "/configuration/zoning/setcorner1", requiresPermission = "zoning.config")
	public static void setCornerOne(State st, SafeMap values) {
		Modules.simpleHtml(st, "zoning.setcornerone", values);
	}

	@URLs(url = "/configuration/zoning/setcorner2", requiresPermission = "zoning.config")
	public static void setCornerTwo(State st, SafeMap values) {
		Modules.simpleHtml(st, "zoning.setcornertwo", values);
	}

	@URLs(url = "/configuration/zoning/addarea", requiresPermission = "zoning.config")
	public static void addVolume(State st, SafeMap values) {
		Modules.simpleHtml(st, "zoning.addvolume", values);
	}

	@URLs(url = "/configuration/zoning/deletearea", requiresPermission = "zoning.config")
	public static void delVolume(State st, SafeMap values) {
		Modules.run(st, "zoning.deletevolume", values);
		throw new RedirectionException(values.get("okreturnurl"));
	}
}

