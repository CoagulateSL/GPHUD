package net.coagulate.GPHUD.Modules.Events;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserRemoteFailureException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Data.EventSchedule;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.logging.Level.INFO;

/**
 * Starts events.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class EventsMaintenance {
	// ---------- STATICS ----------
	//TODO check if events is actually enabled in the instance
	public static void maintenance() {
		Set<EventSchedule> events=Event.getStartingEvents();
		//System.out.println("events:"+events.size());
		for (final EventSchedule schedule: events) {
			final Event e=schedule.getEvent();
			final Set<Char> inzone=new HashSet<>();
			GPHUD.getLogger().log(INFO,"Starting event "+e.getName()+"#"+e.getId()+" for instance "+e.getInstance().getName());
			final Map<String,String> config=e.loadKVs();
			String message=config.get("events.zonestartmessage");
			for (final Zone loc: e.getZones()) {
				if (message!=null && !message.isEmpty()) {
					try { loc.broadcastMessage("[Event:"+e.getName()+"] "+message); }
					catch (UserRemoteFailureException ignore) {}
				}
				inzone.addAll(Char.getInZone(loc));
			}
			message=config.get("events.broadcaststartmessage");
			if (message!=null && !message.isEmpty()) {
				try { e.getInstance().broadcastMessage("[Event:"+e.getName()+"] "+message); }
				catch (UserRemoteFailureException ignore) {}
			}
			schedule.started();
			for (final Char c: inzone) {
				schedule.startVisit(c);
			}
		}

		events=Event.getStoppingEvents();
		for (final EventSchedule schedule: events) {
			final Event e=schedule.getEvent();
			GPHUD.getLogger().log(INFO,"Stopping event "+e.getName()+"#"+e.getId()+" for instance "+e.getInstance().getName());
			final Map<String,String> config=e.loadKVs();
			String message=config.get("events.zonestopmessage");
			if (message!=null && !message.isEmpty()) {
				for (final Zone loc: e.getZones()) {
					try { loc.broadcastMessage("[Event:"+e.getName()+"] "+message); }
					catch (UserRemoteFailureException ignore) {}
				}
			}
			message=config.get("events.broadcaststopmessage");
			if (message!=null && !message.isEmpty()) {
				try { e.getInstance().broadcastMessage("[Event:"+e.getName()+"] "+message); }
				catch (UserRemoteFailureException ignore) {}
			}
			final State temp=new State();
			temp.setInstance(e.getInstance());
			String limit=temp.getKV(e,"Events.ThisEventXPLimit");
			String minutes=temp.getKV(e,"Events.ThisEventXPMinutes");
			if (limit==null) { limit=temp.getKV("Events.ThisEventXPLimit").value(); }
			if (minutes==null) { minutes=temp.getKV("Events.ThisEventXPMinutes").value(); }
			if (limit==null) {
				throw new SystemConsistencyException("Limit is null in maintenance events awards closure");
			}
			if (minutes==null) {
				throw new SystemConsistencyException("Minutes is null in maintenance events awards closure");
			}
			schedule.awardFinalXP(Integer.parseInt(minutes),Integer.parseInt(limit));
			schedule.ended();
			e.getInstance().pushConveyances(); // TODO also something that pushes XP update messages.  probably another generated conveyance or something :P
			final int repeat=schedule.getRepeat();
			if (repeat==0) {
				schedule.delete();
			}
			else {
				schedule.offsetSchedule(repeat);
			}
		}
	}

	public static void zoneTransition(@Nonnull final State st,
	                                  @Nonnull final JSONObject response,
	                                  @Nullable final Zone oldzone,
	                                  @Nullable final Zone zone) {
		final boolean debug=false;
		if (oldzone!=null) { oldzone.validate(st); }
		if (zone!=null) { zone.validate(st); }
		final Set<EventSchedule> events=EventSchedule.getActive(st);
		EventSchedule wasin=null;
		EventSchedule nowin=null;
		for (final EventSchedule es: events) {
			final Event e=es.getEvent();
			for (final Zone loczone: e.getZones()) {
				if (loczone==zone) { nowin=es; }
				if (loczone==oldzone) { wasin=es; }
			}
		}
		if (wasin==null && nowin==null) { return; } //didn't enter
		if (wasin==nowin) { return; } // didn't leave
		//noinspection ConstantConditions // simply looks clearer
		if (wasin!=nowin) { //transitioned
			if (wasin!=null) {
				final String exitmessage=st.getKV(wasin.getEvent(),"events.eventexitmessage");
				if (exitmessage!=null && !exitmessage.isEmpty()) {
					response.put("eventmessage1","[Event:"+wasin.getEvent().getName()+"] "+exitmessage);
				}
				wasin.endVisit(st.getCharacter());
			}
			if (nowin!=null) {
				final String entrymessage=st.getKV(nowin.getEvent(),"events.evententrymessage");
				if (entrymessage!=null && !entrymessage.isEmpty()) {
					response.put("eventmessage2","[Event:"+nowin.getEvent().getName()+"] "+entrymessage);
				}
				nowin.startVisit(st.getCharacter());
			}
		}
	}


}
