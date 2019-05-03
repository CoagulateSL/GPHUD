package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.State;

/**
 * @author Iain Price
 */
public class Damage {

	public static void apply(State st, Char targetchar, int damage, String reason) {
		st.setTarget(targetchar);
		int oldhealth = st.getTarget().getKV("Health.Health").intValue();
		boolean allownegative = st.getTarget().getKV("Health.allowNegative").boolValue();
		int newvalue = oldhealth - damage;
		if (!allownegative) {
			if (newvalue < 0) { newvalue = 0; }
		}
		st.setKV(targetchar, "health.health", newvalue + "");
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, targetchar, "SUBTRACT", "Health.Health", oldhealth + "", newvalue + "", "Damaged by " + damage + " for " + reason);
	}

	public static void heal(State st, Char targetchar, int healing, String reason) {
		st.setTarget(targetchar);
		int oldhealth = st.getTarget().getKV("Health.Health").intValue();
		int newvalue = oldhealth + healing;
		State targetstate = st.simulate(targetchar);
		int maxhealth = targetstate.getKV("health.initialhealth").intValue();
		if (newvalue > maxhealth) { newvalue = maxhealth; }
		st.setKV(targetchar, "health.health", newvalue + "");
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, targetchar, "ADD", "Health.Health", oldhealth + "", newvalue + "", "Healed by " + healing + " for " + reason);
	}
}
