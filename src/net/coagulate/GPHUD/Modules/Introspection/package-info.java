@SideMenus(name="Introspection", priority=998, url="/introspection/")

@ModuleDefinition(canDisable=false, description="Provides GPHUD introspection capabilities for developers")

@Permission.Permissions(name="ViewStatus",
                        power=Permission.POWER.LOW,
                        description="Allows viewing the Introspection/Status page, which can be used to location track objects and players")

package net.coagulate.GPHUD.Modules.Introspection;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;
