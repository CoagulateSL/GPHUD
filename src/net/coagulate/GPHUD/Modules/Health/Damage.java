package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Damage {

	// ---------- STATICS ----------
	public static void apply(@Nonnull final State st,
	                         @Nonnull final Char targetchar,
	                         final int damage,
	                         final String reason) {
		st.setTarget(targetchar);
		final int oldhealth = st.getTarget().getKV("Health.Health").intValue();
		final boolean allownegative = st.getTarget().getKV("Health.allowNegative").boolValue();
		int newvalue = oldhealth - damage;
		if (!allownegative) {
			if (newvalue < 0) {
				newvalue = 0;
			}
		}
		st.setKV(targetchar, "health.health", String.valueOf(newvalue));
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, targetchar, "SUBTRACT", "Health.Health", String.valueOf(oldhealth), String.valueOf(newvalue), "Damaged by " + damage + " for " + reason);
	}

	public static void heal(@Nonnull final State st,
	                        @Nonnull final Char targetchar,
	                        final int healing,
	                        final String reason) {
		st.setTarget(targetchar);
		final int oldhealth = st.getTarget().getKV("Health.Health").intValue();
		int newvalue = oldhealth + healing;
		final State targetstate = st.simulate(targetchar);
		final int maxhealth = targetstate.getKV("health.initialhealth").intValue();
		if (newvalue > maxhealth) {
			newvalue = maxhealth;
		}
		st.setKV(targetchar, "health.health", String.valueOf(newvalue));
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, targetchar, "ADD", "Health.Health", String.valueOf(oldhealth), String.valueOf(newvalue), "Healed by " + healing + " for " + reason);
	}
}
