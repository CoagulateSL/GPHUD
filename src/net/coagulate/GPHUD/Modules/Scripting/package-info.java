@Module.ModuleDefinition(description="Scripting support for GPHUD", defaultDisable=true, forceConfig=true, implementation="net.coagulate.GPHUD.Modules.Scripting"+
		".ScriptingModule") @Permission.Permissions(description="Allows the creation of scripts", name="Create", power=Permission.POWER.HIGH)


package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
