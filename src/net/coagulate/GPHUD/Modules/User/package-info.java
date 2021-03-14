@ModuleDefinition(canDisable=false,
                  description="User management module")

@Permissions(description="Super admin shadow permission",
             name="SuperAdmin",
             power=POWER.HIGH,
             grantable=false) // note this doesn't actually protect anything, (owners bypass this?) you MUST use State.isSuperAdmin()
// one day i need to make a module able to have many side menus
@SideMenu.SideMenus(name="Reporting",
                    priority=255,
                    requiresPermission="Instance.Reporting",
                    url="/reporting")


package net.coagulate.GPHUD.Modules.User;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.POWER;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.SideMenu;

