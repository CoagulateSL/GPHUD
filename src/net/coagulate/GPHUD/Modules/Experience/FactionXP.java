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
public class FactionXP extends QuotaedXP {
	public FactionXP(final int id) {
		super(id);
	}
	
	// ---------- INSTANCE ----------
	public Module getModule() {
		return Modules.get(null,"Faction");
	}
	
	@Nonnull
	public String periodKV(final State st) {
		return "Faction.XPCycleLength";
	}
	
	@Nonnull
	public String poolName(final State st) {
		return "Faction.FactionXP";
	}
	
	@Nonnull
	public String quotaKV(final State st) {
		return "Faction.XPPerCycle";
	}
	
	@Nonnull
	public String getName() {
		return "FactionXP";
	}
	
}
