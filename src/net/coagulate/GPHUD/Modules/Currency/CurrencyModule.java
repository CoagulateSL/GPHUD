package net.coagulate.GPHUD.Modules.Currency;


import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;

public class CurrencyModule extends ModuleAnnotation {

	public CurrencyModule(final String name,
	                      final ModuleDefinition def) {
		super(name,def);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public Map<String,KV> getKVDefinitions(final State st) {
		final Map<String,KV> definitions=super.getKVDefinitions(st);
		if (st==null) { return definitions; }
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				definitions.put("TransactionTax"+a.getName(),getKVDefinition(st,"TransactionTax"+a.getName()));
			}
		}
		return definitions;
	}

	@Override
	public KV getKVDefinition(final State st,
	                          @Nonnull String qualifiedname) {
		qualifiedname=qualifiedname.toLowerCase();
		if (qualifiedname.startsWith("transactiontax")) {
			final String componentname=qualifiedname.substring("TransactionTax".length());
			final Currency c=Currency.find(st,componentname);
			return new TransactionTaxKV(st,c);
		}
		return super.getKVDefinition(st,qualifiedname);
	}

	@Nonnull
	@Override
	public Command getCommandNullable(final State st,
	                                  @Nonnull final String commandname) {
		if (commandname.toLowerCase().startsWith("create")) {
			if (Attribute.find(st.getInstance(),commandname.substring(6)).getType()==ATTRIBUTETYPE.CURRENCY) {
				return new CreateCoinsCommand(commandname.substring(6));
			}
		}
		if (commandname.toLowerCase().startsWith("destroy")) {
			if (Attribute.find(st.getInstance(),commandname.substring(7)).getType()==ATTRIBUTETYPE.CURRENCY) {
				return new DestroyCoinsCommand(commandname.substring(7));
			}
		}
		return super.getCommandNullable(st,commandname);
	}

	@Nonnull
	@Override
	public Pool getPool(final State st,
	                    @Nonnull final String itemname) {
		final Attribute a=Attribute.find(st.getInstance(),itemname);
		if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
			return new CurrencyPool(a.getName());
		}
		return super.getPool(st,itemname);
	}

	@Override
	public Permission getPermission(final State st,
	                                @Nonnull final String itemname) {
		if (itemname.startsWith("Create")) {
			Attribute.find(st.getInstance(),itemname.substring(6));
			return new CurrencyCreateCoinsPermission(itemname.substring(6));
		}
		if (itemname.startsWith("Destroy")) {
			Attribute.find(st.getInstance(),itemname.substring(7));
			return new CurrencyDestroyCoinsPermission(itemname.substring(7));
		}
		return super.getPermission(st,itemname);
	}

	@Nonnull
	@Override
	public Map<String,Pool> getPoolMap(final State st) {
		final Map<String,Pool> map=super.getPoolMap(st);
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				map.put("Currency."+a.getName(),Currency.find(st,a.getName()).getPool(st));
			}
		}
		return map;
	}

	@Nonnull
	@Override
	public Map<String,Command> getCommands(final State st) {
		final Map<String,Command> commands=super.getCommands(st);
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				commands.put("Create"+a.getName(),new CreateCoinsCommand(a.getName()));
				commands.put("Destroy"+a.getName(),new DestroyCoinsCommand(a.getName()));
			}
		}
		return commands;
	}

	@Override
	public Map<String,Permission> getPermissions(final State st) {
		final Map<String,Permission> permissions=super.getPermissions(st);
		if (st==null) { return permissions; }
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				permissions.put("Create"+a.getName(),getPermission(st,"Create"+a.getName()));
				permissions.put("Destroy"+a.getName(),getPermission(st,"Destroy"+a.getName()));
			}
		}
		return permissions;
	}

}

