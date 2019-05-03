package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.State;

/**
 * @author Iain Price
 */
public class HealthTemplate {
	@Templater.Template(name = "HEALTH", description = "Character's health")
	public static String abilityPoints(State st, String key) {
		if (st.getCharacterNullable() == null) { return ""; }
		return st.getKV("Health.Health").intValue().toString();
	}

}
