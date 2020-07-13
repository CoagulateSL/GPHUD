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
     editpermission="User.SuperAdmin",
     conveyas="legacymenu",
     defaultvalue="--MAINMENU--",
     hidden=true)

package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.GPHUD.Modules.KV.KVHIERARCHY;
import net.coagulate.GPHUD.Modules.KV.KVS;
import net.coagulate.GPHUD.Modules.KV.KVSCOPE;
import net.coagulate.GPHUD.Modules.KV.KVTYPE;
import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Permission.POWER;
import net.coagulate.GPHUD.Modules.Permission.Permissions;

