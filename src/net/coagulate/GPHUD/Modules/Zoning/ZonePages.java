package net.coagulate.GPHUD.Modules.Zoning;

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

import javax.annotation.Nonnull;

/**
 * Zone management pages.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ZonePages {

	// ---------- STATICS ----------
	@URLs(url="/configuration/zoning",
		  requiresPermission = "Zoning.*")
	public static void listZones(@Nonnull final State st,
	                             final SafeMap values) {
		final Form f=st.form();
		f.noForm();
		f.p(new TextHeader("Zoning configuration"));
		f.add("<table border=0>");
		for (final Zone zone: Zone.getZones(st)) {
			f.add("<tr><td>");
			f.add(new Link(zone.getName(),"./zoning/view/"+zone.getId()));
			if (st.hasPermission("Zoning.config")) {
				f.add("</td><td>");
				f.add(new Form(st,true,"./Zoning/Delete","Delete Zone","zone",zone.getName()+""));
			}
			f.add("</td></tr>");
		}
		f.add("</table>");
		f.add("<br>");
		if (st.hasPermission("Zoning.config")) {
			f.add(new Form(st,true,"./zoning/create","Create Zone"));
		}
		Modules.get(st,"Zoning").kvConfigPage(st);
	}

	@URLs(url="/configuration/zoning/create")
	public static void createZone(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"zoning.create",values);
	}

	@URLs(url="/configuration/zoning/delete")
	public static void deleteZone(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"zoning.delete",values);
	}

	@URLs(url="/configuration/zoning/view/*")
	public static void viewZone(@Nonnull final State st,
	                            final SafeMap values) {
		final String[] split=st.getDebasedURL().split("/");
		final String id=split[split.length-1];
		final Zone z=Zone.get(Integer.parseInt(id));
		viewZone(st,values,z,false);
	}

	public static void viewZone(@Nonnull final State st,
	                            final SafeMap values,
	                            @Nonnull final Zone z,
	                            final boolean brief) {
		final boolean full = false;
		boolean admin = st.hasPermission("zoning.config");
		final Form f = st.form();
		f.noForm();
		f.add(new TextHeader("Zone: " + z.getName()));
		final Table t = new Table();
		f.add(t);
		t.border(false);
		for (final ZoneArea a : z.getZoneAreas()) {
			String name = "NoPosition";
			final String[] vectors = a.getVectors();
			if (vectors != null) {
				name = vectors[0] + " - " + vectors[1];
			}
			t.openRow().add("Location").add(a.getRegion(true).getName() + ", " + name);
			if (admin) { t.add(new Form(st,true,"../deletearea","Delete Area","zoneareaid",a.getId()+"")); }
		}
		if (admin) { t.openRow().add("").add(new Form(st,true,"../addarea","Add Area","zone",z.getName())); }
		f.add(new TextSubHeader("Influenced KVs"));
		GenericConfiguration.page(st,values,z,st.simulate(st.getCharacter()));
	}

	@URLs(url="/configuration/zoning/setcorner1",
	      requiresPermission="zoning.config")
	public static void setCornerOne(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"zoning.setcornerone",values);
	}

	@URLs(url="/configuration/zoning/setcorner2",
	      requiresPermission="zoning.config")
	public static void setCornerTwo(@Nonnull final State st,
	                                @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"zoning.setcornertwo",values);
	}

	@URLs(url="/configuration/zoning/addarea",
	      requiresPermission="zoning.config")
	public static void addVolume(@Nonnull final State st,
	                             @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"zoning.addvolume",values);
	}

	@URLs(url="/configuration/zoning/deletearea",
	      requiresPermission="zoning.config")
	public static void delVolume(@Nonnull final State st,
	                             @Nonnull final SafeMap values) {
		Modules.run(st,"zoning.deletevolume",values);
		throw new RedirectionException(values.get("okreturnurl"));
	}
}

