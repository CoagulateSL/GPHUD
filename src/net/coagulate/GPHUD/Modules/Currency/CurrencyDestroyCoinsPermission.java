package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class CurrencyDestroyCoinsPermission extends Permission {
	String name;
	public CurrencyDestroyCoinsPermission(String name) {this.name=name;}

	@Override
	public Module getModule(State st) {
		return Modules.get(st,"Currency");
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String name() {
		return "Destroy"+name;
	}

	@Nonnull
	@Override
	public String description() {
		return "Permission to destroy (remote from a character) "+name+" currency";
	}

	@Nonnull
	@Override
	public POWER power() {
		return POWER.LOW;
	}

	@Override
	public boolean grantable() {
		return true;
	}
}
