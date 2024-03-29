package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScriptingModule extends ModuleAnnotation {
	public ScriptingModule(final String name,final ModuleDefinition annotation) {
		super(name,annotation);
	}
	
	@Nonnull
	@Override
	public Map<String,Command> getCommands(@Nonnull final State st) {
		final Map<String,Command> commands=new HashMap<>();
		final Set<Script> scripts=Script.getScripts(st);
		for (final Script script: scripts) {
			try {
				commands.put(script.getName(),new ScriptingCommand(script));
			} catch (final NoDataException ignored) {
			}
		}
		return commands;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public Command getCommandNullable(@Nonnull final State st,@Nonnull final String commandname) {
		if ("characterresponse".equalsIgnoreCase(commandname)||"stringresponse".equalsIgnoreCase(commandname)) {
			return super.getCommandNullable(st,commandname);
		}
		final Script script=Script.findNullable(st,commandname.replaceFirst("gs",""));
		if (script==null) {
			throw new UserInputLookupFailureException("No script named "+commandname+" exists",true);
		}
		return new ScriptingCommand(script);
	}
	
}
