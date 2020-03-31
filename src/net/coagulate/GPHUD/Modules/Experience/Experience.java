package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.EXPERIENCE;

/**
 * Generic utilities.
 *
 * @author iain
 */
public abstract class Experience {
	// ---------- STATICS ----------
	@Nonnull
	@Template(name="TOTALXP",
	          description="Total experience")
	public static String getTotalXP(@Nonnull final State st,
	                                final String key) {
		if (!st.hasModule("Experience")) { return ""; }
		if (st.getCharacterNullable()==null) { return ""; }
		return getExperience(st,st.getCharacter())+"";
	}

	@Nonnull
	@Template(name="LEVEL",
	          description="Current Level")
	public static String getLevel(@Nonnull final State st,
	                              final String key) {
		if (!st.hasModule("Experience")) { return ""; }
		if (st.getCharacterNullable()==null) { return ""; }
		return toLevel(st,getExperience(st,st.getCharacter()))+"";
	}

	public static int toLevel(@Nonnull final State st,
	                          final int xp) {
		if (!st.hasModule("Experience")) { return 0; }
		final int step=st.getKV("Experience.LevelXPStep").intValue();
		int tolevel=0;
		for (int i=0;i<=1000;i++) {
			tolevel=(int) (tolevel+Math.floor(((float) i)/((float) step))+1);
			if (tolevel>xp) { return i; }
		}
		return 1000;

	}

	public static int getExperience(@Nonnull final State st,
	                                @Nonnull final Char character) {
		int sum=0;
		if (Modules.get(null,"experience").isEnabled(st)) {
			sum+=character.sumPool(Modules.getPool(st,"Experience.VisitXP"));
		}
		if (Modules.get(null,"faction").isEnabled(st)) {
			sum+=character.sumPool(Modules.getPool(st,"Faction.FactionXP"));
		}
		if (Modules.get(null,"Events").isEnabled(st)) {
			sum+=character.sumPool(Modules.getPool(st,"Events.EventXP"));
		}
		for (final Attribute a: st.getAttributes()) {
			if (a.getType()==EXPERIENCE) {
				sum+=character.sumPool(Modules.getPool(st,"Experience."+a.getName()+"XP"));
			}
		}
		return sum;
	}

	@Nonnull
	public static String getCycleLabel(@Nonnull final State st) {
		if (Modules.get(null,"Experience").isEnabled(st)) {
			return Math.round(st.getKV("Experience.XPCycleDays").floatValue())+" days";
		}
		else { return "week"; }
	}

	public static int getCycle(@Nonnull final State st) {
		if (Modules.get(null,"Experience").isEnabled(st)) {
			return (int) (60*60*24*st.getKV("Experience.XPCycleDays").floatValue());
		}
		else {
			return 60*60*24*7;
		}
	}

}
