@ModuleDefinition(description="Provides support for Character Groups (used for factions, races, classes, attributes etc)",
                  implementation="net.coagulate.GPHUD.Modules.Groups.GroupModule")

@Permissions(name="Create",
             description="Create a Group",
             power=Permission.POWER.LOW)

@Permissions(name="Delete",
             description="Delete a Group",
             power=Permission.POWER.HIGH)

@SideMenus(name="Groups",
           priority=55,
           url="/groups")

@Permissions(name="SetOwner",
             description="May set the owner on a Group",
             power=Permission.POWER.LOW)

@Permissions(name="SetGroup",
             description="May set a characters group memberships",
             power=Permission.POWER.LOW)

@Permissions(name="SetOpen",
             description="May toggle the open status of a group",
             power=Permission.POWER.LOW)

@Permissions(name="SetPrecedence",
             description="May alter the KV precedence of a group",
             power=Permission.POWER.LOW)


package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;

