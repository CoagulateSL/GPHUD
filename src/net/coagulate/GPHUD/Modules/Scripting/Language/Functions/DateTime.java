package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class DateTime {
	private DateTime() {}

	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Gets the current UNIX/Epoch time (e.g. llGetUnixTime)",
	                        notes="",
	                        parameters="",
	                        returns="Integer - current Epoch time (number of "+"seconds since 01/01/1970)",
	                        privileged=false)
	public static BCInteger gsGetUnixTime(final State st,
	                                      final GSVM vm) {
		return new BCInteger(null,UnixTime.getUnixTime());
	}
}
