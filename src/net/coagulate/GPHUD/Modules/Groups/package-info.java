@ModuleDefinition(description = "Provides support for Character Groups (used for factions, races, classes, attributes etc)",implementation = "net.coagulate.GPHUD.Modules.Groups.GroupModule")
@Permissions(name="Create",description="Create a Group")
@Permissions(name="Delete",description="Delete a Group")
@SideMenus(name = "Groups",priority = 55,url = "/groups")
@Permissions(name="SetOwner",description="May set the owner on a Group")
@Permissions(name="SetGroup",description="May set a characters group memberships")
@Permissions(name="SetOpen",description="May toggle the open status of a group")
package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;

