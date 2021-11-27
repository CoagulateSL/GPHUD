package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSResourceUnavailableException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSUnknownIdentifier;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

public class GSFunctions {
	private static final Map<String,Method> functionMap =new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	// ---------- STATICS ----------
	@Nonnull
	public static Method get(final String functionName) {
		if (functionMap.containsKey(functionName)) { return functionMap.get(functionName); }
		throw new GSUnknownIdentifier("Function call "+functionName+" is not defined.");
	}
	@Nullable
	public static Method getNullable(@Nonnull final String functionName) { return functionMap.get(functionName); }

	public static void register(final String string,
	                            final Method method) {
		if (functionMap.containsKey(string)) {
			throw new SystemImplementationException("Duplicate definition for gsFunction "+string);
		}
		functionMap.put(string,method);
	}

	@Nonnull
	public static Map<String,Method> getAll() { return functionMap; }

	public static void assertModule(final State st,
	                                final String moduleName) {
		if (!Modules.get(null,moduleName).isEnabled(st)) {
			throw new GSResourceUnavailableException(moduleName+" module is disabled, thus its function calls are disabled.");
		}
	}

	public enum SCRIPT_CATEGORY {
		API,
		AVATAR,
		CURRENCY,
		DATETIME,
		EFFECTS,
		GROUPS,
		INPUT,
		KV,
		OUTPUT,
		RANDOMNESS,
		ZONES,
		CHARACTER,
		SETS,
		INVENTORY,
		UTILITY
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

		@Nonnull SCRIPT_CATEGORY category();

		boolean privileged();
	}


}
