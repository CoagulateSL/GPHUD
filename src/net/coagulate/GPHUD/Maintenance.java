package net.coagulate.GPHUD;

import net.coagulate.Core.Database.LockException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Events.EventsMaintenance;
import net.coagulate.GPHUD.Modules.Experience.VisitXP;
import net.coagulate.SL.Data.LockTest;
import org.json.JSONObject;

import java.util.Calendar;

import static java.util.logging.Level.*;
import static net.coagulate.SL.Config.LOCK_NUMBER_GPHUD_MAINTENANCE;

/**
 * Maintenance tasks, runs every 60 seconds.
 * No functional need for this as a class, just keep the code out of the MAIN program.
 * Note we're running in the programs main thread (though otherwise asleep) thread.  Don't crash it :P
 *
 * @author iain
 */
public class Maintenance extends Thread {

	public static final int PINGHUDINTERVAL = 10;
	public static final int PINGSERVERINTERVAL = 10;
	public static final int UPDATEINTERVAL = 5;
	public static int cycle = 0;

	public static void purgeOldCookies() {
		try {
			int now = UnixTime.getUnixTime();
			int before = GPHUD.getDB().dqi(true, "select count(*) from cookies");
			GPHUD.getDB().d("delete from cookies where expires<?", now);
			int after = GPHUD.getDB().dqi(true, "select count(*) from cookies");
			if (before != after) {
				GPHUD.getLogger().log(FINE, "Cookies cleaned from " + before + " to " + after + " (" + (after - before) + ")");
			}
		} catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Cookie expiration task exceptioned!", e);
		}
	}

	public static void refreshCharacterURLs() {
		// note limit 0,30, we do no more than 30 of these per minute

		Results results = GPHUD.getDB().dq("select characterid,name,url,urllast from characters where url is not null and authnode like ? and urllast<? order by urllast asc limit 0,30", Interface.getNode(), UnixTime.getUnixTime() - (PINGHUDINTERVAL * 60));
		if (results.notEmpty()) {
			GPHUD.getLogger().log(FINE,"Pinging out to "+results.size()+" character URLs");
			for (ResultsRow r : results) {
				//System.out.println("About to background for a callback to character:"+r.getString("name"));
				JSONObject ping = new JSONObject().put("incommand", "ping");
				Transmission t = new Transmission((Char) null, ping, r.getString("url"));
				t.start();
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
			}
		} //else { GPHUD.getLogger().log(FINE,"Pinging out to no character URLs"); }
	}

	public static void refreshRegionURLs() {
		// note limit 0,30, we do no more than 30 of these per minute

		Results results = GPHUD.getDB().dq("select regionid,name,url,urllast from regions where url is not null and url!='' and authnode like ? and urllast<? order by urllast asc limit 0,30", Interface.getNode(), UnixTime.getUnixTime() - (PINGSERVERINTERVAL * 60));
		if (results.notEmpty()) {
			//GPHUD.getLogger().log(FINE,"Pinging out to "+results.size()+" region URLs");
			for (ResultsRow r : results) {
				//System.out.println("About to background for a callback to region:"+r.getString("name"));
				JSONObject ping = new JSONObject().put("incommand", "ping");
				Transmission t = new Transmission((Region) null, ping, r.getString("url"));
				t.start();
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
			}
		}
	}

	// remove cookies that are past expirey

	public static void updateInstances() {
		for (Instance i : Instance.getInstances()) {
			try {
				//GPHUD.getLogger().log(FINER,"Pushing status update for instance "+i.getName());
				i.updateStatus();
			} catch (Exception e) {
				GPHUD.getLogger().log(WARNING, "Exception while pushing status update for instance " + i.getNameSafe());
			}
		}
	}

	public static void startEvents() {
		EventsMaintenance.maintenance();
	}

	// for calling from OTHER MAINTENANCE CODE (GPHUD from SL)
	public static void gphudMaintenance() {
		try { Maintenance.refreshCharacterURLs(); } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance refresh character URLs caught an exception", e);
		}
		try { Maintenance.refreshRegionURLs(); } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance refresh region URLs caught an exception", e);
		}

		// this stuff all must run 'exclusively' across the cluster...
		LockTest lock = new LockTest(LOCK_NUMBER_GPHUD_MAINTENANCE);
		int lockserial;
		try { lockserial = lock.lock(60); } catch (LockException e) {
			GPHUD.getLogger().finer("Maintenance didn't aquire lock: " + e.getLocalizedMessage());
			return;
		} // maintenance session already locked

		try { Maintenance.startEvents(); } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance start events caught an exception", e);
		}

		lock.extendLock(lockserial, 60);
		try { Maintenance.purgeOldCookies(); } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance run purge cookies caught an exception", e);
		}

		lock.extendLock(lockserial, 60);
		try { new VisitXP(-1).runAwards(); } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance run awards run caught an exception", e);
		}

		lock.extendLock(lockserial, 60);
		try { if ((cycle % UPDATEINTERVAL) == 0) { Maintenance.updateInstances(); } } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance update Instances caught an exception", e);
		}

		lock.unlock(lockserial);
	}

	public void runAlways() {
		try { refreshCharacterURLs(); } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance refresh character URLs caught an exception", e);
		}
		try { refreshRegionURLs(); } catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Maintenance refresh region URLs caught an exception", e);
		}
	}

	public void run() {
		cycle++;
		runAlways();
		runExclusive();
	}

	public void runExclusive() {
		Calendar c = Calendar.getInstance();
		int now = c.get(Calendar.MINUTE);
		if (c.get(Calendar.SECOND) > 30) { now++; }
		if (now >= 60) { now -= 60; }
		if (GPHUD.ourCycle(now)) {
			try { startEvents(); } catch (Exception e) {
				GPHUD.getLogger().log(SEVERE, "Maintenance start events caught an exception", e);
			}
			try { purgeOldCookies(); } catch (Exception e) {
				GPHUD.getLogger().log(SEVERE, "Maintenance run purge cookies caught an exception", e);
			}
			try { new VisitXP(-1).runAwards(); } catch (Exception e) {
				GPHUD.getLogger().log(SEVERE, "Maintenance run awards run caught an exception", e);
			}
			try { if ((cycle % UPDATEINTERVAL) == 0) { updateInstances(); } } catch (Exception e) {
				GPHUD.getLogger().log(SEVERE, "Maintenance update Instances caught an exception", e);
			}
		}
	}


}
