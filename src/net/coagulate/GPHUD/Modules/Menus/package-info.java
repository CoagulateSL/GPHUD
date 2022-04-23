@ModuleDefinition(canDisable=false,
                  description="Controls HUD menus",
                  forceConfig=true,
                  implementation="net.coagulate.GPHUD.Modules.Menus.MenuModule")

@Permissions(description="Permission to reconfigure the menus",
             name="Config",
             power=Permission.POWER.MEDIUM)

@Permissions(description="Permission to delete a menu",
             name="Delete",
             power=POWER.MEDIUM)

@KVS(description="Conveyance container for main menu template, best not messed with.",
     type=KVTYPE.TEXT,
     name="MainMenuContainer",
     hierarchy=KVHIERARCHY.NONE,
     scope=KVSCOPE.INSTANCE,
     template=true,
     editPermission ="User.SuperAdmin",
     conveyAs ="legacymenu",
     defaultValue ="--MAINMENU--",
     hidden=true)

@Classes.Change(component=Classes.COMPONENT.Core,type=ChangeLogging.CHANGETYPE.Fix,date="2022-04-23",message="Menu descriptions are now conveyed to the HUD and displayed, also editable.  Note that main menu description support requires a HUD upgrade")
@Classes.Change(component=Classes.COMPONENT.Protocol,type=ChangeLogging.CHANGETYPE.Add,date="2022-04-23",message="HUD Protocol version 5 expects the mainmenu payload to be prefixed with a description.")
package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.GPHUD.Classes;
import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.POWER;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.SL.ChangeLogging;

