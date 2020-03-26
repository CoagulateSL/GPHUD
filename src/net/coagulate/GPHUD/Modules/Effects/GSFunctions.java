package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class GSFunctions {

	@GSFunction(description="Gets a List of Strings, naming all the Effects applied to the character",
	            parameters="Character - Who to list the effects of",
	            returns="List of "+"Strings - names of all the applied effects",
	            notes="",
	            privileged=false)
	@Nonnull
	public static BCList gsGetEffects(final State st,
	                                  @Nonnull final GSVM vm,
	                                  @Nonnull final BCCharacter target) {
		BCList ret=new BCList(null);
		for (Effect e: Effect.get(st,target.getContent())) {
			ret.append(new BCString(null,e.getName()));
		}
		return ret;
	}

	@GSFunction(description="This retrieves the time left on a particular effect, or minus one if the effect isn't active on the character",
	            parameters="Character - Character to query the effect status of<br>String - Name of the Effect to query",
	            privileged=false,
	            returns="Integer - number of seconds left on the effect, or -1 if the effect is not active on this character",
	            notes="")
	@Nonnull
	public static BCInteger gsGetEffectDuration(final State st,
	                                            @Nonnull final GSVM vm,
	                                            @Nonnull final BCCharacter target,
	                                            @Nonnull final BCString effectname) {
		return new BCInteger(null,Effect.get(st,effectname.getContent()).remains(target.getContent()));
	}
}
