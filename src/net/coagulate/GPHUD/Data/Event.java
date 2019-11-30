package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * An event - a timed occurance in a Zone
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Event extends TableRow {

	protected Event(int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A zone representation
	 */
	@Nonnull
	public static Event get(int id) {
		return (Event) factoryPut("Event", id, new Event(id));
	}

	/**
	 * Find an event by name
	 *
	 * @param instance Instance we're searching
	 * @param name     Name of event
	 * @return Event object
	 */
	@Nullable
	public static Event find(@Nonnull Instance instance, String name) {
		try {
			Integer eventid = GPHUD.getDB().dqi("select eventid from events where name like ? and instanceid=?", name, instance.getId());
			return get(eventid);
		} catch (NoDataException e) { return null; }
	}

	/**
	 * Create an event
	 *
	 * @param instance  Instance for the event
	 * @param eventName Name of the event
	 * @return The new event
	 * @throws UserException If the named event already exists
	 */
	@Nullable
	public static Event create(@Nonnull Instance instance, String eventName) throws UserException {
		Event event = find(instance, eventName);
		if (event != null) { throw new UserException("Event " + eventName + " already exists."); }
		GPHUD.getDB().d("insert into events(instanceid,name) values(?,?)", instance.getId(), eventName);
		event = find(instance, eventName);
		if (event == null) {
			throw new SystemException("Failed to create event " + eventName + " for instance " + instance.getName() + ", no error, just doesn't get found...");
		}
		return event;
	}

	/**
	 * Get all the events for an instance.
	 *
	 * @param instance Instance to get events for
	 * @return Set of Events
	 */
	@Nonnull
	public static Set<Event> getAll(@Nonnull Instance instance) {
		Set<Event> events = new TreeSet<>();
		for (ResultsRow r : GPHUD.getDB().dq("select eventid from events where instanceid=?", instance.getId())) {
			events.add(get(r.getInt()));
		}
		return events;
	}

	/**
	 * Get all currently active events for an instance.
	 *
	 * @param instance Instance to get active events for
	 * @return Set of Events that are currently active and started
	 */
	@Nonnull
	static Set<Event> getActive(@Nonnull Instance instance) {
		Set<Event> events = new TreeSet<>();
		int now = getUnixTime();
		for (ResultsRow r : GPHUD.getDB().dq("select eventsschedule.eventid from eventsschedule,events where eventsschedule.eventid=events.eventid and events.instanceid=? and eventsschedule.starttime<? and eventsschedule.endtime>? and eventsschedule.started=1", instance.getId(), now, now)) {
			events.add(get(r.getInt()));
		}
		return events;

	}

	static void wipeKV(@Nonnull Instance instance, String key) {
		String kvtable = "eventskvstore";
		String maintable = "events";
		String idcolumn = "eventid";
		GPHUD.getDB().d("delete from " + kvtable + " using " + kvtable + "," + maintable + " where " + kvtable + ".k like ? and " + kvtable + "." + idcolumn + "=" + maintable + "." + idcolumn + " and " + maintable + ".instanceid=?", key, instance.getId());
	}

	/**
	 * Get a list of events that have not started but should.
	 *
	 * @return Set of event schedules that need to be started.
	 */
	@Nonnull
	public static Set<EventSchedule> getStartingEvents() {
		Set<EventSchedule> start = new TreeSet<>();
		// find events where start time is in the past but "started"=0
		int now = getUnixTime();
		//System.out.println("select eventsscheduleid from eventsschedule where starttime<="+now+" and started=0 and endtime>"+now+";");
		for (ResultsRow r : GPHUD.getDB().dq("select eventsscheduleid from eventsschedule where starttime<=? and started=0 and endtime>?", now, now)) {
			//System.out.println(r.getInt());
			start.add(EventSchedule.get(r.getInt()));
		}
		return start;
	}

	/**
	 * Get a list of events that have started and need to stop.
	 *
	 * @return Set of event schedules that need to be stopped.
	 */
	@Nonnull
	public static Set<EventSchedule> getStoppingEvents() {
		Set<EventSchedule> stop = new TreeSet<>();
		// find events where start time is in the past but "started"=0
		int now = getUnixTime();
		//System.out.println("select eventsscheduleid from eventsschedule where starttime<="+now+" and started=0 and endtime>"+now+";");
		for (ResultsRow r : GPHUD.getDB().dq("select eventsscheduleid from eventsschedule where started=1 and endtime<=?", now)) {
			//System.out.println(r.getInt());
			stop.add(EventSchedule.get(r.getInt()));
		}
		return stop;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "events";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "eventid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/GPHUD/event/" + getId();
	}

	@Nullable
	public Instance getInstance() {
		Integer id = getInt("instanceid");
		if (id == null) { return null; }
		return Instance.get(id);
	}

	@Nonnull
	@Override
	public String getKVTable() {
		return "eventskvstore";
	}

	@Nonnull
	@Override
	public String getKVIdField() {
		return "eventid";
	}

	/**
	 * Get the set of locations (zones) affected by this event.
	 *
	 * @return Set of Zones
	 */
	@Nonnull
	public Set<Zone> getZones() {
		Set<Zone> zones = new TreeSet<>();
		for (ResultsRow r : dq("select zoneid from eventslocations where eventid=?", getId())) {
			Integer zone = r.getInt();
			zones.add(Zone.get(zone));
		}
		return zones;
	}

	/**
	 * Add a zone to this event
	 *
	 * @param zone Zone to add to the event
	 */
	public void addZone(@Nonnull Zone zone) {
		Integer count = dqi( "select count(*) from eventslocations where eventid=? and zoneid=?", getId(), zone.getId());
		if (count != 0) { return; }
		d("insert into eventslocations(eventid,zoneid) values(?,?)", getId(), zone.getId());
	}

	/**
	 * Delete a zone from this event
	 *
	 * @param zone Zone to remove from the event
	 */
	public void deleteZone(@Nonnull Zone zone) {
		d("delete from eventslocations where eventid=? and zoneid=?", getId(), zone.getId());
	}

	/**
	 * Get the schedules for this event
	 *
	 * @return Set of EventSchedules
	 */
	@Nonnull
	public Set<EventSchedule> getSchedule() {
		return EventSchedule.get(this);
	}

	/**
	 * Add a schedule to this event
	 *
	 * @param startdate UnixTime start of the event
	 * @param enddate   EndTime start of the event
	 * @param interval  How often to repeat the event, for repeating events
	 */
	public void addSchedule(int startdate, int enddate, int interval) {
		d("insert into eventsschedule(eventid,starttime,endtime,repeatinterval) values(?,?,?,?)", getId(), startdate, enddate, interval);
	}

	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Event / State Instance mismatch"); }
	}

	protected int getNameCacheTime() { return 60; } // events may become renamable, cache 60 seconds
	// perhaps flush the caches (to do) when this happens...
}

