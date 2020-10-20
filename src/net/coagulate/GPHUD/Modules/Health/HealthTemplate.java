package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class HealthTemplate {
	// ---------- STATICS ----------
	@Nonnull
	@Templater.Template(name="HEALTH",
	                    description="Character's health")
	public static String currentHealth(@Nonnull final State st,
	                                   final String key) {
		if (!st.hasModule("Health")) { return ""; }
		if (st.getCharacterNullable()==null) { return ""; }
		return st.getKV("Health.Health").intValue().toString();
	}

	@Nonnull
	@Templater.Template(name="HEALTHMAX",description="Character's maximum health")
	public static String maximumHealth(@Nonnull final State st,
									   final String key) {
		if (!st.hasModule("Health")) { return ""; }
		if (st.getCharacterNullable()==null) { return ""; }
		return st.getKV("health.initialhealth").intValue().toString();
	}

}
