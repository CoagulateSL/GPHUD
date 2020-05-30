@ModuleDefinition(description="Provides support for exchangable currencies",
                  canDisable=true,
                  defaultDisable=true,
                  implementation="net.coagulate.GPHUD.Modules.Currency.CurrencyModule",
                  forceConfig=true)

@Permissions(description="The ability to reconfigure currency denominations (coin types) and base coin names",
             power=POWER.LOW,
             name="Denominations")


package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.POWER;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
