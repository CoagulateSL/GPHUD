@ModuleDefinition(description = "General purpose 'dice roller' commands")
@KVS(name = "defaultsides", scope = KVSCOPE.COMPLETE, type = KVTYPE.INTEGER, description = "Number of sides on the default dice roll", editpermission = "roller.config", defaultvalue = "100", hierarchy = KVHIERARCHY.DELEGATING, template = true)
@KVS(name = "defaultcount", scope = KVSCOPE.COMPLETE, type = KVTYPE.INTEGER, description = "Number of dice to roll by default", editpermission = "roller.config", defaultvalue = "1", hierarchy = KVHIERARCHY.DELEGATING, template = true)
@Permissions(name = "config", description = "Ability to configure the dice roller module")
package net.coagulate.GPHUD.Modules.Roller;

import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

