/**
 * Module for managing configuration and module enablement.
 */


@ModuleDefinition(canDisable=false,
                  description="Module for managing the configuration of the instance",
                  implementation="net.coagulate.GPHUD.Modules.Configuration.ConfigurationModule")

@SideMenus(name="Configuration",
           priority=800,
           url="/configuration/")

package net.coagulate.GPHUD.Modules.Configuration;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.SideMenu.SideMenus;

