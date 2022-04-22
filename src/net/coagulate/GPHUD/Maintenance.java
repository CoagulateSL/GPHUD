package net.coagulate.GPHUD;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Events.EventsMaintenance;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Set;

import static java.util.logging.Level.*;

/**
 * Maintenance tasks, runs every 60 seconds.
 * No functional need for this as a class, just keep the code out of the MAIN program.
 * Note we're (sometimes) running in the programs main thread (though otherwise asleep) thread.  Don't crash it :P
 *
 * @author iain
 */
public class Maintenance extends Thread {

	public static final int PINGHUDINTERVAL=15;
	public static final int PINGSERVERINTERVAL=5;

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
				try { Thread.sleep(1000); } catch (@Nonnull final InterruptedException ignored) {}
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
				try { Thread.sleep(1000); } catch (@Nonnull final InterruptedException ignored) {}
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
				if (Config.getDevelopment()) { GPHUD.getLogger().log(WARNING,"Exception while pushing status update for instance "+i.getNameSafe(),e); }
				else
				{ GPHUD.getLogger().log(WARNING,"Exception while pushing status update for instance "+i.getNameSafe()); }
			}
		}
	}

	// remove cookies that are past expiry

	public static void startEvents() {
		EventsMaintenance.maintenance();
	}

	// for calling from OTHER MAINTENANCE CODE (GPHUD from SL)
	public static void gphudMaintenance() {
		try { ScriptRun.maintenance(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance script run expiration caught an exception",e);
		}
		try { Maintenance.refreshCharacterURLs(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh character URLs caught an exception",e);
		}
		try { Maintenance.refreshRegionURLs(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh region URLs caught an exception",e);
		}

		try { Maintenance.startEvents(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance start events caught an exception",e);
		}

		try { Maintenance.purgeOldCookies(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run purge cookies caught an exception",e);
		}

		try { Maintenance.purgeConnections(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run purge connections caught an exception",e);
		}

		try { Visit.runAwards(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run awards run caught an exception",e);
		}

		try { Maintenance.updateInstances(); }
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance update Instances caught an exception",e);
		}
	}

	public static void quotaCredits() {
		try {
			Instance.quotaCredits();
		}
		catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Quota update caught an exception",e);
		}

	}
	private static boolean disablementNoted=false;
	public static void instanceCleanup() {
		if (!Config.getGPHUDAutoCleanInstances()) {
			if (!disablementNoted) {
				disablementNoted=true;
				GPHUD.getLogger().log(CONFIG,"Region/Instance automatic retirement is disabled.");
			}
			return;
		}
		// Look for regions we've not heard from in a while
		for (Region region:Region.getTimedOut()) {
			System.out.println("In instanceCleanup region "+region.getName());
			String instance=region.getInstance().getName();
			String name=region.getName();
			region.retire();
			GPHUD.getLogger(instance).log(INFO,"Region "+name+" was retired due to inactivity.");
			SL.im(region.getInstance().getOwner().getUUID(),"=== GPHUD Information ===\n"+
																	"Instance: "+instance+"\n"+
																	"Region: "+name+"\n"+
																	"-\n"+
																	"The above region has been marked 'retired' due to extended inactivity.");
		}
		for(Instance instance:Instance.getExpiredInstances()) {
			String name=instance.getName();
			String owner=instance.getOwner().getUUID();
			instance.delete();
			GPHUD.getLogger(name).log(SEVERE,"Instance passed termination time and is being deleted.");
			SL.im(owner,"=== GPHUD Warning ===\n"+
								"Instance: "+instance+"\n"+
								"-\n"+
								"This instance has passed its expiration date and has been deleted.");
		}
		Set<Instance> activeInstances=Region.getActiveInstances();
		for (Instance instance:Instance.getNonRetiringInstances()) {
			// all instances with zero active regions should have retireat set, otherwise we set it now.
			String name=instance.getName();
			if (!activeInstances.contains(instance)) {
				GPHUD.getLogger(name).log(WARNING,"Instance has no active regions and is being scheduled for termination.");
				instance.setRetireAt(UnixTime.getUnixTime()+Config.getGPHUDInstanceTimeout());
				instance.setRetireWarn(UnixTime.getUnixTime()+(Config.getGPHUDInstanceTimeout()/2));
				SL.im(instance.getOwner().getUUID(),"=== GPHUD Warning ===\n" +
															"Instance: "+instance+"\n"+
															"Deletes At: "+UnixTime.fromUnixTime(instance.retireAt(),"UTC")+" UTC\n"+
															"Deletes In: "+UnixTime.durationRelativeToNow(instance.retireAt(),true)+"\n"+
															"-\n"+
															"This instance no longer has any active regions associated with it and it will be automatically deleted when the above time has elapsed.");
			}
		}
		for (Instance instance:Instance.getWarnableInstances()) {
			// all instances with zero active regions should have retireat set, otherwise we set it now.
			String name=instance.getName();
			if (!activeInstances.contains(instance)) {
				GPHUD.getLogger(name).log(WARNING,"Instance passed termination warning time.");
				SL.im(instance.getOwner().getUUID(),"=== GPHUD Warning ===\n" +
															"Instance: "+instance+"\n"+
															"Deletes At: "+UnixTime.fromUnixTime(instance.retireAt(),"UTC")+" UTC\n"+
															"Deletes In: "+UnixTime.durationRelativeToNow(instance.retireAt(),true)+"\n"+
															"-\n"+
															"This instance no longer has any active regions associated with it and it will be automatically deleted when the above time has elapsed.\n"+
															"This is a second, and final warning.  You will be informed next once the instance has been deleted.");
				instance.setRetireWarn(UnixTime.getUnixTime()+Config.getGPHUDInstanceTimeout()+(60*60*24));
			}
		}
	}
	public static class PingTransmission extends Transmission {
		public PingTransmission(final Char character,
                                @Nonnull final JSONObject json,
                                final String url) {
            super(character, json, url);
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
