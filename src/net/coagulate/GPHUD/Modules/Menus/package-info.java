@ModuleDefinition(canDisable=false,
                  description="Controls HUD menus",
                  forceConfig=true,
                  implementation="net.coagulate.GPHUD.Modules.Menus.MenuModule")

@Permissions(description="Permission to reconfigure the menus",
             name="Config",
             power=Permission.POWER.MEDIUM)

package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

