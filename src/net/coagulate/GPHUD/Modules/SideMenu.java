package net.coagulate.GPHUD.Modules;

import java.lang.annotation.*;

/**
 * Wraps a side menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class SideMenu {

	public abstract boolean isGenerated();

	public abstract String name();

	public abstract int priority();

	public abstract String url();

	public abstract String requiresPermission();

	/**
	 * Defines a sidemenu section for this module.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface SideMenus {
		String name();

		int priority();

		String url();

		String requiresPermission() default "";
	}
}
