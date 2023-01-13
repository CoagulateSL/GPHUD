package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class CurrencyStaticCommands {
	
	// ---------- STATICS ----------
	@Commands(description="Shows your current total balance", context=Context.CHARACTER)
	public static Response balance(@Nonnull final State state) {
		final StringBuilder report=new StringBuilder("Current balance:");
		for (final Attribute a: Attribute.getAttributes(state.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				if (!report.isEmpty()) {
					report.append("\n");
				}
				report.append(a.getName()).append(" - ");
				final Currency currency=Currency.find(state,a.getName());
				report.append(currency.longSum(state));
			}
		}
		return new OKResponse(report.toString());
	}
}
