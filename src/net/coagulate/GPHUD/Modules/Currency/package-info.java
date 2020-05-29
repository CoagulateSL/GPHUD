@ModuleDefinition(description="Provides support for exchangable currencies",
                  canDisable=true,
                  defaultDisable=true,
                  implementation="net.coagulate.GPHUD.Modules.Currency.CurrencyModule")

@Permissions(description="The ability to reconfigure currency denominations (coin types)",
             power=POWER.LOW,
             name="Denominations")


package net.coagulate.GPHUD.Modules.Currency;

import net.coagulate.GPHUD.Modules.Module.ModuleDefinition;
import net.coagulate.GPHUD.Modules.Permission.POWER;
import net.coagulate.GPHUD.Modules.Permission.Permissions;
