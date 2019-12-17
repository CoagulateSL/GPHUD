package net.coagulate.GPHUD.Modules.Events;

import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.Hidden;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Configuration.GenericConfiguration;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Web pages related to managing events.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EventsPages {


	@URLs(url = "/events")
	public static void listEvents(@Nonnull final State st, final SafeMap values) {
		final Form f = st.form();
		f.noForm();
		f.add(new TextHeader("Events Listing"));
		final Set<Event> events = st.getInstance().getEvents();
		for (final Event e : events) {
			f.add(new Link(e.getName(), e.getLinkTarget()));
			f.add("<br>");
		}
		f.add("<br>");
		if (st.hasPermission("events.create")) {
			f.add(new Form(st, true, "./events/create", "Create Event"));
		}

	}

	@URLs(url = "/events/create", requiresPermission = "events.create")
	public static void createEvent(@Nonnull final State st, @Nonnull final SafeMap values) {
		final Form f = st.form();
		Modules.simpleHtml(st, "events.create", values);
		final Command c = Modules.getCommandNullable(st, "events.create");
	}


	@URLs(url = "/event/*")
	public static void viewEvent(@Nonnull final State st, final SafeMap values) throws UserException, SystemException {
		//System.out.println(st.uri);
		final String[] split = st.getDebasedURL().split("/");
		//System.out.println(split.length);
		final String id = split[split.length - 1];
		final Event e = Event.get(Integer.parseInt(id));
		viewEvent(st, values, e, false);
	}

	public static void viewEvent(@Nonnull final State st, final SafeMap values, @Nonnull final Event e, final boolean brief) throws UserException, SystemException {
		e.validate(st);
		final Form f = st.form();
		f.noForm();
		f.add(new TextHeader("Event: " + e.getName()));

		f.add(new TextSubHeader("Zones"));
		final Table z = new Table();
		f.add(z);
		final Set<Zone> zones = e.getZones();
		for (final Zone location : zones) {
			z.openRow().add(location);
			if (st.hasPermission("events.locations")) {
				z.add(new Form(st, true, "./deletelocation", "Remove Zone", "event", e.getName(), "zone", location.getName()));
			}
		}
		if (st.hasPermission("events.locations")) {
			z.openRow().add(new Cell(new Form(st, true, "./addlocation", "Add Zone", "event", e.getName()), 2));
		}

		final String tz = st.getAvatar().getTimeZone();
		//f.add(new TextSubHeader("Schedule"));
		final Set<EventSchedule> schedule = e.getSchedule();
		final Table sch = new Table();
		sch.border(true);
		f.add(sch);
		sch.add(new HeaderRow().add("Start (" + tz + ")").add("End (" + tz + ")").add("Duration").add("Active").add("Repeats"));
		for (final EventSchedule es : schedule) {
			final Row esrow = es.asRow(tz);
			if (st.hasPermission("events.schedule")) {
				esrow.add(new Form(st, true, "./deleteschedule", "Remove", "eventscheduleid", es.getId() + ""));
			}
			sch.add(esrow);
		}
		if (st.hasPermission("events.schedule")) {
			f.add(new Form(st, true, "./addschedule", "Add Schedule", "event", e.getName()));
		}
		f.add(new TextSubHeader("KV influences"));
		GenericConfiguration.page(st, values, e, st);
	}

	@URLs(url = "/event/addlocation", requiresPermission = "events.locations")
	public static void addLocation(@Nonnull final State st, @Nonnull final SafeMap values) {
		Modules.simpleHtml(st, "events.addlocation", values);
	}

	@URLs(url = "/event/deletelocation", requiresPermission = "events.locations")
	public static void deleteLocation(@Nonnull final State st, @Nonnull final SafeMap values) {
		Modules.simpleHtml(st, "events.deletelocation", values);
	}

	@URLs(url = "/event/deleteschedule", requiresPermission = "events.schedule")
	public static void deleteSchedule(@Nonnull final State st, @Nonnull final SafeMap values) {
		final String id = values.get("eventscheduleid");
		final EventSchedule es = EventSchedule.get(Integer.parseInt(id));
		es.validate(st);
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "DeleteSchedule", es.getEvent().getName(), es.describe("America/Los_Angeles") + " SLT", null, "Avatar deleted event schedule");
		es.delete();
		throw new RedirectionException(values);
	}

	@URLs(url = "/event/addschedule", requiresPermission = "events.schedule")
	public static void addSchedule(@Nonnull final State st, @Nonnull final SafeMap values) {
		final String eventname = values.get("event");
		final String defaulttz = st.getAvatar().getTimeZone();
		final Event event = Event.find(st.getInstance(), eventname);
		if (event==null) { st.form().add(new TextError("Event no longer exists")); return; }
		event.validate(st);
		if ("Add".equals(values.get("Add"))) {
			try {
				final int startdate = DateTime.outputDateTime("Start", values, defaulttz);
				final int enddate = DateTime.outputDateTime("End", values, defaulttz);
				final int repeat = DateTime.outputInterval("Repeat", values);
				event.addSchedule(startdate, enddate, repeat);
				Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "AddSchedule", event.getName(), null, DateTime.fromUnixTime(startdate, "America/Los_Angeles") + " SLT repeat " + UnixTime.duration(repeat), "Schedule added to event");
				throw new RedirectionException(values);
			} catch (@Nonnull final UserException e) {
				st.form().add(new TextError(e.getMessage()));
			}
		}
		final Form f = st.form();
		f.add(new Hidden("event", eventname));
		f.add(new TextSubHeader("Schedule event " + eventname));
		final String tz = st.getAvatar().getTimeZone();
		final Table t = new Table();
		f.add(t);
		t.add(new HeaderRow().add("").add("DD").add("MM").add("YYYY").add("HH").add("MM"));
		t.add(DateTime.inputDateTimeRow("Start", values, tz));
		t.add(DateTime.inputDateTimeRow("End", values, tz));
		t.add(DateTime.inputIntervalRow("Repeat", values, true));
		t.add(new Cell(new Button("Add"), 999));

		f.br();
		f.add("Note DAY MONTH YEAR ordering, also note the HOURS is 24 hour clock!");

	}


}
