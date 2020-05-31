package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSResourceUnavailableException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSUnknownIdentifier;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class GSFunctions {
	private static final Map<String,Method> functionmap=new HashMap<>();

	// ---------- STATICS ----------
	public static Method get(final String functionname) {
		if (functionmap.containsKey(functionname)) { return functionmap.get(functionname); }
		throw new GSUnknownIdentifier("Function call "+functionname+" is not defined.");
	}

	public static void register(final String string,
	                            final Method method) {
		if (functionmap.containsKey(string)) {
			throw new SystemImplementationException("Duplicate definition for gsFunction "+string);
		}
		functionmap.put(string,method);
	}

	@Nonnull
	public static Map<String,Method> getAll() { return functionmap; }

	public static void assertModule(State st,
	                                String modulename) {
		if (!Modules.get(null,modulename).isEnabled(st)) {
			throw new GSResourceUnavailableException(modulename+" module is disabled, thus its function calls are disabled.");
		}
	}

	/**
	 * Defines an exposed command.
	 * That is, something the user can call through web, SL or other user interfaces.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	public @interface GSFunction {
		// ---------- INSTANCE ----------
		@Nonnull String description();

		@Nonnull String parameters();

		@Nonnull String returns();

		@Nonnull String notes();

		boolean privileged();
	}


}
