
@ModuleDefinition(description="Module that supports Characters", canDisable=false, implementation="net.coagulate.GPHUD.Modules.Characters.CharactersModule", forceConfig=true)
@Permissions(name="ViewAll", description="Ability to view other players character records", power=Permission.POWER.LOW)
@Permissions(name="CreateAttribute",description="Ability to create a new attribute or reconfigure an existing one", power=Permission.POWER.MEDIUM)
@Permissions(name="ExceedCharLimits", description="Allowed to exceed the maximum character limit", power=Permission.POWER.LOW)
@Permissions(name="Retire", description="Allowed to force-retire another's character", power=Permission.POWER.LOW)
@Permissions(name="ForceRename", description="Allowed to force-rename another's character", power=Permission.POWER.LOW)
@Permissions(name="DeleteAttribute", description="Remove an attribute from the system", power=Permission.POWER.HIGH)
@Permissions(name="MakeNPC", description="Allows a user to mark their character as an NPC for use by Objects", power=Permission.POWER.MEDIUM)
@SideMenus(name="View Characters", priority=250, requiresPermission="Characters.ViewAll", url="/characters/list?sort=Name")

package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;

