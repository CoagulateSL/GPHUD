package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.Responses.RebootResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Reboot {
    @Command.Commands(description="Sends a reboot response to the HUD (reboots the HUD)",
                      permitUserWeb=false,
                      context= Command.Context.ANY,
                      permitObject=false,
                      permitExternal=false,
                      permitScripting = false)
    public static Response reboot(@Nonnull final State st) {
        return new RebootResponse("The GPHUDClient.Reboot command was invoked, generating this reboot message.");
    }

}
