package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;
import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.POOL;

/**
 * Toolkit for Quotaed XP
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class QuotaedXP extends CharacterAttribute {
	protected QuotaedXP(int id) { super(id); }

	public abstract Module getModule();

	@Nonnull
	public String getFullName() { return getModule().getName() + "." + getName(); }

	/**
	 * Name of the quotaed pool
	 */
	@Nullable
	public String poolName(State st) { return getName(); }

	@Nonnull
	public Pool getPool(State st) { return Modules.getPoolNotNull(st, poolName(st)); }

	/**
	 * Name of the KV that controls the quota per period
	 */
	@Nonnull
	public String quotaKV(State st) { throw new SystemException("Override this method!"); }

	@Nonnull
	public Integer quota(@Nonnull State st) { return st.getKV(quotaKV(st)).intValue(); }

	/**
	 * Name of the KV that controls the period (in days)
	 */
	@Nonnull
	public String periodKV(State st) { throw new SystemException("Override this method!"); }

	@Nonnull
	public Float period(@Nonnull State st) { return st.getKV(periodKV(st)).floatValue(); }

	/**
	 * Return the period, in seconds
	 */
	public int periodSeconds(@Nonnull State st) {
		float days = period(st);
		float seconds = days * 24 * 60 * 60;
		return (int) seconds;
	}

	/**
	 * Text explanation of when the next point of XP will be available, sometimes "NOW!"
	 */
	@Nonnull
	public String nextFree(@Nonnull State st) {
		return st.getCharacter().poolNextFree(getPool(st), quota(st), period(st));
	}

	/**
	 * Calculate the starting unix time to covered by the period
	 */
	public int periodStart(@Nonnull State st) { return getUnixTime() - (periodSeconds(st)); }

	/**
	 * Calculate the ammount awarded in the period
	 */
	public int periodAwarded(@Nonnull State st) {
		return st.getCharacter().sumPoolSince(getPool(st), periodStart(st));
	}

	/**
	 * Cap an award based on ammount already earned
	 */
	public int capAward(@Nonnull State st, int award) {
		int awarded = periodAwarded(st);
		int cap = quota(st);
		if ((awarded + award) < cap) { return award; } // still under the cap
		int capped = cap - awarded;
		if (capped <= 0) { return 0; } // hmm
		return Math.min(capped, award);
	}

	public int cappedSystemAward(@Nonnull State st, int award, String description) {
		int cappedaward = capAward(st, award);
		st.getCharacter().addPoolSystem(st, getPool(st), cappedaward, description);
		return cappedaward;
	}


	@Nonnull
	public String periodRoughly(@Nonnull State st) {
		int seconds = periodSeconds(st);
		float days = (float) seconds;
		days = days / (60 * 60 * 24);
		return Math.round(days) + " days";
	}


	@Nonnull
	@Override
	public String getName() {
		throw new SystemException("Override this method!");
	}

	@Nonnull
	@Override
	public ATTRIBUTETYPE getType() {
		return POOL;
	}

	@Nonnull
	@Override
	public String getSubType() {
		return "Experience";
	}

	@Override
	public boolean usesAbilityPoints() {
		return false;
	}

	@Override
	public boolean getRequired() {
		return false;
	}

	@Nonnull
	@Override
	public String getDefaultValue() {
		return "";
	}

	@Override
	public boolean getSelfModify() {
		return false;
	}

	@Override
	public boolean isKV() {
		return false;
	}

	@Nonnull
	@Override
	public KV.KVTYPE getKVType() {
		throw new SystemException("Is not a KV");
	}

	@Nonnull
	@Override
	public String getKVDefaultValue() {
		throw new SystemException("Is not a KV");
	}

	@Nonnull
	@Override
	public KV.KVHIERARCHY getKVHierarchy() {
		throw new SystemException("Is not a KV");
	}


}
