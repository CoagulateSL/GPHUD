package net.coagulate.GPHUD;

import net.coagulate.SL.SL;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;

public abstract class EndOfLifing {
	// a fairly simple class
	// define an end of life schedule

	// ---------- STATICS ----------
	@Nullable
	public static Date getEndOfLife(final int major,
	                                final int minor,
	                                final int bugfix) {
		Calendar expiration=Calendar.getInstance();
		expiration.add(Calendar.DAY_OF_YEAR,-1);
		return expiration.getTime();
	}

	public static boolean hasExpired(@Nullable final String version) {
		if (version==null) { return false; }
		try {
			String[] parts=version.split("\\.");
			if (parts.length!=3) { SL.reportString("Weird version string received",null,version); }
			return hasExpired(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));
		}
		catch (NumberFormatException e) { return false; }
	}

	public static boolean hasExpired(final int major,
	                                 final int minor,
	                                 final int bugfix) {
		Date endoflife=getEndOfLife(major,minor,bugfix);
		if (endoflife==null) { return false; }
		if (endoflife.before(new Date())) { return true; }
		return false;
	}
}
