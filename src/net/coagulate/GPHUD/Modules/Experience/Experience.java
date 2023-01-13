package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterPool;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Templater;
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
	@Template(name="TOTALXP", description="Total experience")
	public static String getTotalXP(@Nonnull final State st,final String key) {
		if (!st.hasModule("Experience")) {
			return "";
		}
		if (st.getCharacterNullable()==null) {
			return "";
		}
		return String.valueOf(getExperience(st,st.getCharacter()));
	}
	
	public static int getExperience(@Nonnull final State st,@Nonnull final Char character) {
		int sum=0;
		if (Modules.get(null,"experience").isEnabled(st)) {
			sum+=CharacterPool.sumPool(character,Modules.getPool(st,"Experience.VisitXP"));
		}
		if (Modules.get(null,"faction").isEnabled(st)) {
			sum+=CharacterPool.sumPool(character,Modules.getPool(st,"Faction.FactionXP"));
		}
		if (Modules.get(null,"Events").isEnabled(st)) {
			sum+=CharacterPool.sumPool(character,Modules.getPool(st,"Events.EventXP"));
		}
		for (final Attribute a: st.getAttributes()) {
			if (a.getType()==EXPERIENCE) {
				sum+=CharacterPool.sumPool(character,Modules.getPool(st,"Experience."+a.getName()));
			}
		}
		return sum;
	}
	
	@Nonnull
	@Template(name="LEVEL", description="Current Level")
	public static String getLevel(@Nonnull final State st,final String key) {
		if (!st.hasModule("Experience")) {
			return "";
		}
		if (st.getCharacterNullable()==null) {
			return "";
		}
		return String.valueOf(toLevel(st,getExperience(st,st.getCharacter())));
	}
	
	public static int toLevel(@Nonnull final State st,final int xp) {
		if (!st.hasModule("Experience")) {
			return 0;
		}
		int maxLevel=st.getKV("Experience.MaxLevel").intValue();
		final int step=st.getKV("Experience.LevelXPStep").intValue();
		int tolevel=0;
		if (maxLevel==0) {
			maxLevel=1000;
		}
		for (int i=0;i<=maxLevel;i++) {
			tolevel=(int)(tolevel+Math.floor(((float)i)/step)+1);
			if (tolevel>xp) {
				return i;
			}
		}
		return maxLevel;
		
	}
	
	@Nonnull
	@Templater.Template(name="TARGET:LEVEL", description="TARGET's level")
	public static String getTargetLevel(@Nonnull final State st,final String key) {
		if (!st.hasModule("Roller")) {
			return "";
		}
		if (st.getTargetNullable()==null) {
			throw new UserInputStateException("No target!");
		}
		final State target=st.getTargetNullable();
		return String.valueOf(Experience.toLevel(target,Experience.getExperience(target,target.getCharacter())));
	}
	
	@Nonnull
	public static String getCycleLabel(@Nonnull final State st) {
		if (Modules.get(null,"Experience").isEnabled(st)) {
			return Math.round(st.getKV("Experience.XPCycleDays").floatValue())+" days";
		} else {
			return "week";
		}
	}
	
	public static int getCycle(@Nonnull final State st) {
		if (Modules.get(null,"Experience").isEnabled(st)) {
			return (int)(60*60*24*st.getKV("Experience.XPCycleDays").floatValue());
		} else {
			return 60*60*24*7;
		}
	}
	
}
