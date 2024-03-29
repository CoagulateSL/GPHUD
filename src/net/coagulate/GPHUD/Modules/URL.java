package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * Wraps a URL.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class URL {
	
	
	// ---------- INSTANCE ----------
	public abstract boolean isGenerated();
	
	public abstract String url();
	
	@Nonnull
	public abstract String requiresPermission();
	
	public abstract boolean requiresAuthentication();
	
	public abstract String getFullName();
	
	public abstract String getName();
	
	@Nonnull
	public abstract String getMethodName();
	
	public abstract void run(State st,SafeMap values);
	
	public abstract Module getModule();
	
	/**
	 * Defines an exposed command.
	 * That is, something the user can call through web, SL or other user interfaces.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface URLs {
		// ---------- INSTANCE ----------
		@Nonnull String url();
		
		@Nonnull String requiresPermission() default "";
		
		boolean requiresAuthentication() default true;
	}
}
