package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class TransactionTaxKV extends KV {
	final Currency currency;
	final String   name;
	
	public TransactionTaxKV(@Nonnull final State st,@Nonnull final Currency currency) {
		this.currency=currency;
		name=currency.getName();
	}
	
	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}
	
	@Nonnull
	@Override
	public String fullName() {
		return "Currency.TransactionTax"+name;
	}
	
	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.COMPLETE;
	}
	
	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.FLOAT;
	}
	
	@Nonnull
	@Override
	public String description() {
		return "The tax applied to trasactions at the reciving end (as a percentage, without a % symbol)";
	}
	
	@Nonnull
	@Override
	public String editPermission() {
		return "Currency.Configure";
	}
	
	@Override
	public String defaultValue() {
		return "0";
	}
	
	@Nonnull
	@Override
	public String conveyAs() {
		return "";
	}
	
	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.CUMULATIVE;
	}
	
	@Override
	public boolean template() {
		return false;
	}
	
	@Nonnull
	@Override
	public String name() {
		return "TransactionTax"+name;
	}
}
