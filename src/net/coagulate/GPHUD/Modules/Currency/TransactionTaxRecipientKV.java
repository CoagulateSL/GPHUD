package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class TransactionTaxRecipientKV extends KV {
	final Currency currency;
	final String   name;
	
	public TransactionTaxRecipientKV(@Nonnull final State st,@Nonnull final Currency currency) {
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
		return "Currency.TransactionTaxRecipient"+name;
	}
	
	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.COMPLETE;
	}
	
	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.TEXT;
	}
	
	@Nonnull
	@Override
	public String description() {
		return "Where the tax goes - nowhere if blank, otherwise a character name (WARNING: MUST EXIST or transactions will abort)";
	}
	
	@Nonnull
	@Override
	public String editPermission() {
		return "Currency.Configure";
	}
	
	@Override
	public String defaultValue() {
		return "";
	}
	
	@Nonnull
	@Override
	public String conveyAs() {
		return "";
	}
	
	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.DELEGATING;
	}
	
	@Override
	public boolean template() {
		return false;
	}
	
	@Nonnull
	@Override
	public String name() {
		return "TransactionTaxRecipient"+name;
	}
}
