package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSUnknownIdentifier;

import javax.annotation.Nonnull;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class GSFunctions {
	public static Method get(final String functionname) {
		if (gsfunctions.containsKey(functionname)) { return gsfunctions.get(functionname); }
		throw new GSUnknownIdentifier("Function call "+functionname+" is not defined.");
	}

	/**
	 * Defines an exposed command.
	 * That is, something the user can call through web, SL or other user interfaces.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface GSFunction {
		@Nonnull String description();
		@Nonnull String parameters();
		@Nonnull String returns();
		@Nonnull String notes();
	}

	private static final Map<String, Method> gsfunctions=new HashMap<>();
	public static void register(final String string, final Method method) {
		if (gsfunctions.containsKey(string)) { throw new SystemImplementationException("Duplicate definition for gsFunction "+string); }
		gsfunctions.put(string,method);
	}
	@Nonnull
	public static Map<String, Method> getAll() { return gsfunctions; }
}
