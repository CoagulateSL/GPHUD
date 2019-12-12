package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Scripts;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScriptingModule extends ModuleAnnotation {
	public ScriptingModule(String name, ModuleDefinition annotation) {
		super(name, annotation);
	}

	@Override
	public Command getCommand(State st, String commandname) {
		if (commandname.equalsIgnoreCase("characterresponse") ||
			commandname.equalsIgnoreCase("stringresponse")) { return super.getCommand(st,commandname); }
		Scripts script=Scripts.findOrNull(st,commandname.replaceFirst("gs",""));
		if (script==null) { throw new UserException("No script named "+commandname+" exists"); }
		return new ScriptingCommand(script);
	}


	@Override
	public Map<String, Command> getCommands(State st) {
		Map<String,Command> commands=new HashMap<>();
		Set<Scripts> scripts=st.getInstance().getScripts();
		for (Scripts script:scripts) {
			commands.put(script.getName(),new ScriptingCommand(script));
		}
		return commands;
	}

}
