package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Scripts;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScriptingModule extends ModuleAnnotation {
	public ScriptingModule(final String name, final ModuleDefinition annotation) {
		super(name, annotation);
	}

	@Nullable
	@Override
	public Command getCommand(@Nonnull final State st, @Nonnull final String commandname) {
		if (commandname.equalsIgnoreCase("characterresponse") ||
			commandname.equalsIgnoreCase("stringresponse")) { return super.getCommand(st,commandname); }
		final Scripts script=Scripts.findOrNull(st,commandname.replaceFirst("gs",""));
		if (script==null) { throw new UserException("No script named "+commandname+" exists"); }
		return new ScriptingCommand(script);
	}


	@Nonnull
	@Override
	public Map<String, Command> getCommands(@Nonnull final State st) {
		final Map<String,Command> commands=new HashMap<>();
		final Set<Scripts> scripts=st.getInstance().getScripts();
		for (final Scripts script:scripts) {
			commands.put(script.getName(),new ScriptingCommand(script));
		}
		return commands;
	}

}
