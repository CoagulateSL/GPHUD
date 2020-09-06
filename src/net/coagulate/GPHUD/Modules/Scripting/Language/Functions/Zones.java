package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Zones {

	// ---------- STATICS ----------
	@GSFunction(description="Get the current zone a character is in",
	            parameters="Character - character to get the zone information of",
	            returns="The String name of the zone the character is in, may be blank",
	            notes="",
	            privileged=false,
	            category= SCRIPT_CATEGORY.ZONES)
	@Nonnull
	public static BCString gsGetZone(@Nonnull final State st,
	                                 @Nonnull final GSVM vm,
	                                 @Nonnull final BCCharacter target) {
		GSFunctions.assertModule(st,"Zoning");
		final Zone zone=target.getContent().getZone();
		if (zone==null) { return new BCString(null,""); }
		return new BCString(null,zone.getName());
	}
}
