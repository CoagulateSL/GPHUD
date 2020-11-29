package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
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

public class TransferCoinsCommand extends Command {
	private final String name;
	private final boolean senderpaystax;

	public TransferCoinsCommand(final String name,
	                            final boolean senderpaystax) {
		this.name=name;
		this.senderpaystax=senderpaystax;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String description() {
		return "Transfer currency to another player "+(senderpaystax?"(You will pay any incurred taxes)":"(The recipient will receive the ammount less any incurred taxes)");
	}

	@Nonnull
	@Override
	public String notes() { return ""; }

	@Override
	public String requiresPermission() {
		return "";
	}

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
		final ArrayList<Argument> args=new ArrayList<>();
		args.add(new Argument() {
			// ---------- INSTANCE ----------
			@Override
			public boolean isGenerated() { return true; }

			@Nonnull
			@Override
			public ArgumentType type() { return ArgumentType.CHARACTER; }

			@Nonnull
			@Override
			public String description() { return "Character to transfer "+name+" to"; }

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

			@Override
			public void overrideDescription(final String n) {}

			@Nonnull
			@Override
			public List<String> getChoices(final State st) { throw new SystemImplementationException("This should not be being used"); }
		});
		args.add(new Argument() {
			// ---------- INSTANCE ----------
			@Override
			public boolean isGenerated() { return true; }

			@Nonnull
			@Override
			public ArgumentType type() { return ArgumentType.TEXT_ONELINE; }

			@Nonnull
			@Override
			public String description() { return "Ammount of "+name+" to transfer"; }

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
			// ---------- INSTANCE ----------
			@Override
			public boolean isGenerated() { return true; }

			@Nonnull
			@Override
			public ArgumentType type() { return ArgumentType.TEXT_ONELINE; }

			@Nonnull
			@Override
			public String description() { return "Reason for transfer"; }

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
		return "Currency.Transfer"+(senderpaystax?"PayTax":"")+name;
	}

	@Nonnull
	@Override
	public String getName() {
		return "Transfer"+(senderpaystax?"PayTax":"")+name;
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
		if (!currency.tradable()) { return new ErrorResponse("Sorry, you are not allowed to transfer "+currency.getName()); }

		final State targetstate=new State(target);

		final float taxrate=targetstate.getKV("Currency.TransactionTax"+currency.getName()).floatValue()/100F;

		final int taxpayable=(int) (((float) ammount)*taxrate);
		final int remainder=ammount-taxpayable;

		int balancecheck=ammount;
		if (senderpaystax) { balancecheck+=taxpayable; }
		if (currency.sum(state)<balancecheck) {
			return new ErrorResponse("You do not have enough currency to complete this transaction; you need "+currency.shortTextForm(balancecheck));
		}
		// check we can send taxes somewhere, else abort before doing anything
		final String taxrecipient=targetstate.getKV("Currency.TransactionTaxRecipient"+currency.getName()).value();
		Char taxto=null;
		if (!taxrecipient.isEmpty()) {
			taxto=Char.findNullable(state.getInstance(),taxrecipient);
			if (taxto==null) { throw new UserConfigurationException("The tax recipient ("+taxrecipient+") does not exist"); }
		}

		// remove money from A, send money to B and to tax pot (!?)
		final int received;
		final int sent;
		if (senderpaystax) {
			currency.spawnInByChar(targetstate,state.getCharacter(),-(ammount+taxpayable),reason+(taxpayable==0?"":" (Tax:"+currency.shortTextForm(taxpayable)+")"));
			currency.spawnInByChar(state,target,ammount,reason);
			sent=ammount+taxpayable;
			received=ammount;
		}
		else {
			currency.spawnInByChar(targetstate,state.getCharacter(),-ammount,reason);
			currency.spawnInByChar(state,target,remainder,reason+(taxpayable==0?"":" (Tax:"+currency.shortTextForm(taxpayable)+")"));
			received=remainder;
			sent=ammount;
		}
		// does the tax go anywhere?
		if (taxto!=null) {
			if (senderpaystax) {
				currency.spawnInByChar(state,
				                       taxto,
				                       taxpayable,
				                       "Tax for transfer of "+currency.shortTextForm(ammount)+" "+currency.getName()+" to "+target.getName()+": "+reason);
			}
			else {
				currency.spawnInByChar(targetstate,
				                       taxto,
				                       taxpayable,
				                       "Tax for receipt of "+currency.shortTextForm(ammount)+" "+currency.getName()+" from "+state.getCharacter().getName()+": "+reason);
			}
		}
		// tell target
		if (target.isOnline()) {
			final JSONObject json=new JSONObject();
			JSONResponse.message(json,"You received "+currency.longTextForm(received)+" "+currency.getName()+" from "+state.getCharacter().getName()+" : "+reason,target.getProtocol());
			new Transmission(target,json).start();
		}
		return new OKResponse("Transferred "+currency.longTextForm(sent)+" of "+name+" to "+target.getName()+(taxpayable==0?"":" (Payed to tax: "+currency.shortTextForm(
				taxpayable)+")"));
	}
}
