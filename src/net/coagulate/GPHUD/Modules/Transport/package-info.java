/**
 * Transport module.
 * More specifically an Import and Export module, a module for transferring settings.
 * Or doing backups I suppose.
 */

@Module.ModuleDefinition(description="Allows import and export of configuration",
                         defaultDisable=true,
                         implementation="net.coagulate.GPHUD.Modules.Transport.TransportModule")

@SideMenus(name="Transport", priority=875, requiresPermission="transport.*", url="/transport")

@Classes.Change(component=Classes.COMPONENT.Core,
                type=ChangeLogging.CHANGETYPE.Add,
                date="2025-01-04",
                message="Added Transport module - used for importing and exporting configuration data")
package net.coagulate.GPHUD.Modules.Transport;

import net.coagulate.GPHUD.Classes;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;
import net.coagulate.SL.ChangeLogging;
