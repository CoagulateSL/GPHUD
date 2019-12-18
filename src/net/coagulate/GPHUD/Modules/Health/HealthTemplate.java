package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class HealthTemplate {
	@Nonnull
	@Templater.Template(name="HEALTH", description="Character's health")
	public static String abilityPoints(@Nonnull final State st,
	                                   final String key)
	{
		if (st.getCharacterNullable()==null) { return ""; }
		return st.getKV("Health.Health").intValue().toString();
	}

}
