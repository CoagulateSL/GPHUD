@Module.ModuleDefinition(description="Enables admin notes on characters and avatars", defaultDisable=true) @Permission.Permissions(description="Can view admin notes on characters and avatars", name="View", power=Permission.POWER.LOW) @Permission.Permissions(description="Can add notes to characters and avatars", name="Add", power=Permission.POWER.LOW) @SideMenu.SideMenus(name="View Admin Notes", priority=850, requiresPermission="notes.view", url="/Notes/ViewAll")

package net.coagulate.GPHUD.Modules.Notes;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.SideMenu;
