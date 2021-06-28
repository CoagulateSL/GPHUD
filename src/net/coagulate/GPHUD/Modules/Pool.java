package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterPool;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * Wraps a Pool.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Pool extends NameComparable {


	// ---------- INSTANCE ----------
	public abstract boolean isGenerated();

	@Nonnull
	public abstract String description();

	@Nonnull
	public abstract String fullName();

	public void delete(final State st) {
		CharacterPool.delete(fullName(),st.getInstance());
	}

	public int entries(final State st,
	                   final Char ch) {
		if (ch.getInstance()!=st.getInstance()) { throw new SystemConsistencyException("Pool entries character instance/state instance mismatch"); }
		return CharacterPool.poolEntries(ch,this);
	}

	/**
	 * Add money logged from a character to a target.  Source is from the state.
	 *
	 * @param st          - State for the source of the currency (debit is NOT done)
	 * @param target      - Target character
	 * @param ammount     - Base ammount of the currency this pool represents
	 * @param description - Description for the pool log
	 */
	public void addChar(final State st,
	                    final Char target,
	                    final int ammount,
	                    final String description) {
		CharacterPool.addPool(st,target,this,ammount,description);
	}

	public void addAdmin(final State st,
	                     final Char target,
	                     final int ammount,
	                     final String description) {
		CharacterPool.addPoolAdmin(st,target,this,ammount,description);
	}

	public void addSystem(final State st,
	                      final Char target,
	                      final int ammount,
	                      final String description) {
		CharacterPool.addPoolSystem(st,target,this,ammount,description);
	}

	public int sum(final State st) {
		return CharacterPool.sumPool(st,this);
	}

	/**
	 * Defines a pool used by a character.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	@Repeatable(Poolss.class)
	public @interface Pools {
		// ---------- INSTANCE ----------
		@Nonnull String name();

		@Nonnull String description();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface Poolss {
		// ---------- INSTANCE ----------
		@Nonnull Pools[] value();
	}

}
