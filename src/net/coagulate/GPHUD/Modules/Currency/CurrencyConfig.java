package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Data.Currency.Coin;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;

public class CurrencyConfig {

	// ---------- STATICS ----------
	@URLs(url="/configuration/currency")
	public static void configPage(@Nonnull final State st,
	                              final SafeMap values) {
		final Form f=st.form();
		f.noForm();

		f.add(new TextHeader("Currency Configuration"));
		f.add(new Paragraph("Currencies are created and deleted in the Characters configuration page, add or remove CURRENCY type attributes"));
		f.add(new Paragraph("Select a currency to configure;"));
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				f.add(new Link(a.getName(),"./Currency/View/"+a.getId()));
			}
		}
	}

	@URLs(url="/configuration/currency/view/*")
	public static void configCurrency(@Nonnull final State st,
	                                  final SafeMap values) {
		final Form f=st.form();
		f.noForm();

		st.form().noForm();
		//System.out.println(st.uri);
		final String[] split=st.getDebasedURL().split("/");
		//System.out.println(split.length);
		if (split.length==5) {
			final String id=split[split.length-1];
			final Attribute a=Attribute.get(Integer.parseInt(id));
			if (a.getInstance()!=st.getInstance()) { throw new SystemConsistencyException("Attribute instance/state instance mismatch"); }
			if (a.getType()!=ATTRIBUTETYPE.CURRENCY) { throw new UserInputInvalidChoiceException("Attribute "+a.getName()+" is not a currency, it's "+a.getType()); }
			configCurrency(st,values,Currency.find(st,a.getName()));
			return;
		}
		throw new SystemImplementationException("Currency config URL parse failure.");
	}

	@URLs(url="/configuration/currency/removecoin",
	      requiresPermission="Currency.denominations")
	public static void removeCoinPage(@Nonnull final State st,
	                                  @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Currency.removeCoin",values);
	}

	@Commands(description="Delete a coin.  This does not remove anyone's balance, just changes how balances are displayed.",
	          requiresPermission="Currency.Denominations",
	          context=Context.AVATAR)
	public static Response removeCoin(@Nonnull final State state,
	                                  @Arguments(description="Currency to remove the base coin from",
	                                             type=ArgumentType.CURRENCY) @Nonnull final Currency currency,
	                                  @Arguments(description="Base value of the coin to delete",
	                                             type=ArgumentType.INTEGER) @Nonnull final Integer basevalue) {
		if (currency.getInstance()!=state.getInstance()) {
			throw new SystemConsistencyException("Currency argument instance / state instance mismatch");
		}
		currency.removeCoin(state,basevalue);
		return new OKResponse("Removed coin");
	}

	@URLs(url="/configuration/currency/addcoin",
	      requiresPermission="Currency.denominations")
	public static void addCoinPage(@Nonnull final State st,
	                               @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Currency.addCoin",values);
	}

	@Commands(description="Creates a coin.  This does not alter anyone's balance, just changes how balances are displayed.",
	          requiresPermission="Currency.Denominations",
	          context=Context.AVATAR)
	public static Response addCoin(@Nonnull final State state,
	                               @Arguments(description="Currency to add the coin to",
	                                          type=ArgumentType.CURRENCY) @Nonnull final Currency currency,
	                               @Arguments(description="Base value of the coin to add",
	                                          type=ArgumentType.INTEGER) @Nonnull final Integer basevalue,
	                               @Arguments(description="Short name (symbol) of the coin",
	                                          type=ArgumentType.TEXT_ONELINE,
	                                          max=16) @Nonnull final String coinshortname,
	                               @Arguments(description="Long name of the coin",
	                                          type=ArgumentType.TEXT_ONELINE,
	                                          max=32) @Nonnull final String coinname) {
		if (currency.getInstance()!=state.getInstance()) {
			throw new SystemConsistencyException("Currency argument instance / state instance mismatch");
		}

		currency.addCoin(state,basevalue,coinshortname,coinname);
		return new OKResponse("Removed coin");
	}

	@URLs(url="/Configuration/Currency/ChangeBaseCoinNames",
	      requiresPermission="Currency.Denominations")
	public static void changeBaseCoinNames(@Nonnull final State st,
	                                       @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Currency.SetBaseCoinNames",values);
	}

	@Commands(description="Set the long and short name of the currency's base coin",
	          requiresPermission="Currency.Denominations",
	          context=Context.AVATAR)
	public static Response setBaseCoinNames(@Nonnull final State state,
	                                        @Arguments(description="Currency to alter",
	                                                   type=ArgumentType.CURRENCY) @Nonnull final Currency currency,
	                                        @Arguments(description="Short name of the coin",
	                                                   type=ArgumentType.TEXT_ONELINE,
	                                                   max=16) @Nonnull final String basecoinshortname,
	                                        @Arguments(description="Long name of the coin",
	                                                   type=ArgumentType.TEXT_ONELINE,
	                                                   max=32) @Nonnull final String basecoinname) {
		if (currency.getInstance()!=state.getInstance()) {
			throw new SystemConsistencyException("Currency argument instance / state instance mismatch");
		}
		currency.setBaseCoinNames(state,basecoinshortname,basecoinname);
		return new OKResponse("Base coin names updated");
	}

	// ----- Internal Statics -----
	private static void configCurrency(@Nonnull final State st,
	                                   @Nonnull final SafeMap values,
	                                   @Nonnull final Currency currency) {
		final Form f=st.form();

		f.add(new TextHeader("Currency configuration for "+currency.getName()));
		f.add(new Paragraph("This currency has a base coin of "+currency.getBaseCoinName()+" ("+currency.getBaseCoinNameShort()+")"));
		f.add(new Form(st,
		               true,
		               "/GPHUD/Configuration/Currency/ChangeBaseCoinNames",
		               "Rename Base Coin",
		               "currency",
		               currency.getName(),
		               "basecoinshortname",
		               currency.getBaseCoinNameShort(),
		               "basecoinname",
		               currency.getBaseCoinName()
		));
		f.add(new TextSubHeader("Coinage"));
		final Table t=new Table();
		final List<Coin> coins=currency.getCoins();
		coins.sort(Comparator.comparingInt(o->o.value));
		for (final Coin coin: coins) {
			t.openRow();
			t.add(new Cell(coin.value+"").align("right"));
			t.add(currency.getBaseCoinName());
			t.add("=");
			t.add("1 "+coin.basecoinname+" ("+coin.basecoinnameshort+")");
			t.add(new Form(st,true,"/GPHUD/Configuration/Currency/RemoveCoin","Delete Coin","currency",currency.getName(),"basevalue",coin.value+""));
		}
		f.add(t);
		f.add(new Form(st,true,"/GPHUD/Configuration/Currency/AddCoin","Add Coin","currency",currency.getName()));
		f.add(new Paragraph("<i>Note: Adding or removing coins does not change anyones balance, only how it is displayed</i>"));
	}

}
