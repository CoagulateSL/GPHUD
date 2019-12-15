package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Outputs.Color;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Allows viewing of an Instance object.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ViewInstance {

	@URLs(url = "/instances/view/*")
	public static void viewInstance(@Nonnull final State st, final SafeMap values) throws UserException, SystemException {
		//System.out.println(st.uri);
		final String[] split = st.getDebasedURL().split("/");
		//System.out.println(split.length);
		final String id = split[split.length - 1];
		final Instance i = Instance.get(Integer.parseInt(id));
		viewInstance(st, values, i);
	}

	public static void viewInstance(@Nonnull final State st, final SafeMap values, @Nonnull final Instance i) throws UserException {
		final String tz = st.getAvatarNullable().getTimeZone();
		boolean full = false;
		if (st.isSuperUser()) { full = true; }
		final Table map = new Table();
		st.form().add(map);
		map.openRow().add("Name").add(i.getName());
		map.openRow().add("Owner").add(i.getOwner().getGPHUDLink());
		for (final Region r : i.getRegions(false)) {
			map.openRow().add("Region").add(r).add(r.getOnlineStatus(tz));
			map.openRow().add("").add("").add("Server " + r.getServerVersion(true));
			map.openRow().add("").add("").add("HUD " + r.getHUDVersion(true));
			if (r.needsUpdate()) { map.openRow().add("").add("").add(new Color("orange", "Update Required")); }
		}
	}


}
