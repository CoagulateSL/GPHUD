package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Outputs.Color;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Allows viewing of a Region object.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ViewRegion {
	@URLs(url="/regions/view/*")
	public static void viewRegion(@Nonnull final State st,
	                              final SafeMap values) {
		//System.out.println(st.uri);
		final String[] split=st.getDebasedURL().split("/");
		//System.out.println(split.length);
		final String id=split[split.length-1];
		final Region r=Region.get(Integer.parseInt(id),true);
		viewRegion(st,values,r);
	}

	public static void viewRegion(@Nonnull final State st,
	                              final SafeMap values,
	                              @Nonnull final Region r) {
		final boolean full=false;
		final Table map=new Table();
		st.form().add(map);
		map.openRow().add("Name").add(r.getName());
		map.openRow().add("Instance").add(r.getInstance());
		map.openRow().add("Communications").add(r.getOnlineStatus(st.getAvatar().getTimeZone()));
		map.openRow().add("Server Version").add(r.getServerVersion(true));
		map.openRow().add("HUD Version").add(r.getHUDVersion(true));
		if (r.needsUpdate()) { map.openRow().add("").add(new Color("orange","Update Required")); }
	}
}
