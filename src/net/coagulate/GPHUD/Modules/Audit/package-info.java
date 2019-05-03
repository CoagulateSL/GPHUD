@Permissions(name = "view", description = "Permission to view the audit logs")
@SideMenus(name = "Audit", priority = 990, url = "/audit", requiresPermission = "audit.view")
@ModuleDefinition(canDisable = false, description = "Provides access to the auditing logs")
package net.coagulate.GPHUD.Modules.Audit;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;

