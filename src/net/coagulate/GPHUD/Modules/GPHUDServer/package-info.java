@ModuleDefinition(description="Provides the interface for a GPHUD Server, providing region broadcast, HUD dispensing and visitation tracking",
                  canDisable=false)

@Permissions(name="Config",
             description="Configure operations of the GPHUD Server",
             power=Permission.POWER.MEDIUM)

@KVS(scope=KVSCOPE.SERVER,
     type=KVTYPE.BOOLEAN,
     defaultvalue="false",
     editpermission="GPHUDServer.Config",
     name="AutoAttach",
     description="Enable Experience based auto attachment of the HUD",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(scope=KVSCOPE.SERVER,
     type=KVTYPE.BOOLEAN,
     defaultvalue="false",
     editpermission="GPHUDServer.Config",
     name="ParcelONLY",
     description="Scan only the PARCEL rather than the whole REGION",
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

package net.coagulate.GPHUD.Modules.GPHUDServer;

import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;


