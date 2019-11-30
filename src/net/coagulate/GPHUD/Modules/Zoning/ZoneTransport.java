package net.coagulate.GPHUD.Modules.Zoning;

import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Zone;

/**
 * Packages up the zones in a simple-to-use format for the HUD.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ZoneTransport {

	public static String createZoneTransport(Region r) {
		StringBuilder response = new StringBuilder();
		for (Zone z : r.getZones()) {
			String s = z.getTransportFormat();
			if (s != null) {
				if (response.length() > 0) { response.append("|"); }
				response.append(s);
			}
		}
		return response.toString();
	}

}
