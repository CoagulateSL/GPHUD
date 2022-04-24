@ModuleDefinition(description="Provides the interface for a GPHUD Server, providing region broadcast, HUD dispensing and visitation tracking",
		canDisable=false)

@Permissions(name="Config",
		description="Configure operations of the GPHUD Server",
		power=Permission.POWER.MEDIUM)

@KVS(scope=KVSCOPE.SERVER,
		type=KVTYPE.BOOLEAN,
		defaultValue="false",
		editPermission="GPHUDServer.Config",
		name="AutoAttach",
		description="Enable Experience based auto attachment of the HUD",
		hierarchy=KVHIERARCHY.DELEGATING,
		template=false)

@KVS(scope=KVSCOPE.SERVER,
		type=KVTYPE.BOOLEAN,
		defaultValue="false",
		editPermission="GPHUDServer.Config",
		name="ParcelONLY",
		description="Scan only the PARCEL rather than the whole REGION",
		hierarchy=KVHIERARCHY.DELEGATING,
		template=false)

@KVS(scope=KVSCOPE.SERVER,
		type=KVTYPE.FLOAT,
		hierarchy=KVHIERARCHY.DELEGATING,
		editPermission="GPHUDServer.Config",
		defaultValue="0",
		description="Minimum height for HUD attach",
		name="DispenserMinimumZ",
		template=false
)

@KVS(scope=KVSCOPE.SERVER,
		type=KVTYPE.FLOAT,
		hierarchy=KVHIERARCHY.DELEGATING,
		editPermission="GPHUDServer.Config",
		defaultValue="9999",
		description="Maximum height for HUD attach",
		name="DispenserMaximumZ",
		template=false
)

@Change(date="2022-04-24", component=COMPONENT.RegionServer, type=CHANGETYPE.Add, message="Server emits warnings if there are insufficient prims before attempting to rez a HUD, and warns if a HUD rez doesn't succeed.")
package net.coagulate.GPHUD.Modules.GPHUDServer;

import net.coagulate.GPHUD.Classes.COMPONENT;
import net.coagulate.GPHUD.Classes.Change;
import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.SL.ChangeLogging.CHANGETYPE;

