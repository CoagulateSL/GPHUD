package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DestroyCoinsCommand extends Command {
	final String name;

	public DestroyCoinsCommand(final String name) {this.name=name;}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String description() {
		return "Destroys an ammount of "+name+" for a character";
	}

	// ---------- INSTANCE ----------

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
	public String notes() { return ""; }

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		final ArrayList<Argument> args=new ArrayList<>();
		args.add(new Argument() {
			@Override
			public boolean isGenerated() { return true; }

			@Nonnull
			@Override
		public ArgumentType type() { return ArgumentType.CHARACTER; }

		@Nonnull
		@Override
		public String description() { return "Character to remove "+name+" from"; }

		@Override
		public boolean mandatory() { return true; }

		@Override
		public Class<? extends Object> objectType() { return Char.class; }

		@Override
		public String name() { return "target"; }

		@Override
		public boolean delayTemplating() { return false;}

		@Override
		public int max() { return 0; }

		// ---------- INSTANCE ----------
		@Override
		public void overrideDescription(final String n) {}

		@Nonnull
		@Override
		public List<String> getChoices(final State st) { throw new SystemImplementationException("This should not be being used"); }
	});
	args.add(new Argument() {
		@Override
		public boolean isGenerated() { return true; }

		// ---------- INSTANCE ----------
		@Nonnull
		@Override
		public ArgumentType type() { return ArgumentType.TEXT_ONELINE; }

		@Nonnull
		@Override
		public String description() { return "Ammount of "+name+" to remove"; }

		@Override
		public boolean mandatory() { return true; }

		@Override
		public Class<? extends Object> objectType() { return String.class; }

		@Override
		public String name() { return "ammount"; }

		@Override
		public boolean delayTemplating() { return false;}

		@Override
		public int max() { return 128; }

		@Override
		public void overrideDescription(final String n) {}

		@Nonnull
		@Override
		public List<String> getChoices(final State st) { throw new SystemImplementationException("This should not be being used"); }
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
		public String name() { return "reason"; }

		@Override
		public boolean delayTemplating() { return false;}

		@Override
		public int max() { return 255; }

		// ---------- INSTANCE ----------
		@Override
		public void overrideDescription(final String n) {}

		@Nonnull
		@Override
		public List<String> getChoices(final State st) { throw new SystemImplementationException("This should not be being used"); }
	});
	return args;
}
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

	// ----- Internal Instance -----
	@Override
	protected Response execute(final State state,
	                           final Map<String,Object> arguments) {
		final Char target=(Char) arguments.get("target");
		final Currency currency=Currency.find(state,name);
		final int ammount=currency.decode((String) arguments.get("ammount"));
		if (ammount<1) { return new ErrorResponse("Ammount must be a positive integer"); }
		final String reason=(String) arguments.get("reason");
		currency.spawnInAsAdmin(state,target,-ammount,reason);
		if (target.isOnline()) {
			final JSONObject json=new JSONObject();
			json.put("message","[Admin:"+state.getAvatar().getName()+"] You lost "+currency.longTextForm(ammount)+" of "+currency.getName()+" : "+reason);
			new Transmission(target,json).start();
		}
		return new OKResponse("Destroyed "+currency.longTextForm(ammount)+" of "+name+" from "+target.getName());
	}
}
