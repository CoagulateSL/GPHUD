package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.annotation.*;
import java.util.List;

/**
 * Wraps an argument.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Argument {

	// ---------- INSTANCE ----------
	public abstract boolean isGenerated();

	@Nonnull
	public abstract String name();

	@Nonnull
	public abstract ArgumentType type();

	@Nonnull
	public abstract String description();

	public abstract boolean mandatory();

	//public abstract String choiceMethod();
	public abstract Class<? extends Object> objectType();

	public abstract boolean delayTemplating();

	public abstract int max();

	public abstract void overrideDescription(String n);

	@Nonnull
	public abstract List<String> getChoices(State st);

	public enum ArgumentType {
		TEXT_ONELINE,
		TEXT_MULTILINE,
		PASSWORD,
		TEXT_CLEAN,
		TEXT_INTERNAL_NAME,
		BOOLEAN,
		INTEGER,
		FLOAT,
		CHOICE,
		CHARACTER,
		CHARACTER_PLAYABLE,
		CHARACTER_NEAR,
		CHARACTER_FACTION,
		AVATAR,
		AVATAR_NEAR,
		PERMISSIONSGROUP,
		PERMISSION,
		CHARACTERGROUP,
		KVLIST,
		MODULE,
		REGION,
		ZONE,
		COORDINATES,
		EVENT,
		EFFECT,
		ATTRIBUTE,
		ATTRIBUTE_WRITABLE,
		SET,
		INVENTORY,
		ITEM,
		CURRENCY
	}


	/**
	 * Defines metadata about a command's argument.
	 * Note you are not required to document the first "State" parameter that all methods assume.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PARAMETER)
	public @interface Arguments {
		// ---------- INSTANCE ----------
		@Nonnull String name();

		@Nonnull ArgumentType type();

		@Nonnull String description();

		boolean mandatory() default true;

		@Nonnull String choiceMethod() default "";

		boolean delayTemplating() default false;

		int max() default -1;
	}
}
