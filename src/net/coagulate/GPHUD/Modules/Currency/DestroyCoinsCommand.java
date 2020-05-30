package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DestroyCoinsCommand extends Command {
	String name;
	public DestroyCoinsCommand(String name) {this.name=name;}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String description() {
		return "Destroys an ammount of "+name+" for a character";
	}

	@Override
	public String requiresPermission() {
		return "Currency.Destroy"+name;
	}

	@Override
	public Context context() {
		return Context.AVATAR;
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
		return true;
	}

	@Override
	public boolean permitScripting() {
		return true;
	}

	@Override
	public boolean permitExternal() {
		return true;
	}

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		ArrayList<Argument> args=new ArrayList<>();
		args.add(new Argument() {
			@Override
			public boolean isGenerated() { return true; }
			@Nonnull @Override
			public ArgumentType type() { return ArgumentType.CHARACTER; }
			@Nonnull @Override
			public String description() { return "Character to remove "+name+" from"; }
			@Override
			public boolean mandatory() { return true; }
			@Override
			public Class<? extends Object> objectType() { return Char.class; }
			@Override
			public String getName() { return "target"; }
			@Override
			public boolean delayTemplating() { return false;}
			@Override
			public int max() { return 0; }
			@Override
			public void overrideDescription(String n) {}
			@Nonnull @Override
			public List<String> getChoices(State st) { return null; }
		});
		args.add(new Argument() {
			@Override
			public boolean isGenerated() { return true; }
			@Nonnull @Override
			public ArgumentType type() { return ArgumentType.INTEGER; }
			@Nonnull @Override
			public String description() { return "Ammount of "+name+" to remove"; }
			@Override
			public boolean mandatory() { return true; }
			@Override
			public Class<? extends Object> objectType() { return Integer.class; }
			@Override
			public String getName() { return "ammount"; }
			@Override
			public boolean delayTemplating() { return false;}
			@Override
			public int max() { return 0; }
			@Override
			public void overrideDescription(String n) {}
			@Nonnull @Override
			public List<String> getChoices(State st) { return null; }
		});
		args.add(new Argument() {
			@Override
			public boolean isGenerated() { return true; }
			@Nonnull @Override
			public ArgumentType type() { return ArgumentType.TEXT_ONELINE; }
			@Nonnull @Override
			public String description() { return "Reason for currency destruction"; }
			@Override
			public boolean mandatory() { return true; }
			@Override
			public Class<? extends Object> objectType() { return String.class; }
			@Override
			public String getName() { return "reason"; }
			@Override
			public boolean delayTemplating() { return false;}
			@Override
			public int max() { return 255; }
			@Override
			public void overrideDescription(String n) {}
			@Nonnull @Override
			public List<String> getChoices(State st) { return null; }
		});
		return args;
	}

// ---------- INSTANCE ----------
@Nonnull
@Override
public String getFullName() {
	return "Currency.Destroy"+name;
}

	@Nonnull
	@Override
	public String getName() {
		return "Destroy"+name;
	}

	@Override
	protected Response execute(State state,
	                           Map<String,Object> arguments) {
		Char target=(Char)arguments.get("target");
		Integer ammount=(Integer)arguments.get("ammount");
		if (ammount<1) { return new ErrorResponse("Ammount must be a positive integer"); }
		String reason=(String)arguments.get("reason");
		Currency.find(state,name).spawnInAsAdmin(state,target,-ammount,reason);
		return new OKResponse("Destroyed "+ammount+" "+name+" from "+target.getName());
	}
}
