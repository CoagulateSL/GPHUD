package net.coagulate.GPHUD.Modules;

import java.lang.annotation.*;

/**
 * Wraps a permission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Permission {


	public abstract boolean isGenerated();

	public abstract String name();

	public abstract String description();

	public abstract boolean grantable();

	/**
	 * Defines a module permission, declare these (repeatedly) on your module's constructor.
	 *
	 * @param name        Name of the permission within your modules namespace (abc becomes module.abc)
	 * @param description Description of the permission
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	@Repeatable(Permissionss.class)
	public @interface Permissions {
		String name();

		String description();

		boolean grantable() default true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface Permissionss {
		Permissions[] value();
	}
}
