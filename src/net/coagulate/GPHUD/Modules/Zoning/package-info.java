
@ModuleDefinition(defaultDisable=true, description="Zoning management", forceConfig=true) 
@Permissions(description="Permission to create or modify zones", name="Config",power=Permission.POWER.LOW)
@KVS(name="EntryMessage", defaultvalue="", description="Message emitted when zone is entered", editpermission="Zoning.Config", scope=KVSCOPE.ZONE, type=KVTYPE.TEXT, template=false)
package net.coagulate.GPHUD.Modules.Zoning;

import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

