package net.coagulate.GPHUD.Modules;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * Wraps a side menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class SideMenu {

	public abstract boolean isGenerated();

	@Nonnull
	public abstract String name();

	public abstract int priority();

	@Nonnull
	public abstract String url();

	@Nonnull
	public abstract String requiresPermission();

	/**
	 * Defines a sidemenu section for this module.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.PACKAGE)
	public @interface SideMenus {
		@Nonnull String name();

		int priority();

		@Nonnull String url();

		@Nonnull String requiresPermission() default "";
	}
}
