package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Implements Visit XP awards.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EventXP extends QuotaedXP {
	public EventXP(final int id) { super(id); }

	@Nonnull
	public String getName() { return "EventXP"; }

	@Nonnull
	public String poolName(final State st) {return "Events.EventXP";}

	@Nonnull
	public String quotaKV(final State st) {return "Events.EventXPLimit";}

	@Nonnull
	public String periodKV(final State st) { return "Events.EventXPPeriod"; }

	public Module getModule() { return Modules.get(null,"Events"); }

}
