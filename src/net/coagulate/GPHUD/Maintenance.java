package net.coagulate.GPHUD;

import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Events.EventsMaintenance;
import net.coagulate.GPHUD.Modules.Experience.VisitXP;
import net.coagulate.SL.Data.LockTest;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Calendar;

import static java.util.logging.Level.*;
import static net.coagulate.SL.Config.LOCK_NUMBER_GPHUD_MAINTENANCE;

/**
 * Maintenance tasks, runs every 60 seconds.
 * No functional need for this as a class, just keep the code out of the MAIN program.
 * Note we're (sometimes) running in the programs main thread (though otherwise asleep) thread.  Don't crash it :P
 *
 * @author iain
 */
public class Maintenance extends Thread {

	public static final int PINGHUDINTERVAL=5;
	public static final int PINGSERVERINTERVAL=5;
	public static final int UPDATEINTERVAL=5;
	public static final int PURGECONNECTIONS=60;
	public static int cycle;

	// ---------- STATICS ----------
	public static void purgeConnections() {
		try {
			final int purgecount=Obj.getPurgeInactiveCount();
			if (purgecount>0) {
				GPHUD.getLogger().log(FINE,"Purging "+purgecount+" disconnected objects");
				Obj.purgeInactive();
			}
		}
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"purgeConnections Exceptioned!",e);
		}
	}

	public static void purgeOldCookies() {
		try {
			final int before=Cookie.countAll();
			Cookie.expire();
			final int after=Cookie.countAll();
			if (before!=after) {
				GPHUD.getLogger().log(FINE,"Cookies cleaned from "+before+" to "+after+" ("+(after-before)+")");
			}
		}
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Cookie expiration task exceptioned!",e);
		}
	}

	public static void refreshCharacterURLs() {
		// note limit 0,30, we do no more than 30 of these per minute

		final Results results=Char.getPingable();
		if (results.notEmpty()) {
			//GPHUD.getLogger().log(FINE,"Pinging out to "+results.size()+" character URLs");
			for (final ResultsRow r: results) {
				//System.out.println("About to background for a callback to character:"+r.getString("name"));
				final JSONObject ping=new JSONObject().put("incommand","ping");
				final Transmission t=new PingTransmission(Char.get(r.getInt("characterid")),ping,r.getStringNullable("url"));
				t.start();
				try { Thread.sleep(1000); } catch (@Nonnull final InterruptedException e) {}
			}
		} //else { GPHUD.getLogger().log(FINE,"Pinging out to no character URLs"); }
	}

	public static void refreshRegionURLs() {
		// note limit 0,30, we do no more than 30 of these per minute

		final Results results=Region.getPingable();
		if (results.notEmpty()) {
			//GPHUD.getLogger().log(FINE,"Pinging out to "+results.size()+" region URLs");
			for (final ResultsRow r: results) {
				//System.out.println("About to background for a callback to region:"+r.getString("name"));
				final JSONObject ping=new JSONObject().put("incommand","ping");
				final Transmission t=new Transmission((Region) null,ping,r.getStringNullable("url"));
				t.start();
				try { Thread.sleep(1000); } catch (@Nonnull final InterruptedException e) {}
			}
		}
	}

	public static void updateInstances() {
		for (final Instance i: Instance.getInstances()) {
			try {
				//GPHUD.getLogger().log(FINER,"Pushing status update for instance "+i.getName());
				i.updateStatus();
			}
			catch (@Nonnull final Exception e) {
				GPHUD.getLogger().log(WARNING,"Exception while pushing status update for instance "+i.getNameSafe());
			}
		}
	}

	// remove cookies that are past expirey

	public static void startEvents() {
		EventsMaintenance.maintenance();
	}

	// for calling from OTHER MAINTENANCE CODE (GPHUD from SL)
	public static void gphudMaintenance() {
		try { Maintenance.refreshCharacterURLs(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh character URLs caught an exception",e);
		}
		try { Maintenance.refreshRegionURLs(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh region URLs caught an exception",e);
		}

		// this stuff all must run 'exclusively' across the cluster...
		final LockTest lock=new LockTest(LOCK_NUMBER_GPHUD_MAINTENANCE);
		final int lockserial;
		try { lockserial=lock.lock(60); }
		catch (@Nonnull final LockException e) {
			GPHUD.getLogger().finer("Maintenance didn't aquire lock: "+e.getLocalizedMessage());
			return;
		} // maintenance session already locked

		try { Maintenance.startEvents(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance start events caught an exception",e);
		}

		lock.extendLock(lockserial,60);
		try { Maintenance.purgeOldCookies(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run purge cookies caught an exception",e);
		}

		lock.extendLock(lockserial,60);
		try { Maintenance.purgeConnections(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run purge connections caught an exception",e);
		}

		lock.extendLock(lockserial,60);
		try { new VisitXP(-1).runAwards(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run awards run caught an exception",e);
		}

		lock.extendLock(lockserial,60);
		try { if ((cycle%UPDATEINTERVAL)==0) { Maintenance.updateInstances(); } }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance update Instances caught an exception",e);
		}

		lock.unlock(lockserial);
	}

	// ---------- INSTANCE ----------
	public void runAlways() {
		try { refreshCharacterURLs(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh character URLs caught an exception",e);
		}
		try { refreshRegionURLs(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh region URLs caught an exception",e);
		}
	}

	public void run() {
		cycle++;
		runAlways();
		runExclusive();
	}

	public void runExclusive() {
		final Calendar c=Calendar.getInstance();
		int now=c.get(Calendar.MINUTE);
		if (c.get(Calendar.SECOND)>30) { now++; }
		if (now >= 60) { now-=60; }
		if (GPHUD.ourCycle(now)) {
			try { startEvents(); }
			catch (@Nonnull final Exception e) {
				GPHUD.getLogger().log(SEVERE,"Maintenance start events caught an exception",e);
			}
			try { purgeOldCookies(); }
			catch (@Nonnull final Exception e) {
				GPHUD.getLogger().log(SEVERE,"Maintenance run purge cookies caught an exception",e);
			}
			try { new VisitXP(-1).runAwards(); }
			catch (@Nonnull final Exception e) {
				GPHUD.getLogger().log(SEVERE,"Maintenance run awards run caught an exception",e);
			}
			try { if ((cycle%UPDATEINTERVAL)==0) { updateInstances(); } }
			catch (@Nonnull final Exception e) {
				GPHUD.getLogger().log(SEVERE,"Maintenance update Instances caught an exception",e);
			}
		}
	}

	public static class PingTransmission extends Transmission {
		public PingTransmission(final Char c,
		                        @Nonnull final JSONObject json,
		                        final String url) {
			super(c,json,url);
		}

		// ---------- INSTANCE ----------
		public void run() {
			super.run();
			if (!failed()) {
				if (character==null) { // erm
					return;
				}
				character.pinged();
			}
		}
	}


}
