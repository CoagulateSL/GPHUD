package net.coagulate.GPHUD.Modules.Currency;


import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE;
import net.coagulate.GPHUD.Data.Currency;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;

public class CurrencyModule extends ModuleAnnotation {

	public CurrencyModule(String name,
	                      ModuleDefinition def) {
		super(name,def);
	}

	@Nonnull
	@Override
	public Pool getPool(State st,
	                    @Nonnull String itemname) {
		Attribute a=Attribute.find(st.getInstance(),itemname);
		if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
			return new CurrencyPool(a.getName());
		}
		return super.getPool(st,itemname);
	}

	@Nonnull
	@Override
	public Map<String,Pool> getPoolMap(State st) {
		Map<String,Pool> map=super.getPoolMap(st);
		for (Attribute a:Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				map.put("Currency."+a.getName(),Currency.find(st,a.getName()).getPool(st));
			}
		}
		return map;
	}

	@Override
	public Permission getPermission(State st,
	                                @Nonnull String itemname) {
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

// ---------- INSTANCE ----------
@Override
public Map<String,Permission> getPermissions(State st) {
	Map<String,Permission> permissions=super.getPermissions(st);
	if (st==null) { return permissions; }
	for (Attribute a: Attribute.getAttributes(st.getInstance())) {
		if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
			permissions.put("Create"+a.getName(),getPermission(st,"Create"+a.getName()));
			permissions.put("Destroy"+a.getName(),getPermission(st,"Destroy"+a.getName()));
		}
	}
	return permissions;
}

	@Nonnull
	@Override
	public Command getCommandNullable(State st,
	                                  @Nonnull String commandname) {
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
	public Map<String,Command> getCommands(State st) {
		Map<String,Command> commands=super.getCommands(st);
		for (Attribute a:Attribute.getAttributes(st.getInstance())) {
			if (a.getType()==ATTRIBUTETYPE.CURRENCY) {
				commands.put("Create"+a.getName(),new CreateCoinsCommand(a.getName()));
				commands.put("Destroy"+a.getName(),new DestroyCoinsCommand(a.getName()));
			}
		}
		return commands;
	}
}

