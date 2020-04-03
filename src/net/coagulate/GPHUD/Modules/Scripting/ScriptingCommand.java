package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ScriptingCommand extends Command {
	final Script script;

	public ScriptingCommand(final Script script) { this.script=script; }

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public Method getMethod() {
		try { return getClass().getMethod("execute",State.class); }
		catch (@Nonnull final NoSuchMethodException e) {
			throw new SystemImplementationException("Reflection exception finding gsScriptCommand's execute() method",e);
		}
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String description() {
		return "Run script "+script.getName();
	}

	@Nonnull
	@Override
	public String requiresPermission() {
		return "";
	}

	@Nonnull
	@Override
	public Context context() {
		return Context.CHARACTER;
	}

	@Override
	public boolean permitHUD() {
		return true;
	}

	@Override
	public boolean permitObject() { return true; }

	@Override
	public boolean permitConsole() {
		return true;
	}

	@Override
	public boolean permitWeb() {
		return false;
	}

	@Override
	public boolean permitScripting() {
		return false;
	}

	@Override
	public boolean permitExternal() { return false; }

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		return new ArrayList<>();
	}

	@Override
	public int getArgumentCount() {
		return 0;
	}

	@Nonnull
	@Override
	public String getFullName() {
		return "Scripting."+script.getName();
	}

	@Nonnull
	@Override
	public String getName() {
		return script.getName();
	}

	@Nonnull
	public Response execute(@Nonnull final State st) {
		final GSVM vm=new GSVM(script.getByteCode());
		//System.out.println("Script about to execute "+script.getNameSafe());
		return vm.execute(st);
	}
}
