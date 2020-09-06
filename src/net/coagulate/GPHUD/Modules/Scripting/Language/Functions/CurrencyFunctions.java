package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class CurrencyFunctions {

	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Gets the players current balance",
	                        parameters="BCCharacter - who to query<br>BCString - Currency name",
	                        returns="Integer - Number of basecoins the character has",
	                        notes="",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.CURRENCY)
	public static BCInteger gsGetBalance(@Nonnull final State st,
	                                     @Nonnull final GSVM vm,
	                                     @Nonnull final BCCharacter target,
	                                     @Nonnull final BCString currencyname) {
		GSFunctions.assertModule(st,"Currency");
		final Currency currency=Currency.find(st,currencyname.getContent());
		return new BCInteger(null,currency.sum(new State(target.getContent())));
	}

	@Nonnull
	@GSFunction(description="Converts a number of base coins into a short format text string",
	            parameters="BCString - Currency name<br>BCInteger - ammount of basecoins",
	            returns="BCString - Formatted representation of the currency",
	            notes="",
	            privileged=false,
	            category= SCRIPT_CATEGORY.CURRENCY)
	public static BCString gsFormatCoins(@Nonnull final State st,
	                                     @Nonnull final GSVM vm,
	                                     @Nonnull final BCString currencyname,
	                                     @Nonnull final BCInteger ammount) {
		GSFunctions.assertModule(st,"Currency");
		final Currency currency=Currency.find(st,currencyname.getContent());
		return new BCString(null,currency.shortTextForm(ammount.getContent()));
	}

	@Nonnull
	@GSFunction(description="Converts a number of base coins into a long format text string",
	            parameters="BCString - Currency name<br>BCInteger - ammount of basecoins",
	            returns="BCString - Formatted representation of the currency",
	            notes="",
	            privileged=false,
	            category= SCRIPT_CATEGORY.CURRENCY)
	public static BCString gsFormatCoinsLong(@Nonnull final State st,
	                                         @Nonnull final GSVM vm,
	                                         @Nonnull final BCString currencyname,
	                                         @Nonnull final BCInteger ammount) {
		GSFunctions.assertModule(st,"Currency");
		final Currency currency=Currency.find(st,currencyname.getContent());
		return new BCString(null,currency.longTextForm(ammount.getContent()));
	}

	@Nonnull
	@GSFunction(description="Converts a formatted currency string into a number of base coins",
	            parameters="BCString - Currency name<br>BCString - formatted currency",
	            returns="BCInteger - number of base coins this represented",
	            notes="",
	            privileged=false,
	            category= SCRIPT_CATEGORY.CURRENCY)
	public static BCInteger gsParseCoins(@Nonnull final State st,
	                                     @Nonnull final GSVM vm,
	                                     @Nonnull final BCString currencyname,
	                                     @Nonnull final BCString ammount) {
		GSFunctions.assertModule(st,"Currency");
		final Currency currency=Currency.find(st,currencyname.getContent());
		return new BCInteger(null,currency.decode(ammount.getContent()));
	}


}
