package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.State;

import java.lang.annotation.*;
import java.util.List;

/**
 * Wraps an argument.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Argument {

	public abstract boolean isGenerated();

	public abstract ArgumentType type();

	public abstract String description();

	public abstract boolean mandatory();

	//public abstract String choiceMethod();
	public abstract Class objectType();

	public abstract String getName();

	public abstract boolean delayTemplating();

	public abstract int max();

	public abstract void overrideDescription(String n);

	public abstract List<String> getChoices(State st);

	public static enum ArgumentType {
		TEXT_ONELINE, TEXT_MULTILINE, PASSWORD, TEXT_CLEAN, TEXT_INTERNAL_NAME,
		BOOLEAN, INTEGER, FLOAT,
		CHOICE,
		CHARACTER, CHARACTER_PLAYABLE, CHARACTER_NEAR, CHARACTER_FACTION,
		AVATAR, AVATAR_NEAR,
		PERMISSIONSGROUP,
		PERMISSION,
		CHARACTERGROUP,
		KVLIST,
		MODULE,
		REGION, ZONE,
		COORDINATES,
		EVENT,
		ATTRIBUTE, ATTRIBUTE_WRITABLE
	}


	/**
	 * Defines metadata about a command's argument.
	 * Note you are not required to document the first "State" parameter that all methods assume.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PARAMETER)
	public @interface Arguments {
		ArgumentType type();

		String description();

		boolean mandatory() default true;

		String choiceMethod() default "";

		boolean delayTemplating() default false;

		int max() default -1;
	}
}
