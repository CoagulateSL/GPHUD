package net.coagulate.GPHUD.Modules.Zoning;

import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Zone;

import javax.annotation.Nonnull;

/**
 * Packages up the zones in a simple-to-use format for the HUD.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ZoneTransport {
	
	// ---------- STATICS ----------
	@Nonnull
	public static String createZoneTransport(@Nonnull final Region r) {
		final StringBuilder response=new StringBuilder();
		for (final Zone z: r.getZones()) {
			final String s=z.getTransportFormat();
			//if (s != null) {
			if (!response.isEmpty()) {
				response.append("|");
			}
			response.append(s);
			//}
		}
		return response.toString();
	}
	
}
