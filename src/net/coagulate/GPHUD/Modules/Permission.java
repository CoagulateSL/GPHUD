package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * Wraps a permission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Permission {
	
	public static final String RED   ="#ffdfdf";
	public static final String YELLOW="#ffffdf";
	public static final String GREEN ="#dfffdf";
	
	// ---------- INSTANCE ----------
	public abstract Module getModule(State st);
	
	@Nonnull
	public String getColor() {
		if (power()==POWER.HIGH) {
			return RED;
		}
		if (power()==POWER.MEDIUM) {
			return YELLOW;
		}
		if (power()==POWER.LOW) {
			return GREEN;
		}
		return "#dfdfdf";
	}
	
	public abstract boolean isGenerated();
	
	/** the non prefixed name */
	@Nonnull
	public abstract String name();
	
	@Nonnull
	public abstract String description();
	
	@Nonnull
	public abstract POWER power();
	
	public abstract boolean grantable();
	
	/*
		LOW - stuff that doesn't break the instance and that the admins can probably reverse (time consumingly)
		MEDIUM - stuff that might break the instance, but can be repaired, or partially restored
		HIGH - stuff that destroys things, likely to cause rollbacks if misused
		 */
	public enum POWER {
		LOW,MEDIUM,HIGH,UNKNOWN
	}
	
	/**
	 * Defines a module permission, declare these (repeatedly) on your module's constructor.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	@Repeatable(Permissionss.class)
	public @interface Permissions {
		// ---------- INSTANCE ----------
		@Nonnull String name();
		
		@Nonnull String description();
		
		@Nonnull POWER power();
		
		boolean grantable() default true;
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface Permissionss {
		// ---------- INSTANCE ----------
		@Nonnull Permissions[] value();
	}
}
