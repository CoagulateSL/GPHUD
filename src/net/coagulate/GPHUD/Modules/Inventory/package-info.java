@Module.ModuleDefinition(description="Handles Inventory Sets",
                         canDisable=false,
                         forceConfig=true,
                         implementation="net.coagulate.GPHUD.Modules.Inventory.InventoryModule")

@Permission.Permissions(name="ConfigureLimits",
                        description="Configure the storage limits for inventories",
                        power=Permission.POWER.LOW) @Permission.Permissions(name="ConfigureAccess",
                                                                            description="Configure the accessibility KV for inventories",
                                                                            power=Permission.POWER.LOW)
package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;