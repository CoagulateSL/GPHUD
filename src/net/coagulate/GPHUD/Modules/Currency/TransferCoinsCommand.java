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
	private final String  name;
	private final boolean senderPaysTax;
	
	public TransferCoinsCommand(final String name,final boolean senderPaysTax) {
		this.name=name;
		this.senderPaysTax=senderPaysTax;
	}
	
	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}
	
	@Override
	public String description() {
		return "Transfer currency to another player "+(senderPaysTax?"(You will pay any incurred taxes)":
		                                               "(The recipient will receive the ammount less any incurred taxes)");
	}
	
	@Nonnull
	@Override
	public String notes() {
		return "";
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
			public boolean isGenerated() {
				return true;
			}
			
			@Nonnull
			@Override
			public String name() {
				return "target";
			}
			
			@Nonnull
			@Override
			public ArgumentType type() {
				return ArgumentType.CHARACTER;
			}
			
			@Nonnull
			@Override
			public String description() {
				return "Character to transfer "+name+" to";
			}
			
			@Override
			public boolean mandatory() {
				return true;
			}
			
			@Override
			public Class<?> objectType() {
				return Char.class;
			}
			
			@Override
			public boolean delayTemplating() {
				return false;
			}
			
			@Override
			public int max() {
				return 0;
			}
			
			@Override
			public void overrideDescription(final String n) {
			}
			
			@Nonnull
			@Override
			public List<String> getChoices(final State st) {
				throw new SystemImplementationException("This should not be being used");
			}
		});
		args.add(new Argument() {
			// ---------- INSTANCE ----------
			@Override
			public boolean isGenerated() {
				return true;
			}
			
			@Nonnull
			@Override
			public String name() {
				return "ammount";
			}
			
			@Nonnull
			@Override
			public ArgumentType type() {
				return ArgumentType.TEXT_ONELINE;
			}
			
			@Nonnull
			@Override
			public String description() {
				return "Ammount of "+name+" to transfer";
			}
			
			@Override
			public boolean mandatory() {
				return true;
			}
			
			@Override
			public Class<?> objectType() {
				return String.class;
			}
			
			@Override
			public boolean delayTemplating() {
				return false;
			}
			
			@Override
			public int max() {
				return 128;
			}
			
			@Override
			public void overrideDescription(final String n) {
			}
			
			@Nonnull
			@Override
			public List<String> getChoices(final State st) {
				throw new SystemImplementationException("This should not be being used");
			}
		});
		args.add(new Argument() {
			// ---------- INSTANCE ----------
			@Override
			public boolean isGenerated() {
				return true;
			}
			
			@Nonnull
			@Override
			public String name() {
				return "reason";
			}
			
			@Nonnull
			@Override
			public ArgumentType type() {
				return ArgumentType.TEXT_ONELINE;
			}
			
			@Nonnull
			@Override
			public String description() {
				return "Reason for transfer";
			}
			
			@Override
			public boolean mandatory() {
				return true;
			}
			
			@Override
			public Class<?> objectType() {
				return String.class;
			}
			
			@Override
			public boolean delayTemplating() {
				return false;
			}
			
			@Override
			public int max() {
				return 255;
			}
			
			@Override
			public void overrideDescription(final String n) {
			}
			
			@Nonnull
			@Override
			public List<String> getChoices(final State st) {
				throw new SystemImplementationException("This should not be being used");
			}
		});
		return args;
	}
	
	@Nonnull
	@Override
	public String getFullName() {
		return "Currency.Transfer"+(senderPaysTax?"PayTax":"")+name;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return "Transfer"+(senderPaysTax?"PayTax":"")+name;
	}
	
	// ----- Internal Instance -----
	@Override
	protected Response execute(final State state,final Map<String,Object> arguments) {
		final Char target=(Char)arguments.get("target");
		final Currency currency=Currency.find(state,name);
		final int amount=currency.decode((String)arguments.get("ammount"));
		if (amount<1) {
			return new ErrorResponse("Ammount must be a positive integer");
		}
		final String reason=(String)arguments.get("reason");
		if (!currency.tradable()) {
			return new ErrorResponse("Sorry, you are not allowed to transfer "+currency.getName());
		}
		
		final State targetState=new State(target);
		
		final float taxRate=targetState.getKV("Currency.TransactionTax"+currency.getName()).floatValue()/100.0F;
		
		final int taxPayable=(int)(amount*taxRate);
		final int remainder=amount-taxPayable;
		
		int balanceCheck=amount;
		if (senderPaysTax) {
			balanceCheck+=taxPayable;
		}
		if (currency.sum(state)<balanceCheck) {
			return new ErrorResponse("You do not have enough currency to complete this transaction; you need "+
			                         currency.shortTextForm(balanceCheck));
		}
		// check we can send taxes somewhere, else abort before doing anything
		final String taxRecipient=targetState.getKV("Currency.TransactionTaxRecipient"+currency.getName()).value();
		Char taxTo=null;
		if (!taxRecipient.isEmpty()) {
			taxTo=Char.findNullable(state.getInstance(),taxRecipient);
			if (taxTo==null) {
				throw new UserConfigurationException("The tax recipient ("+taxRecipient+") does not exist",true);
			}
		}
		
		// remove money from A, send money to B and to tax pot (!?)
		final int received;
		final int sent;
		if (senderPaysTax) {
			currency.spawnInByChar(targetState,
			                       state.getCharacter(),
			                       -(amount+taxPayable),
			                       reason+(taxPayable==0?"":" (Tax:"+currency.shortTextForm(taxPayable)+")"));
			currency.spawnInByChar(state,target,amount,reason);
			sent=amount+taxPayable;
			received=amount;
		} else {
			currency.spawnInByChar(targetState,state.getCharacter(),-amount,reason);
			currency.spawnInByChar(state,
			                       target,
			                       remainder,
			                       reason+(taxPayable==0?"":" (Tax:"+currency.shortTextForm(taxPayable)+")"));
			received=remainder;
			sent=amount;
		}
		// does the tax go anywhere?
		if (taxTo!=null) {
			if (senderPaysTax) {
				currency.spawnInByChar(state,
				                       taxTo,
				                       taxPayable,
				                       "Tax for transfer of "+currency.shortTextForm(amount)+" "+currency.getName()+
				                       " to "+target.getName()+": "+reason);
			} else {
				currency.spawnInByChar(targetState,
				                       taxTo,
				                       taxPayable,
				                       "Tax for receipt of "+currency.shortTextForm(amount)+" "+currency.getName()+
				                       " from "+state.getCharacter().getName()+": "+reason);
			}
		}
		// tell target
		if (target.isOnline()) {
			final JSONObject json=new JSONObject();
			JSONResponse.message(json,
			                     "You received "+currency.longTextForm(received)+" "+currency.getName()+" from "+
			                     state.getCharacter().getName()+" : "+reason,
			                     target.getProtocol());
			new Transmission(target,json).start();
		}
		return new OKResponse("Transferred "+currency.longTextForm(sent)+" of "+name+" to "+target.getName()+
		                      (taxPayable==0?"":" (Payed to tax: "+currency.shortTextForm(taxPayable)+")"));
	}
}
