@Module.ModuleDefinition(description = "Item Management",
                         forceConfig = true,
                         implementation = "net.coagulate.GPHUD.Modules.Items.ItemsModule",
                         canDisable = false)

@Permission.Permissions(name = "edit",
                        description = "Permission to add or edit item definitions",
                        power = LOW)
@Permission.Permissions(name = "editinventories",
                        description = "Permission to changes what inventories an item is allowed in",
                        power = LOW)

package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;

import static net.coagulate.GPHUD.Modules.Permission.POWER.LOW;