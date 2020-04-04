@ModuleDefinition(description="Allows configuration of the External Access API.  See documentation.",
                  defaultDisable=true) @Permissions(description="Allows authorisation of a developer to the instance",
                                                    power=POWER.HIGH,
                                                    name="Authorise") @Permissions(description="Allows deauthorisation of a developer to the instance",
                                                                                   power=POWER.MEDIUM,
                                                                                   name="DeAuthorise")
package net.coagulate.GPHUD.Modules.External;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.POWER;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
