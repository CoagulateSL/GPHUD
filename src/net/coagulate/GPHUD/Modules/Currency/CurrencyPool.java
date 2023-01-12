package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Modules.Pool;

import javax.annotation.Nonnull;

public class CurrencyPool extends Pool {
	
	final String name;
	
	CurrencyPool(final String name) {
		this.name=name;
	}
	
	@Override
	public boolean isGenerated() {
		return true;
	}
	
	@Nonnull
	@Override
	public String description() {
		return "Holds currency transactions for "+name;
	}
	
	@Nonnull
	@Override
	public String fullName() {
		return "Currency."+name;
	}
	
	@Nonnull
	@Override
	public String name() {
		return name;
	}
}
