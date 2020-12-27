package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
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

public class CreateCoinsCommand extends Command {
	final String name;

	public CreateCoinsCommand(final String name) {this.name=name;}

	@Override
	public boolean isGenerated() {
		return true;
	}

	// ---------- INSTANCE ----------

	@Override
	public String description() {
		return "Creates an ammount of "+name+" for a character";
	}

	@Override
	public String requiresPermission() {
		return "Currency.Create"+name;
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
			public String name() { return "target"; }

			@Nonnull
			@Override
		public ArgumentType type() { return ArgumentType.CHARACTER; }

		@Nonnull
		@Override
		public String description() { return "Character to give "+name+" to"; }

		@Override
		public boolean mandatory() { return true; }

		@Override
		public Class<? extends Object> objectType() { return Char.class; }

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
		public String name() { return "ammount"; }

		@Nonnull
		@Override
		public ArgumentType type() { return ArgumentType.TEXT_ONELINE; }

		@Nonnull
		@Override
		public String description() { return "Ammount of "+name+" to give"; }

		@Override
		public boolean mandatory() { return true; }

		@Override
		public Class<? extends Object> objectType() { return String.class; }

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
		public String description() { return "Reason for currency creation"; }

		@Override
		public boolean mandatory() { return true; }

		@Override
		public Class<? extends Object> objectType() { return String.class; }

		@Nonnull
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
		return "Currency.Create"+name;
	}

	@Nonnull
	@Override
	public String getName() {
		return "Create"+name;
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
		currency.spawnInAsAdmin(state,target,ammount,reason);
		if (target.isOnline()) {
			final JSONObject json=new JSONObject();
			JSONResponse.message(json,"[Admin:"+state.getAvatar().getName()+"] You gained "+currency.longTextForm(ammount)+" of "+currency.getName()+" : "+reason, target.getProtocol());
			new Transmission(target,json).start();
		}
		return new OKResponse("Spawned in "+currency.longTextForm(ammount)+" of "+name+" for "+target.getName());
	}
}
