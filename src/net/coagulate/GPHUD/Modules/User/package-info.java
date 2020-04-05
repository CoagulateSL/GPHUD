@ModuleDefinition(canDisable=false,
                  description="User management module") @Permissions(description="Super admin shadow permission",
                                                                     name="SuperAdmin",
                                                                     power=POWER.HIGH,
                                                                     grantable=false)
package net.coagulate.GPHUD.Modules.User;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.POWER;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

