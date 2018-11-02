package net.coagulate.GPHUD.Data;

import java.util.Set;
import java.util.TreeSet;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import static net.coagulate.Core.Tools.UnixTime.duration;
import static net.coagulate.Core.Tools.UnixTime.fromUnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Experience.EventXP;
import net.coagulate.GPHUD.State;

/** A schedule (start time, stop time, repeat interval) for an event
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EventSchedule extends TableRow {
    
    /** Factory style constructor
     * 
     * @param id the ID number we want to get
     * @return A zone representation
     */
    public static EventSchedule get(int id) {
        return (EventSchedule)factoryPut("EventSchedule",id,new EventSchedule(id));
    }

    /** Get a set of active events schedules that are in schedule and have started.
     * 
     * @param instance Instance to get the events for
     * @return Set of EventSchedules that are within start and end times, and have been started
     */
    static Set<EventSchedule> getActive(Instance instance) {
        Set<EventSchedule> events=new TreeSet<>();
        int now=UnixTime.getUnixTime();
        for (ResultsRow r:GPHUD.getDB().dq("select eventsscheduleid from eventsschedule,events where eventsschedule.eventid=events.eventid and events.instanceid=? and eventsschedule.starttime<? and eventsschedule.endtime>? and eventsschedule.started=1",instance.getId(),now,now)) {
            events.add(get(r.getInt()));
        }
        return events;        
    }

    protected EventSchedule(int id) { super(id); }

    @Override
    public String getTableName() {
        return "eventsschedule";
    }

    @Override
    public String getIdField() {
        return "eventsscheduleid";
    }

    @Override
    public String getNameField() {
        throw new SystemException("Event Schedule does not support a naming attribute");
    }

    @Override
    public String getLinkTarget() {
           return null;
    }
    public Event getEvent() {
        Integer id=getInt("eventid");
        if (id==null) { return null; }
        return Event.get(id);
    }
    @Override
    public String getKVTable() {
        return null;
    }

    @Override
    public String getKVIdField() {
        return null;
    }

    /** Get the set of event schedules for a given event.
     * 
     * @param e Event to load schedules for
     * @return Set of EventSchedules for this event
     */
    public static Set<EventSchedule> get(Event e) {
        Set<EventSchedule> schedule=new TreeSet<>();
        for (ResultsRow r:GPHUD.getDB().dq("select eventsscheduleid from eventsschedule where eventid=?",e.getId())) {
            schedule.add(get(r.getInt()));
        }
        return schedule;
    }

    /** Convert an EventSchedule to a table row.
     * 
     * @return (table) Row representing the event
     */
    public net.coagulate.GPHUD.Interfaces.Outputs.Row asRow(String timezone) {
        ResultsRow r=dqone(true,"select * from eventsschedule where eventsscheduleid=?",getId());
        net.coagulate.GPHUD.Interfaces.Outputs.Row ret=new net.coagulate.GPHUD.Interfaces.Outputs.Row();
        ret.add(fromUnixTime(r.getInt("starttime"),timezone));
        ret.add(fromUnixTime(r.getInt("endtime"),timezone));
        ret.add(duration(r.getInt("endtime")-r.getInt("starttime")));
        ret.add(r.getInt("started")==1?"ACTIVE":"");
        ret.add(duration(r.getInt("repeatinterval")));
        return ret;
    }

    /** Mark schedule as having been started.
     * Used internally to trigger the "event starting" stuff, then we set this and known not to do that again.
     * Purges the event visit log!
     */
    public void started() {
        d("update eventsschedule set started=1 where eventsscheduleid=?",getId());
        d("delete from eventvisits where eventscheduleid=?",getId());
    }
    /** Mark schedule as having ended (technically set started to false)
     * Purges the event visit log!
     */
    public void ended() {
        d("update eventsschedule set started=0 where eventsscheduleid=?",getId());
        d("delete from eventvisits where eventscheduleid=?",getId());
    }

    /** Get the repeat interval for this event.
     * 
     * @return Interval in seconds between repetitions of this event
     */
    public int getRepeat() {
        return getInt("repeatinterval");
    }

    /** Offset the event by a given interval.
     * Literally just adds the repeat to the start + end times to move it forward an interval.
     * @param repeat Number of seconds to offset the event by
     */
    public void offsetSchedule(int repeat) {
        d("update eventsschedule set starttime=starttime+?,endtime=endtime+? where eventsscheduleid=?",repeat,repeat,getId());
    }

    /** Delete this schedule.
     * 
     */
    public void delete() {
        d("delete from eventsschedule where eventsscheduleid=?",getId());
    }

    /** Start a visit for this event + schedule.
     * 
     * @param c Character that is part of the event.
     */
    public void startVisit(Char c) {
        d("insert into eventvisits(characterid,eventscheduleid,starttime) values(?,?,?)",c.getId(),getId(),UnixTime.getUnixTime());
    }

    /** End a visit for this event + schedule.
     * 
     * @param character Character that is leaving the event.
     */
    public void endVisit(Char character) {
        d("update eventvisits set endtime=? where characterid=? and eventscheduleid=? and endtime is null",UnixTime.getUnixTime(),character.getId(),getId());
    }

    /** End all visits for this event + schedule.
     * 
     */
    public void endAllVisits() {
        d("update eventvisits set endtime=? where eventscheduleid=? and endtime is null",UnixTime.getUnixTime(),getId());
    }

    /** Award XP based on visit log.
     * Ends all event visits!
     * @param minutes Number of minutes per unit XP
     * @param limit Maximum number of XP to allocate
     */
    public void awardFinalXP(int minutes, int limit) {
        endAllVisits();
        for (ResultsRow r:dq("select characterid,sum(endtime-starttime) as totaltime,sum(awarded) as awarded from eventvisits where eventscheduleid=? group by characterid",getId())) {
            int charid=r.getInt("characterid");
            int timespent=r.getInt("totaltime");
            int awarded=r.getInt("awarded");
            int wanttoaward=timespent/(minutes*60);
            if (wanttoaward>limit) { wanttoaward=limit; }
            wanttoaward-=awarded;
            if (wanttoaward>0) {
                //TO DO AWARD EVENT XP HERE.
                // TODO notify the end users, in batch using the disseminate feature :P
                State fake=new State(); fake.setInstance(getEvent().getInstance()); fake.setCharacter(Char.get(charid)); fake.setAvatar(Avatar.getSystem(fake));
                String description="Awarded for "+duration(timespent)+" spent at event "+getEvent().getName();
                int finallyawarded=new EventXP(-1).cappedSystemAward(fake, wanttoaward, description);
                if (finallyawarded>0) { Audit.audit(fake, Audit.OPERATOR.AVATAR, null, null, fake.getCharacter(), "Add", "EventXP", null, finallyawarded+"", description+" ("+finallyawarded+" of up to "+wanttoaward+")"); }
            }
        }
    }

    public String describe(String timezone) {
        ResultsRow r=dqone(true,"select * from eventsschedule where eventsscheduleid=?",getId());
        String ret=fromUnixTime(r.getInt("starttime"),timezone);
        ret+=" - "+fromUnixTime(r.getInt("endtime"),timezone);
        return ret;
    }

    public void validate(State st) throws SystemException {
        if (validated) { return; }
        validate();
        if (st.getInstance()!=getEvent().getInstance()) { throw new SystemException("EventSchedule / State Instance mismatch"); }
    }
    
    protected int getNameCacheTime() { return 0; } // doesn't support name, dont cache (will crash before even calling this)

    @Override
    public String getName() {
        // doesn't really have a name, so we'll make one up...
        return pad(getInt("starttime"),20)+"-"+pad(getInt("endtime"),20)+"-"+getId();
    }
    
    private String pad(Integer padmeint,int howmuch) {
        String padme=padmeint+"";
        while (padme.length()<howmuch) { padme+=" "; }
        return padme;
    }
    
}

