/**
 * Transport module.
 * More specifically an Import and Export module, a module for transferring settings.
 * Or doing backups I suppose.
 */

@Module.ModuleDefinition(description="Allows import and export of configuration", defaultDisable=true)

@SideMenus(name="Transport", priority=875, requiresPermission="transport.*", url="/transport")

package net.coagulate.GPHUD.Modules.Transport;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;
