package net.coagulate.GPHUD.Modules.Events;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * The commands that back events.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EventsCommands {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Creates a new event with an empty configuration",
	          requiresPermission="Events.Create")
	public static Response create(@Nonnull final State st,
	                              @Arguments(description="Name for the event",
	                                         type=ArgumentType.TEXT_ONELINE,
	                                         max=128) final String eventName) {
		Event.create(st.getInstance(),eventName);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Create","Event",null,eventName,"Event created");
		return new OKResponse("Created event "+eventName);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Add a zone to an event",
	          requiresPermission="Events.Locations")
	public static Response addLocation(@Nonnull final State st,
	                                   @Nonnull @Arguments(description="Event to add the zone to",
	                                                       type=ArgumentType.EVENT) final Event event,
	                                   @Nonnull @Arguments(description="Zone to add to the event",
	                                                       type=ArgumentType.ZONE) final Zone zone) {
		zone.validate(st);
		event.validate(st);
		event.addZone(zone);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"AddLocation","Event/"+event.getName(),null,zone.getName(),"Added zone to event");
		return new OKResponse("Added zone "+zone.getName()+" to event "+event.getName());
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Delete zone from event",
	          requiresPermission="Events.Locations")
	public static Response deleteLocation(@Nonnull final State st,
	                                      @Nonnull @Arguments(description="Event to remove the zone from",
	                                                          type=ArgumentType.EVENT) final Event event,
	                                      @Nonnull @Arguments(description="Zone to remove from the event",
	                                                          type=ArgumentType.ZONE) final Zone zone) {
		zone.validate(st);
		event.validate(st);
		event.deleteZone(zone);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"DeleteLocation","Event/"+event.getName(),zone.getName(),null,"Removed zone from event");
		return new OKResponse("Removed zone "+zone.getName()+" from event "+event.getName());
	}


}
