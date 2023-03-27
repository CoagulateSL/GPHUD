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
import javax.annotation.Nullable;
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
	private static final int ONE_DAY=60*60*24;
	
	/** Number of minutes before we ping the HUD to refresh the URL timer */
	public static final int PINGHUDINTERVAL   =15;
	/** Number of minutes before we ping a region server to refresh the URL timer */
	public static final int PINGSERVERINTERVAL=5;
	
	// ---------- STATICS ----------
	
	/** Call to Obj.purgeInactive */
	public static void purgeConnections() {
		try {
			final int purgecount=Obj.getPurgeInactiveCount();
			if (purgecount>0) {
				GPHUD.getLogger().log(FINE,"Purging "+purgecount+" disconnected objects");
				Obj.purgeInactive();
			}
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"purgeConnections Exceptioned!",e);
		}
	}
	
	/** Call Cookie.expire() */
	public static void purgeOldCookies() {
		try {
			final int before=Cookie.countAll();
			Cookie.expire();
			final int after=Cookie.countAll();
			if (before!=after) {
				GPHUD.getLogger().log(FINE,"Cookies cleaned from "+before+" to "+after+" ("+(after-before)+")");
			}
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Cookie expiration task exceptioned!",e);
		}
	}
	
	/**
	 * Call maintenance code for GPHUD.
	 * <p>
	 * Specifically:
	 * Purging expired script runs
	 * Refreshing character URLs
	 * Refreshing region URLs
	 * Starting events
	 * Purging old cookies
	 * Purging old URLs
	 * Running visit XP awards
	 * Updating instance region server status text
	 */
	public static void gphudMaintenance() {
		try {
			ScriptRun.maintenance();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance script run expiration caught an exception",e);
		}
		try {
			Maintenance.refreshCharacterURLs();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh character URLs caught an exception",e);
		}
		try {
			Maintenance.refreshRegionURLs();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance refresh region URLs caught an exception",e);
		}
		
		try {
			Maintenance.startEvents();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance start events caught an exception",e);
		}
		
		try {
			Maintenance.purgeOldCookies();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run purge cookies caught an exception",e);
		}
		
		try {
			Maintenance.purgeConnections();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run purge connections caught an exception",e);
		}
		
		try {
			Visit.runAwards();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance run awards run caught an exception",e);
		}
		
		try {
			Maintenance.updateInstances();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Maintenance update Instances caught an exception",e);
		}
	}
	
	/** Sents a ping command to any char that needs pinging (see Char.getPingable */
	public static void refreshCharacterURLs() {
		// note limit 0,30, we do no more than 30 of these per minute
		
		final Results results=Char.getPingable();
		if (results.notEmpty()) {
			//GPHUD.getLogger().log(FINE,"Pinging out to "+results.size()+" character URLs");
			for (final ResultsRow r: results) {
				//System.out.println("About to background for a callback to character:"+r.getString("name"));
				final JSONObject ping=new JSONObject().put("incommand","ping");
				final Transmission t=
						new PingTransmission(Char.get(r.getInt("characterid")),ping,r.getStringNullable("url"));
				t.start();
			}
		} //else { GPHUD.getLogger().log(FINE,"Pinging out to no character URLs"); }
	}
	
	/** Ping pingable regions (see Region.getPingable()) */
	public static void refreshRegionURLs() {
		// note limit 0,30, we do no more than 30 of these per minute
		
		final Results results=Region.getPingable();
		if (results.notEmpty()) {
			//GPHUD.getLogger().log(FINE,"Pinging out to "+results.size()+" region URLs");
			for (final ResultsRow r: results) {
				//System.out.println("About to background for a callback to region:"+r.getString("name"));
				final JSONObject ping=new JSONObject().put("incommand","ping");
				final Transmission t=new Transmission((Region)null,ping,r.getStringNullable("url"));
				t.start();
				try {
					Thread.sleep(1000);
				} catch (@Nonnull final InterruptedException ignored) {
				}
			}
		}
	}
	
	// remove cookies that are past expiry
	
	/** Wraps EventsMaintenance.maintenance() */
	public static void startEvents() {
		EventsMaintenance.maintenance();
	}
	
	// for calling from OTHER MAINTENANCE CODE (GPHUD from SL)
	
	/** Push status update to all instance region servers via Instance.updateStatus() */
	public static void updateInstances() {
		for (final Instance i: Instance.getInstances()) {
			try {
				//GPHUD.getLogger().log(FINER,"Pushing status update for instance "+i.getName());
				i.updateStatus();
			} catch (@Nonnull final Exception e) {
				if (Config.getDevelopment()) {
					GPHUD.getLogger()
					     .log(WARNING,"Exception while pushing status update for instance "+i.getNameSafe(),e);
				} else {
					GPHUD.getLogger()
					     .log(WARNING,"Exception while pushing status update for instance "+i.getNameSafe());
				}
			}
		}
	}
	
	/** Wraps Instance.quotaCredits() */
	public static void quotaCredits() {
		try {
			Instance.quotaCredits();
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Quota update caught an exception",e);
		}
		
	}
	
	private static boolean disablementNoted=false;
	
	/**
	 * Instance cleanup.
	 * <p>
	 * Retires any region whose URL is more than 2 weeks since refresh.
	 * Warn and flag any instance that then has no active regions.
	 * After the warning timer (half the expiration timer) send another warning about the instance
	 * Once the instance expiration time lapses the instance is deleted.
	 */
	public static void instanceCleanup() {
		if (!Config.getGPHUDAutoCleanInstances()) {
			if (!disablementNoted) {
				disablementNoted=true;
				GPHUD.getLogger().log(CONFIG,"Region/Instance automatic retirement is disabled.");
			}
			return;
		}
		// Look for regions we've not heard from in a while
		for (final Region region: Region.getTimedOut()) {
			final String instance=region.getInstance().getName();
			final String name=region.getName();
			region.retire();
			GPHUD.getLogger(instance).log(INFO,"Region "+name+" was retired due to inactivity.");
			SL.im(region.getInstance().getOwner().getUUID(),
			      "=== GPHUD Information ===\n"+"Instance: "+instance+"\n"+"Region: "+name+"\n"+"-\n"+
			      "The above region has been marked 'retired' due to extended inactivity.");
		}
		for (final Instance instance: Instance.getExpiredInstances()) {
			final String name=instance.getName();
			final String owner=instance.getOwner().getUUID();
			instance.delete();
			GPHUD.getLogger(name).log(SEVERE,"Instance passed termination time and is being deleted.");
			SL.im(owner,
			      "=== GPHUD Warning ===\n"+"Instance: "+instance+"\n"+"-\n"+
			      "This instance has passed its expiration date and has been deleted.");
		}
		final Set<Instance> activeInstances=Region.getActiveInstances();
		for (final Instance instance: Instance.getNonRetiringInstances()) {
			// all instances with zero active regions should have retireat set, otherwise we set it now.
			final String name=instance.getName();
			if (!activeInstances.contains(instance)) {
				GPHUD.getLogger(name)
				     .log(WARNING,"Instance has no active regions and is being scheduled for termination.");
				instance.setRetireAt(UnixTime.getUnixTime()+Config.getGPHUDInstanceTimeout());
				instance.setRetireWarn(UnixTime.getUnixTime()+(Config.getGPHUDInstanceTimeout()/2));
				SL.im(instance.getOwner().getUUID(),
				      "=== GPHUD Warning ===\n"+"Instance: "+instance+"\n"+"Deletes At: "+
				      UnixTime.fromUnixTime(instance.retireAt(),"UTC")+" UTC\n"+"Deletes In: "+
				      UnixTime.durationRelativeToNow(instance.retireAt(),true)+"\n"+"-\n"+
				      "This instance no longer has any active regions associated with it and it will be automatically deleted when the above time has elapsed.");
			}
		}
		for (final Instance instance: Instance.getWarnableInstances()) {
			// all instances with zero active regions should have retireat set, otherwise we set it now.
			final String name=instance.getName();
			if (!activeInstances.contains(instance)) {
				GPHUD.getLogger(name).log(WARNING,"Instance passed termination warning time.");
				SL.im(instance.getOwner().getUUID(),
				      "=== GPHUD Warning ===\n"+"Instance: "+instance+"\n"+"Deletes At: "+
				      UnixTime.fromUnixTime(instance.retireAt(),"UTC")+" UTC\n"+"Deletes In: "+
				      UnixTime.durationRelativeToNow(instance.retireAt(),true)+"\n"+"-\n"+
				      "This instance no longer has any active regions associated with it and it will be automatically deleted when the above time has elapsed.\n"+
				      "This is a second, and final warning.  You will be informed next once the instance has been deleted.");
				instance.setRetireWarn(UnixTime.getUnixTime()+Config.getGPHUDInstanceTimeout()+(ONE_DAY));
			}
		}
	}
	
	public static void truncateLogs() {
		Audit.truncate();
		Visit.truncate();
	}
	
	/** Wraps a simple ping check, if the transmission doesn't fail, then all is well... */
	public static class PingTransmission extends Transmission {
		/**
		 * Initiates a ping check against a particular character, using a particular json, on a particular URL
		 *
		 * @param character The character associated with this transmission (can be null)
		 * @param json      The JSON payload to ping with
		 * @param url       The URL to ping
		 */
		public PingTransmission(@Nullable final Char character,
		                        @Nonnull final JSONObject json,
		                        @Nonnull final String url) {
			super(character,json,url);
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
