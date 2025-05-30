package net.coagulate.GPHUD.Modules.Transport;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Transport.Transports.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Import/export module for GPHUD.
 * <p>
 * Exports configuration data to a JSON file, and supports reimporting it.
 * Largely one module per table / object type we manage.
 * Ensure you consider dependancies ; e.g. probably a bad idea to import ItemInventories before Inventories
 */
public class TransportModule extends ModuleAnnotation {
	
	static final Transporter[] transports=
			{new ModulesTransport(),new AttributeTransport(),new CurrencyTransport(),new ScriptTransport(),
			 new MenuTransport(),new AliasTransport(),new InstanceKVTransport(),new PermissionsGroupTransport(),
			 new EffectTransport(),new CharacterGroupTransport(),new EventTransport(),new ItemTransport(),
			 new ObjectTypeTransport(),new ZoneTransport()};
	// an importantly ordered list, dependancies in the DB
	
	@Override
	@Nullable
	public Permission getPermission(final State st,@Nonnull final String itemname) {
		Permission p=super.getPermission(st,itemname);
		if (p!=null) {
			return p;
		}
		p=getPermissions(st).getOrDefault(itemname,null);
		return p;
	}
	
	@Override
	public Map<String,Permission> getPermissions(final State st) {
		final Map<String,Permission> map=new TreeMap<>(super.getPermissions(st));
		for (final Transporter t: transports) {
			map.put("Import"+t.transportName(),new Permission() {
				@Override
				public Module getModule(final State st) {
					return Modules.get(st,"Transport");
				}
				
				@Override
				public boolean isGenerated() {
					return true;
				}
				
				@Nonnull
				@Override
				public String name() {
					return "Import"+t.transportName();
				}
				
				@Nonnull
				@Override
				public String description() {
					return "Allows the importing of "+t.transportName();
				}
				
				@Nonnull
				@Override
				public POWER power() {
					return POWER.HIGH;
				}
				
				@Override
				public boolean grantable() {
					return true;
				}
			});
			map.put("Export"+t.transportName(),new Permission() {
				@Override
				public Module getModule(final State st) {
					return Modules.get(st,"Transport");
				}
				
				@Override
				public boolean isGenerated() {
					return true;
				}
				
				@Nonnull
				@Override
				public String name() {
					return "Export"+t.transportName();
				}
				
				@Nonnull
				@Override
				public String description() {
					return "Allows the exporting of "+t.transportName();
				}
				
				@Nonnull
				@Override
				public POWER power() {
					return POWER.MEDIUM;
				}
				
				@Override
				public boolean grantable() {
					return true;
				}
			});
		}
		return map;
	}
	
	/**
	 * Construct the transport module
	 *
	 * @param name       Name of the module
	 * @param annotation Annotation for the module
	 */
	public TransportModule(final String name,final ModuleDefinition annotation) {
		super(name,annotation);
	}
	
	/* JSON Export Format
	 * {
	 *      identifier: "GPHUD Export",                 Used to verify import file
	 *      exportedBy: "SL User Name",                 Decorative
	 *      exportedByKey: "123456-12345678(...)",      Decorative
	 *      exportedWhen: "2024-12-24 00:00:00",        Decorative
	 *      version: 1,                                 Reserved for future blah
	 *      module -> {module specific JSON object}     Module specific data blob (usually a map of object name -> object data as json object)
	 *      (...)                                       for all modules
	 * }
	 */
	
	
	/*
	 * Mark tables off below when added to import/export
	 * +-------------------------+
	 * | Tables_in_gphud         |
	 * +-------------------------+
	 * | adminnotes              | (Char data)
	 * | aliases                 | (Implemented)
	 * | attributes              | (Implemented)
	 * | audit                   | (Don't export)
	 * | charactergroupkvstore   | (Implemented)
	 * | charactergroupmembers   | (Char data)
	 * | charactergroups         | (Implemented)
	 * | characterkvstore        | (Char data)
	 * | characterpools          | (Char data)
	 * | characters              | (Char data)
	 * | charactersets           | (Char data)
	 * | cookies                 | (Don't export)
	 * | currencies              | (Implemented)
	 * | currencycoins           | (Implemented)
	 * | effects                 | (Implemented)
	 * | effectsapplications     | (Char data)
	 * | effectskvstore          | (Implemented)
	 * | events                  | (Implemented)
	 * | eventskvstore           | (Implemented)
	 * | eventslocations         | (Implemented)
	 * | eventsschedule          | (Implemented)
	 * | eventvisits             | (Char data)
	 * | instancedevelopers      | (Skip?)
	 * | instancekvstore         | (Implemented)
	 * | instances               | (NEVER export :)
	 * | iteminventories         | (Char data?)
	 * | items                   | (Implemented)
	 * | itemverbs               | (Implemented)
	 * | landmarks               | (Skipped)
	 * | menus                   | (Implemented)
	 * | messages                | (Char data)
	 * | objects                 | (Do not export)
	 * | objecttypes             | (Implemented)
	 * | permissions             | (Implemented)
	 * | permissionsgroupmembers | (Implemented)
	 * | permissionsgroups       | (Implemented)
	 * | ping                    | (Non instanced data)
	 * | primarycharacters       | (Char data, if not entirely obsoleted)
	 * | regionkvstore           | (Don't export)
	 * | regions                 | (Don't export)
	 * | schemaversions          | (Non instanced data)
	 * | scriptruns              | (Char data)
	 * | scripts                 | (Implemented)
	 * | visits                  | (Char data)
	 * | zoneareas               | (Implemented)
	 * | zonekvstore             | (Implemented)
	 * | zones                   | (Implemented)
	 * +-------------------------+
	 */
}
