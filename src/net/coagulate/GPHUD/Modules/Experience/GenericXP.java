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
public class GenericXP extends QuotaedXP {
	private final String myname;
	
	public GenericXP(final String name) {
		super(-1);
		myname=name;
	}
	
	// ---------- INSTANCE ----------
	public Module getModule() {
		return Modules.get(null,"Experience");
	}
	
	@Nonnull
	public String periodKV(final State st) {
		return "Experience."+myname+"XPPeriod";
	}
	
	@Nonnull
	public String poolName(final State st) {
		return "Experience."+myname;
	}
	
	@Nonnull
	public String quotaKV(final State st) {
		return "Experience."+myname+"XPLimit";
	}
	
	@Nonnull
	public String getName() {
		return myname;
	}
	
}
