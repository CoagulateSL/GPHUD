package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class LEDInfo {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Command.Context.ANY,
	          description="Gives information about the RED / TRANSMIT light",
	          permitScripting=false,
	          permitObject=false,
	          permitConsole=false,
	          permitUserWeb=false,
	          permitJSON=false,
	          permitExternal=false)
	public static Response tx(final State st) {
		return new OKResponse(
				"The red light is the TRANSMIT light.\nDARK RED - transmission circuit has been successfully used, and is currently idle.\nBRIGHT RED - transmission in "+"progress, this should complete shortly, if not, wait for it to time out, it will automatically retry.\nBLACK - No transmission made or all servers "+"failed and HUD will reboot.");
	}

	@Nonnull
	@Commands(context=Command.Context.ANY,
	          description="Gives information about the GREEN / RECEIVE light",
	          permitScripting=false,
	          permitObject=false,
	          permitConsole=false,
	          permitUserWeb=false,
	          permitJSON=false,
	          permitExternal=false)
	public static Response rx(final State st) {
		return new OKResponse("The green light is the RECEIVE light.\nDARK GREEN - Receiver has been set up and registered, and is idle.\nBRIGHT GREEN - A message is being "+"received\nBLACK"+" - No receiving circuit has been created, or it failed.");
	}
}
