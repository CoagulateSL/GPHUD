package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.State;

import java.lang.annotation.*;

/**
 * Wraps a permission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Permission {

	public abstract Module getModule(State st);
	public static final String RED="#ffdfdf";
	public static final String YELLOW="#ffffdf";
	public static final String GREEN="#dfffdf";
	public String getColor() {
		if (power()==POWER.HIGH) { return RED; }
		if (power()==POWER.MEDIUM) { return YELLOW; }
		if (power()==POWER.LOW) { return GREEN; }
		return "#dfdfdf";
	}

	/*
		LOW - stuff that doesn't break the instance and that the admins can probably reverse (time consumingly)
		MEDIUM - stuff that might break the instance, but can be repaired, or partially restored
		HIGH - stuff that destroys things, likely to cause rollbacks if misused
		 */
	public static enum POWER {LOW,MEDIUM,HIGH,UNKNOWN};

	public abstract boolean isGenerated();

	public abstract String name();

	public abstract String description();

	public abstract POWER power();

	public abstract boolean grantable();

	/**
	 * Defines a module permission, declare these (repeatedly) on your module's constructor.
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	@Repeatable(Permissionss.class)
	public @interface Permissions {
		String name();

		String description();

		POWER power();

		boolean grantable() default true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface Permissionss {
		Permissions[] value();
	}
}
