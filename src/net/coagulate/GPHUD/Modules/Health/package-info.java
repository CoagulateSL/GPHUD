@ModuleDefinition(description="Provides health support", defaultDisable=true)

@Permissions(name="config", description="Configure the health module", power=Permission.POWER.MEDIUM)

@Permissions(name="sethealth", description="May set any users health value", power=Permission.POWER.LOW)

@KVS(name="allowreset",
     description="May user reset their own health",
     defaultValue="false",
     editPermission="health.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.BOOLEAN,
     hierarchy=KVHIERARCHY.DELEGATING,
     template=false)

@KVS(name="initialhealth",
     description="Initial health value",
     defaultValue="10",
     editPermission="health.config",
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.INTEGER,
     hierarchy=KVHIERARCHY.CUMULATIVE,
     template=true)

@KVS(name="health",
     description="Current health",
     editPermission="health"+".sethealth",
     scope=KVSCOPE.COMPLETE,
     defaultValue="10",
     template=true,
     type=KVTYPE.INTEGER,
     hierarchy=KVHIERARCHY.DELEGATING)

@KVS(name="allowNegative",
     defaultValue="false",
     editPermission="health.config",
     description="Allow health to go negative (otherwise stops at zero)",
     hierarchy=KVHIERARCHY.DELEGATING,
     scope=KVSCOPE.COMPLETE,
     type=KVTYPE.BOOLEAN,
     template=false)

@KVS(name="allowSelfSet",
     description="Allow character to set their own health",
     editPermission="health.config",
     hierarchy=KVHIERARCHY.DELEGATING,
     type=KVTYPE.BOOLEAN,
     defaultValue="false",
     scope=KVSCOPE.COMPLETE,
     template=false)

package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

