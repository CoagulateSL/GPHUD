package net.coagulate.GPHUD.Modules;

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
