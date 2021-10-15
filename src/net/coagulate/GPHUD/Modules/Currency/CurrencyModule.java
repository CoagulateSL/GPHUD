package net.coagulate.GPHUD.Modules.Currency;


import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Modules.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

public class CurrencyModule extends ModuleAnnotation {

	public CurrencyModule(final String name,
	                      final ModuleDefinition def) {
		super(name,def);
	}

	// ---------- STATICS ----------
	@Nonnull
	public static String templateCurrencyLong(@Nonnull final State st,
	                                          final String key) {
		final String name=key.split(":")[0].substring(2);
		final Attribute a=st.getAttribute(name);
		final Currency currency=Currency.find(st,name);
		return currency.longSum(st);
	}

	@Nonnull
	@Override
	public Map<String,KV> getKVDefinitions(final State st) {
		final Map<String,KV> definitions=new TreeMap<>(super.getKVDefinitions(st));
		if (st==null) { return definitions; }
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				definitions.put("TransactionTax"+a.getName(),getKVDefinition(st,"TransactionTax"+a.getName()));
				definitions.put("TransactionTaxRecipient"+a.getName(),getKVDefinition(st,"TransactionTaxRecipient"+a.getName()));
			}
		}
		return definitions;
	}

	@Override
	public KV getKVDefinition(final State st,
	                          @Nonnull String qualifiedname) {
		qualifiedname=qualifiedname.toLowerCase();
		if (qualifiedname.startsWith("transactiontaxrecipient")) {
			final String componentname=qualifiedname.substring("TransactionTaxRecipient".length());
			final Currency c=Currency.find(st,componentname);
			return new TransactionTaxRecipientKV(st,c);
		}
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
		if (commandname.toLowerCase().startsWith("transferpaytax")) {
			final String currencyname=commandname.substring("transferpaytax".length());
			if (Attribute.find(st.getInstance(),currencyname).getType()==ATTRIBUTETYPE.CURRENCY) {
				return new TransferCoinsCommand(currencyname,true);
			}
		}
		if (commandname.toLowerCase().startsWith("transfer")) {
			if (Attribute.find(st.getInstance(),commandname.substring("transfer".length())).getType()==ATTRIBUTETYPE.CURRENCY) {
				return new TransferCoinsCommand(commandname.substring("transfer".length()),false);
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
		if (itemname.toLowerCase().startsWith("create")) {
			Attribute.find(st.getInstance(),itemname.substring(6));
			return new CurrencyCreateCoinsPermission(itemname.substring(6));
		}
		if (itemname.toLowerCase().startsWith("destroy")) {
			Attribute.find(st.getInstance(),itemname.substring(7));
			return new CurrencyDestroyCoinsPermission(itemname.substring(7));
		}
		return super.getPermission(st,itemname);
	}

	@Nonnull
	@Override
	public Map<String,Pool> getPoolMap(final State st) {
		final Map<String,Pool> map=new TreeMap<>(super.getPoolMap(st));
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
		final Map<String,Command> commands=new TreeMap<>(super.getCommands(st));
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				commands.put("Create"+a.getName(),new CreateCoinsCommand(a.getName()));
				commands.put("Destroy"+a.getName(),new DestroyCoinsCommand(a.getName()));
				commands.put("Transfer"+a.getName(),new TransferCoinsCommand(a.getName(),false));
				commands.put("TransferPayTax"+a.getName(),new TransferCoinsCommand(a.getName(),true));
			}
		}
		return commands;
	}

	@Override
	public Map<String,Permission> getPermissions(final State st) {
		final Map<String,Permission> permissions=new TreeMap<>(super.getPermissions(st));
		if (st==null) { return permissions; }
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				permissions.put("create"+a.getName().toLowerCase(),getPermission(st,"Create"+a.getName()));
				permissions.put("destroy"+a.getName().toLowerCase(),getPermission(st,"Destroy"+a.getName()));
			}
		}
		return permissions;
	}

	// ---------- INSTANCE ----------
	@Override
	public void addTemplateDescriptions(final State st,
	                                    final Map<String,String> templates) {
		super.addTemplateDescriptions(st,templates);
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				templates.put("--"+a.getName().toUpperCase()+":LONG--","Long format template for currenct "+a.getName());
			}
		}
	}

	@Override
	public void addTemplateMethods(final State st,
	                               final Map<String,Method> ret) {
		super.addTemplateMethods(st,ret);
		for (final Attribute a: Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				try {
					ret.put("--"+a.getName().toUpperCase()+":LONG--",getClass().getMethod("templateCurrencyLong",State.class,String.class));
				}
				catch (final NoSuchMethodException failure) {
					throw new SystemImplementationException("Reflection failed for long form currency templater");
				}
			}
		}
	}
}
