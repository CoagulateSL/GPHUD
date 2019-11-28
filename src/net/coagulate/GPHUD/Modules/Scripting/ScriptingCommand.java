package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Scripts;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ScriptingCommand extends Command {
	Scripts script;
	public ScriptingCommand(Scripts script) { this.script=script; }
	@Override
	public Method getMethod() {
		try { return this.getClass().getMethod("execute",State.class); }
		catch (NoSuchMethodException e) { throw new SystemException("Reflection exception finding gsScriptCommand's execute() method",e); }
	}

	public Response execute(State st) {
		GSVM vm=new GSVM(script.getByteCode());
		//System.out.println("Script about to execute "+script.getNameSafe());
		Response r=vm.execute(st);
		return r;
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String description() {
		return "Run script "+script.getName();
	}

	@Override
	public String requiresPermission() {
		return "";
	}

	@Override
	public Context context() {
		return Context.CHARACTER;
	}

	@Override
	public boolean permitJSON() {
		return true;
	}

	@Override
	public boolean permitConsole() {
		return true;
	}

	@Override
	public boolean permitScripting() {
		return false;
	}

	@Override
	public boolean permitObject() { return true; }

	@Override
	public boolean permitUserWeb() {
		return false;
	}

	@Override
	public List<Argument> getArguments() {
		 return new ArrayList<>();
	}

	@Override
	public int getArgumentCount() {
		return 0;
	}

	@Override
	public String getFullName() {
		return "Scripting."+script.getName();
	}

	@Override
	public String getName() {
		return script.getName();
	}
}
