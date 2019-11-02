package net.coagulate.GPHUD.Modules.Events;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Data.EventSchedule;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

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
	//TODO check if events is actually enabled in the instance
	public static void maintenance() {
		Set<EventSchedule> events = Event.getStartingEvents();
		//System.out.println("events:"+events.size());
		for (EventSchedule schedule : events) {
			Event e = schedule.getEvent();
			Set<Char> inzone = new HashSet<>();
			GPHUD.getLogger().log(INFO, "Starting event " + e.getName() + "#" + e.getId() + " for instance " + e.getInstance().getName());
			Map<String, String> config = e.loadKVs();
			String message = config.get("events.zonestartmessage");
			if (message != null && !message.isEmpty()) {
				for (Zone loc : e.getZones()) {
					loc.broadcastMessage("[Event:" + e.getName() + "] " + message);
					inzone.addAll(Char.getInZone(loc));
				}
			}
			message = config.get("events.broadcaststartmessage");
			if (message != null && !message.isEmpty()) {
				e.getInstance().broadcastMessage("[Event:" + e.getName() + "] " + message);
			}
			schedule.started();
			for (Char c : inzone) {
				schedule.startVisit(c);
			}
		}

		events = Event.getStoppingEvents();
		for (EventSchedule schedule : events) {
			Event e = schedule.getEvent();
			GPHUD.getLogger().log(INFO, "Stopping event " + e.getName() + "#" + e.getId() + " for instance " + e.getInstance().getName());
			Map<String, String> config = e.loadKVs();
			String message = config.get("events.zonestopmessage");
			if (message != null && !message.isEmpty()) {
				for (Zone loc : e.getZones()) {
					loc.broadcastMessage("[Event:" + e.getName() + "] " + message);
				}
			}
			message = config.get("events.broadcaststopmessage");
			if (message != null && !message.isEmpty()) {
				e.getInstance().broadcastMessage("[Event:" + e.getName() + "] " + message);
			}
			State temp = new State();
			temp.setInstance(e.getInstance());
			String limit = temp.getKV(e, "Events.ThisEventXPLimit");
			String minutes = temp.getKV(e, "Events.ThisEventXPMinutes");
			if (limit==null) { limit=temp.getKV("Events.ThisEventXPLimit").value(); }
			if (minutes==null) { minutes=temp.getKV( "Events.ThisEventXPMinutes").value(); }
			if (limit==null) { throw new SystemException("Limit is null in maintenance events awards closure"); }
			if (minutes==null) { throw new SystemException("Minutes is null in maintenance events awards closure"); }
			schedule.awardFinalXP(Integer.parseInt(minutes), Integer.parseInt(limit));
			schedule.ended();
			e.getInstance().pushConveyances(); // TODO also something that pushes XP update messages.  probably another generated conveyance or something :P
			int repeat = schedule.getRepeat();
			if (repeat == 0) {
				schedule.delete();
			} else {
				schedule.offsetSchedule(repeat);
			}
		}
	}

	public static void zoneTransition(State st, JSONObject response, Zone oldzone, Zone zone) {
		boolean debug = false;
		if (oldzone != null) { oldzone.validate(st); }
		if (zone != null) { zone.validate(st); }
		Set<EventSchedule> events = st.getInstance().getActiveEventSchedules();
		EventSchedule wasin = null;
		EventSchedule nowin = null;
		if (debug) {
			System.out.println("From " + oldzone.getName() + " to " + zone.getName() + " checking " + events.size());
		}
		for (EventSchedule es : events) {
			Event e = es.getEvent();
			for (Zone loczone : e.getZones()) {
				if (loczone == zone) { nowin = es; }
				if (loczone == oldzone) { wasin = es; }
			}
		}
		if (wasin == null && nowin == null) { return; } //didn't enter
		if (wasin == nowin) { return; } // didn't leave
		if (wasin != nowin) { //transitioned
			if (wasin != null) {
				String exitmessage = st.getKV(wasin.getEvent(), "events.eventexitmessage");
				if (exitmessage != null && !exitmessage.isEmpty()) {
					response.put("eventmessage1", "[Event:" + wasin.getEvent().getName() + "] " + exitmessage);
				}
				wasin.endVisit(st.getCharacter());
			}
			if (nowin != null) {
				String entrymessage = st.getKV(nowin.getEvent(), "events.evententrymessage");
				if (entrymessage != null && !entrymessage.isEmpty()) {
					response.put("eventmessage2", "[Event:" + nowin.getEvent().getName() + "] " + entrymessage);
					nowin.startVisit(st.getCharacter());
				}
			}
		}
	}


}
