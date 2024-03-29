package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Modules.Experience.EventXP;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

import static net.coagulate.Core.Tools.UnixTime.duration;
import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;

/**
 * A schedule (start time, stop time, repeat interval) for an event
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EventSchedule extends TableRow {
	
	/**
	 * Get a set of active events schedules that are in schedule and have started.
	 *
	 * @param instance Instance to get the events for
	 * @return Set of EventSchedules that are within start and end times, and have been started
	 */
	@Nonnull
	public static Set<EventSchedule> getActive(@Nonnull final Instance instance) {
		final Set<EventSchedule> events=new TreeSet<>();
		final int now=UnixTime.getUnixTime();
		for (final ResultsRow r: db().dq(
				"select eventsscheduleid from eventsschedule,events where eventsschedule.eventid=events.eventid and events.instanceid=? and "+
				"eventsschedule.starttime<? and eventsschedule.endtime>? and eventsschedule.started=1",
				instance.getId(),
				now,
				now)) {
			events.add(get(r.getInt()));
		}
		return events;
	}
	
	// ---------- STATICS ----------
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A zone representation
	 */
	@Nonnull
	public static EventSchedule get(final int id) {
		return (EventSchedule)factoryPut("EventSchedule",id,EventSchedule::new);
	}
	
	/**
	 * Get the set of event schedules for a given event.
	 *
	 * @param e Event to load schedules for
	 * @return Set of EventSchedules for this event
	 */
	@Nonnull
	public static Set<EventSchedule> get(@Nonnull final Event e) {
		final Set<EventSchedule> schedule=new TreeSet<>();
		for (final ResultsRow r: db().dq("select eventsscheduleid from eventsschedule where eventid=?",e.getId())) {
			schedule.add(get(r.getInt()));
		}
		return schedule;
	}
	
	protected EventSchedule(final int id) {
		super(id);
	}
	
	/**
	 * Get a set of active events schedules that are in schedule and have started.
	 *
	 * @param st State Instance to get the events for
	 * @return Set of EventSchedules that are within start and end times, and have been started
	 */
	@Nonnull
	public static Set<EventSchedule> getActive(@Nonnull final State st) {
		return getActive(st.getInstance());
	}
	
	/**
	 * Create a schedule
	 *
	 * @param event     The event this schedule initiates
	 * @param startdate UnixTime start of the event
	 * @param enddate   EndTime start of the event
	 * @param interval  How often to repeat the event, for repeating events
	 */
	public static void create(@Nonnull final Event event,final int startdate,final int enddate,final int interval) {
		db().d("insert into eventsschedule(eventid,starttime,endtime,repeatinterval) values(?,?,?,?)",
		       event.getId(),
		       startdate,
		       enddate,
		       interval);
	}
	
	// ----- Internal Statics -----
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getTableName() {
		return "eventsschedule";
	}
	
	@Nonnull
	@Override
	public String getIdColumn() {
		return "eventsscheduleid";
	}
	
	public void validate(@Nonnull final State st) {
		if (validated) {
			return;
		}
		validate();
		if (st.getInstance()!=getEvent().getInstance()) {
			throw new SystemConsistencyException("EventSchedule / State Instance mismatch");
		}
	}
	
	@Nonnull
	@Override
	public String getNameField() {
		throw new SystemImplementationException("Event Schedule does not support a naming attribute");
	}
	
	@Nullable
	@Override
	public String getLinkTarget() {
		return null;
	}
	
	@Nonnull
	@Override
	public String getName() {
		// doesn't really have a name, so we'll make one up...
		return pad(getIntNullable("starttime"),20)+"-"+pad(getIntNullable("endtime"),20)+"-"+getId();
	}
	
	@Nonnull
	@Override
	public String toString() {
		return "[Schedule#"+getId()+"="+getName()+"]";
	}
	
	@Nullable
	@Override
	public String getKVTable() {
		return null;
	}
	
	@Nullable
	@Override
	public String getKVIdField() {
		return null;
	}
	
	protected int getNameCacheTime() {
		return 0;
	} // doesn't support name, dont cache (will crash before even calling this)
	
	/**
	 * Get the event this schedule drives
	 *
	 * @return The connected Event
	 */
	@Nonnull
	public Event getEvent() {
		final int id=getInt("eventid");
		return Event.get(id);
	}
	
	/**
	 * Convert an EventSchedule to a table row.
	 *
	 * @return (table) Row representing the event
	 */
	@Nonnull
	public net.coagulate.GPHUD.Interfaces.Outputs.Row asRow(@Nonnull final String timezone) {
		final ResultsRow r=dqone("select * from eventsschedule where eventsscheduleid=?",getId());
		final net.coagulate.GPHUD.Interfaces.Outputs.Row ret=new net.coagulate.GPHUD.Interfaces.Outputs.Row();
		ret.add(fromUnixTime(r.getInt("starttime"),timezone));
		ret.add(fromUnixTime(r.getInt("endtime"),timezone));
		ret.add(duration(r.getInt("endtime")-r.getInt("starttime")));
		ret.add(r.getInt("started")==1?"ACTIVE":"");
		ret.add(duration(r.getInt("repeatinterval")));
		return ret;
	}
	
	/**
	 * Mark schedule as having been started.
	 * Used internally to trigger the "event starting" stuff, then we set this and known not to do that again.
	 * Purges the event visit log!
	 */
	public void started() {
		d("update eventsschedule set started=1 where eventsscheduleid=?",getId());
		d("delete from eventvisits where eventscheduleid=?",getId());
	}
	
	/**
	 * Mark schedule as having ended (technically set started to false)
	 * Purges the event visit log!
	 */
	public void ended() {
		d("update eventsschedule set started=0 where eventsscheduleid=?",getId());
		d("delete from eventvisits where eventscheduleid=?",getId());
	}
	
	/**
	 * Get the repeat interval for this event.
	 *
	 * @return Interval in seconds between repetitions of this event
	 */
	public int getRepeat() {
		return getInt("repeatinterval");
	}
	
	/**
	 * Offset the event by a given interval.
	 * Literally just adds the repeat to the start + end times to move it forward an interval.
	 *
	 * @param repeat Number of seconds to offset the event by
	 */
	public void offsetSchedule(final int repeat) {
		d("update eventsschedule set starttime=starttime+?,endtime=endtime+? where eventsscheduleid=?",
		  repeat,
		  repeat,
		  getId());
	}
	
	/**
	 * Delete this schedule.
	 */
	public void delete() {
		d("delete from eventsschedule where eventsscheduleid=?",getId());
	}
	
	/**
	 * Start a visit for this event + schedule.
	 *
	 * @param c Character that is part of the event.
	 */
	public void startVisit(@Nonnull final Char c) {
		d("insert into eventvisits(characterid,eventscheduleid,starttime) values(?,?,?)",
		  c.getId(),
		  getId(),
		  UnixTime.getUnixTime());
	}
	
	/**
	 * End a visit for this event + schedule.
	 *
	 * @param character Character that is leaving the event.
	 */
	public void endVisit(@Nonnull final Char character) {
		d("update eventvisits set endtime=? where characterid=? and eventscheduleid=? and endtime is null",
		  UnixTime.getUnixTime(),
		  character.getId(),
		  getId());
	}
	
	/**
	 * Award XP based on visit log.
	 * Ends all event visits!
	 *
	 * @param minutes Number of minutes per unit XP
	 * @param limit   Maximum number of XP to allocate
	 */
	public void awardFinalXP(final int minutes,final int limit) {
		endAllVisits();
		for (final ResultsRow r: dq(
				"select characterid,sum(endtime-starttime) as totaltime,sum(awarded) as awarded from eventvisits where eventscheduleid=? group by characterid",
				getId())) {
			final int charid=r.getInt("characterid");
			final int timespent=r.getInt("totaltime");
			final int awarded=r.getInt("awarded");
			int wanttoaward=timespent/(minutes*60);
			if (wanttoaward>limit) {
				wanttoaward=limit;
			}
			wanttoaward-=awarded;
			if (wanttoaward>0) {
				//TO DO AWARD EVENT XP HERE.
				// TODO notify the end users, in batch using the disseminate feature :P
				final State fake=new State();
				fake.setInstance(getEvent().getInstance());
				fake.setCharacter(Char.get(charid));
				fake.setAvatar(User.getSystem());
				final String description="Awarded for "+duration(timespent)+" spent at event "+getEvent().getName();
				final int finallyawarded=new EventXP(-1).cappedSystemAward(fake,wanttoaward,description);
				if (finallyawarded>0) {
					Audit.audit(fake,
					            Audit.OPERATOR.AVATAR,
					            null,
					            fake.getCharacter(),
					            "Add",
					            "EventXP",
					            null,
					            String.valueOf(finallyawarded),
					            description+" ("+finallyawarded+" of up to "+wanttoaward+" for time spent)");
				}
			}
		}
	}
	
	/**
	 * End all visits for this event + schedule.
	 */
	public void endAllVisits() {
		d("update eventvisits set endtime=? where eventscheduleid=? and endtime is null",
		  UnixTime.getUnixTime(),
		  getId());
	}
	
	@Nonnull
	public String describe(@Nonnull final String timezone) {
		final ResultsRow r=dqone("select * from eventsschedule where eventsscheduleid=?",getId());
		String ret=fromUnixTime(r.getInt("starttime"),timezone);
		ret+=" - "+fromUnixTime(r.getInt("endtime"),timezone);
		return ret;
	}
	
	// ----- Internal Instance -----
	@Nonnull
	private String pad(final Integer padme,final int howmuch) {
		final StringBuilder padder=new StringBuilder(String.valueOf(padme));
		while (padder.length()<howmuch) {
			padder.append(" ");
		}
		return padder.toString();
	}
	
}

