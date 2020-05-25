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
		int sortableversion=(major*10000)+(minor*100)+bugfix;
		// oldest supported version is:
		if (sortableversion >= 31000) { return null; }
		// all versions prior to 31000 expire Sunday June 14th 2020
		expiration.set(2020,Calendar.JUNE,14,0,0,0);
		return expiration.getTime();
	}

	public static boolean hasExpired(@Nullable final int version) {
		int major=version/10000;
		int minor=(version/100)%100;
		int bugfix=version%100;
		return hasExpired(major,minor,bugfix);
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

	public static Float expiresIn(int version) {
		int major=version/10000;
		int minor=(version/100)%100;
		int bugfix=version%100;
		return expiresIn(major,minor,bugfix);
	}

	public static Float expiresIn(@Nullable final String version) {
		if (version==null) { return null; }
		try {
			String[] parts=version.split("\\.");
			if (parts.length!=3) { SL.reportString("Weird version string received",null,version); }
			return expiresIn(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));
		}
		catch (NumberFormatException e) { return null; }
	}

	public static Float expiresIn(final int major,
	                              final int minor,
	                              final int bugfix) {
		Date endoflife=getEndOfLife(major,minor,bugfix);
		if (endoflife==null) { return null; }
		long expiresin=endoflife.getTime()-new Date().getTime();
		expiresin=expiresin/1000l; // milliseconds
		float daysexpiresin=expiresin/(60f*60f*24f); //convert to days
		//daysexpiresin=((float)((int)(100.0*daysexpiresin)))/100.0f;
		//System.out.println("Version check: "+major+"."+minor+"."+bugfix+" expires in "+daysexpiresin+" days");
		return daysexpiresin;
	}
}
