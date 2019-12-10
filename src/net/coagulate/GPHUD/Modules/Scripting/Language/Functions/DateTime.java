package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

public class DateTime {
	@GSFunctions.GSFunction(description = "Gets the current UNIX/Epoch time (e.g. llGetUnixTime)",notes = "",parameters = "",returns = "Integer - current Epoch time (number of seconds since 01/01/1970)")
	public static BCInteger gsGetUnixTime(State st, GSVM vm) {
		return new BCInteger(null, UnixTime.getUnixTime());
	}
}
