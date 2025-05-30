package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.CacheConfig;

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
	
	private static final Cache<Instance,Set<Event>> activeEventsCache=
			Cache.getCache("gphud/eventActive",CacheConfig.MINIMAL); // does this help?
	
	// ---------- STATICS ----------
	
	/**
	 * Create an event
	 *
	 * @param instance  Instance for the event
	 * @param eventName Name of the event
	 * @return The new event
	 *
	 * @throws UserException If the named event already exists
	 */
	@Nonnull
	public static Event create(@Nonnull final Instance instance,final String eventName) {
		Event event=find(instance,eventName);
		if (event!=null) {
			throw new UserInputDuplicateValueException("Event "+eventName+" already exists.");
		}
		db().d("insert into events(instanceid,name) values(?,?)",instance.getId(),eventName);
		event=find(instance,eventName);
		if (event==null) {
			throw new SystemConsistencyException(
					"Failed to create event "+eventName+" for instance "+instance.getName()+
					", no error, just doesn't get found...");
		}
		return event;
	}
	
	/**
	 * Find an event by name
	 *
	 * @param instance Instance we're searching
	 * @param name     Name of event
	 * @return Event object
	 */
	@Nullable
	public static Event find(@Nonnull final Instance instance,final String name) {
		try {
			final int eventid=db().dqiNotNull("select eventid from events where name like ? and instanceid=?",
			                                  name,
			                                  instance.getId());
			return get(eventid);
		} catch (@Nonnull final NoDataException e) {
			return null;
		}
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A zone representation
	 */
	@Nonnull
	public static Event get(final int id) {
		return (Event)factoryPut("Event",id,Event::new);
	}
	
	/**
	 * Get all the events for an instance.
	 *
	 * @param instance Instance to get events for
	 * @return Set of Events
	 */
	@Nonnull
	public static Set<Event> getAll(@Nonnull final Instance instance) {
		final Set<Event> events=new TreeSet<>();
		for (final ResultsRow r: db().dq("select eventid from events where instanceid=?",instance.getId())) {
			events.add(get(r.getInt()));
		}
		return events;
	}
	
	/**
	 * Get all the events for an instance.
	 *
	 * @param st State Instance to get events for
	 * @return Set of Events
	 */
	@Nonnull
	public static Set<Event> getAll(@Nonnull final State st) {
		return getAll(st.getInstance());
	}
	
	protected Event(final int id) {
		super(id);
	}
	
	/**
	 * Get a list of events that have not started but should.
	 *
	 * @return Set of event schedules that need to be started.
	 */
	@Nonnull
	public static Set<EventSchedule> getStartingEvents() {
		final Set<EventSchedule> start=new TreeSet<>();
		// find events where start time is in the past but "started"=0
		final int now=getUnixTime();
		//System.out.println("select eventsscheduleid from eventsschedule where starttime<="+now+" and started=0 and endtime>"+now+";");
		for (final ResultsRow r: db().dq(
				"select eventsscheduleid from eventsschedule where starttime<=? and started=0 and endtime>?",
				now,
				now)) {
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
		final Set<EventSchedule> stop=new TreeSet<>();
		// find events where start time is in the past but "started"=0
		final int now=getUnixTime();
		//System.out.println("select eventsscheduleid from eventsschedule where starttime<="+now+" and started=0 and endtime>"+now+";");
		for (final ResultsRow r: db().dq("select eventsscheduleid from eventsschedule where started=1 and endtime<=?",
		                                 now)) {
			//System.out.println(r.getInt());
			stop.add(EventSchedule.get(r.getInt()));
		}
		return stop;
	}
	
	/**
	 * Get all currently active events for an instance.
	 *
	 * @param instance Instance to get active events for
	 * @return Set of Events that are currently active and started
	 */
	@Nonnull
	public static Set<Event> getActive(@Nonnull final Instance instance) {
		return activeEventsCache.get(instance,()->{
			final Set<Event> events=new TreeSet<>();
			final int now=getUnixTime();
			for (final ResultsRow r: db().dq(
					"select eventsschedule.eventid from eventsschedule,events where eventsschedule.eventid=events.eventid and events.instanceid=? and eventsschedule.starttime<? and eventsschedule.endtime>? and eventsschedule.started=1",
					instance.getId(),
					now,
					now)) {
				events.add(get(r.getInt()));
			}
			return events;
		});
	}
	
	/**
	 * Get all currently active events for an instance.
	 *
	 * @param st State Instance to get active events for
	 * @return Set of Events that are currently active and started
	 */
	@Nonnull
	public static Set<Event> getActive(@Nonnull final State st) {
		return getActive(st.getInstance());
	}
	
	// ----- Internal Statics -----
	static void wipeKV(@Nonnull final Instance instance,final String key) {
		final String kvtable="eventskvstore";
		final String maintable="events";
		final String idcolumn="eventid";
		db().d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+
		       idcolumn+"="+maintable+"."+idcolumn+" and "+maintable+".instanceid=?",key,instance.getId());
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getTableName() {
		return "events";
	}
	
	@Nonnull
	@Override
	public String getIdColumn() {
		return "eventid";
	}
	
	public void validate(@Nonnull final State st) {
		if (validated) {
			return;
		}
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Event / State Instance mismatch");
		}
	}
	
	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}
	
	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/GPHUD/event/"+getId();
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
	
	protected int getNameCacheTime() {
		return 60*60;
	} // events may become renamable, cache 60 seconds
	
	/**
	 * Get the instance this event relates to
	 *
	 * @return The instance
	 */
	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}
	
	/**
	 * Get the set of locations (zones) affected by this event.
	 *
	 * @return Set of Zones
	 */
	@Nonnull
	public Set<Zone> getZones() {
		final Set<Zone> zones=new TreeSet<>();
		for (final ResultsRow r: dq("select zoneid from eventslocations where eventid=?",getId())) {
			final int zone=r.getInt();
			zones.add(Zone.get(zone));
		}
		return zones;
	}
	
	/**
	 * Add a zone to this event
	 *
	 * @param zone Zone to add to the event
	 */
	public void addZone(@Nonnull final Zone zone) {
		final int count=dqinn("select count(*) from eventslocations where eventid=? and zoneid=?",getId(),zone.getId());
		if (count!=0) {
			return;
		}
		d("insert into eventslocations(eventid,zoneid) values(?,?)",getId(),zone.getId());
	}
	
	/**
	 * Delete a zone from this event
	 *
	 * @param zone Zone to remove from the event
	 */
	public void deleteZone(@Nonnull final Zone zone) {
		d("delete from eventslocations where eventid=? and zoneid=?",getId(),zone.getId());
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
	public void addSchedule(final int startdate,final int enddate,final int interval) {
		EventSchedule.create(this,startdate,enddate,interval);
	}
}

