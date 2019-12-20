package net.coagulate.GPHUD.Modules.Events;

import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.StaticSideSubMenu;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * Dynamic events menu
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EventsModule extends ModuleAnnotation {

	public EventsModule(final String name,
	                    final ModuleDefinition def) {
		super(name,def);
	}

	@Nonnull
	@Override
	public Set<SideSubMenu> getSideSubMenus(@Nonnull final State st) {
		final Set<Event> events=st.getInstance().getEvents();
		final Set<SideSubMenu> ret=new HashSet<>();
		for (final Event event: events) {
			ret.add(new StaticSideSubMenu(event.getName(),event.getId(),"/events/view/"+event.getId(),""));
		}
		return ret;
	}


}
