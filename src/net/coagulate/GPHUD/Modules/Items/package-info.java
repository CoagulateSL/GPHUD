@Module.ModuleDefinition(description="Item Management",
                         forceConfig=true,
                         implementation="net.coagulate.GPHUD.Modules.Items.ItemsModule",
                         canDisable=false)

@Permission.Permissions(name="edit",
                        description="Permission to add or edit item definitions",
                        power=LOW) @Permission.Permissions(name="editinventories",
                                                           description="Permission to change what inventories an item is allowed in",
                                                           power=LOW) @Permission.Permissions(name="editverbs",
                                                                                              description="Permission to change what actions an item has",
                                                                                              power=LOW) @Permission.Permissions(
		name="delete",
		description="Permission to permenantly delete items",
		power=MEDIUM) @Permission.Permissions(name="deleteverb",
                                              description="Permission to delete an action",
                                              power=MEDIUM)

package net.coagulate.GPHUD.Modules.Items;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;

import static net.coagulate.GPHUD.Modules.Permission.POWER.LOW;
import static net.coagulate.GPHUD.Modules.Permission.POWER.MEDIUM;
