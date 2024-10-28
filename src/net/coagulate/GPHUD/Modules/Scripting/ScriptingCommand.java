package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScriptingCommand extends Command {
	final Script script;
	
	public ScriptingCommand(final Script script) {
		this.script=script;
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
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String notes() {
		return "";
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
	public boolean permitObject() {
		return true;
	}
	
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
	
	// ----- Internal Instance -----
	@Override
	protected Response execute(final State state,final Map<String,Object> arguments) {
		final GSVM vm=GSVM.create(script);
		//System.out.println("Script about to execute "+script.getNameSafe());
		return vm.execute(state);
	}
	
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
	
	@Override
	public boolean permitExternal() {
		return false;
	}
}
