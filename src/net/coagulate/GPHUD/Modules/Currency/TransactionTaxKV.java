package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class TransactionTaxKV extends KV {
	final Currency currency;
	final String name;

	public TransactionTaxKV(@Nonnull final State st,
	                        @Nonnull final Currency currency) {
		super();
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
	public String fullname() {
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
		return "The tax applied to trasactions at the reciving end";
	}

	@Nonnull
	@Override
	public String editpermission() {
		return "Currency.Configure";
	}

	@Override
	public String defaultvalue() {
		return "0";
	}

	@Nonnull
	@Override
	public String conveyas() {
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
