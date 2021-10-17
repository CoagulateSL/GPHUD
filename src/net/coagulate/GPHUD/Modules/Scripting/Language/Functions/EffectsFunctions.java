package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.GSFunction;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class EffectsFunctions {

	// ---------- STATICS ----------
	@GSFunction(description="Gets a List of Strings, naming all the Effects applied to the character",
	            parameters="Character - Who to list the effects of",
	            returns="List of "+"Strings - names of all the applied effects",
	            notes="",
	            privileged=false,
	            category= SCRIPT_CATEGORY.EFFECTS)
	@Nonnull
	public static BCList gsGetEffects(final State st,
	                                  @Nonnull final GSVM vm,
	                                  @Nonnull final BCCharacter target) {
		net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.assertModule(st,"Effects");
		final BCList ret=new BCList(null);
		for (final Effect e: Effect.get(st,target.getContent())) {
			ret.append(new BCString(null,e.getName()));
		}
		return ret;
	}

	@GSFunction(description="This retrieves the time left on a particular effect, or minus one if the effect isn't active on the character",
	            parameters="Character - Character to query the effect status of<br>String - Name of the Effect to query",
	            privileged=false,
	            returns="Integer - number of seconds left on the effect, or -1 if the effect is not active on this character",
	            notes="",
	            category= SCRIPT_CATEGORY.EFFECTS)
	@Nonnull
	public static BCInteger gsGetEffectDuration(final State st,
	                                            @Nonnull final GSVM vm,
	                                            @Nonnull final BCCharacter target,
	                                            @Nonnull final BCString effectname) {
		net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.assertModule(st,"Effects");
		return new BCInteger(null,Effect.get(st,effectname.getContent()).remains(target.getContent()));
	}

	@GSFunction(description="Applies an effect to another character",
	            parameters="Character - Target to apply effect to<br>String - Name of the effect<br>Integer - Number of seconds to apply effect for",
	            privileged=false,
	            returns="Integer - the number zero if buff was not applied, 1 if it was",
	            notes="",
	            category= SCRIPT_CATEGORY.EFFECTS)
	public static BCInteger gsApplyEffect(final State st,
	                                      @Nonnull final GSVM vm,
	                                      @Nonnull final BCCharacter target,
	                                      @Nonnull final BCString effectname,
	                                      @Nonnull final BCInteger duration) {
		net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.assertModule(st,"Effects");
		if (Effect.get(st,effectname.getContent()).apply(st,false,target.getContent(),duration.getContent())) {
			return new BCInteger(null,1);
		}
		else { return new BCInteger(null,0); }
	}

	@GSFunction(description="Removes an effect from another character",
	            parameters="Character - Target to remove effect from<br>String - name of effect to remove",
	            privileged=false,
	            returns="Integer - 1 if an effect was removed, otherwise zero",
	            notes="",
	            category= SCRIPT_CATEGORY.EFFECTS)
	public static BCInteger gsRemoveEffect(final State st,
	                                       @Nonnull final GSVM vm,
	                                       @Nonnull final BCCharacter target,
	                                       @Nonnull final BCString effect) {
		net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.assertModule(st,"Effects");
		if (Effect.get(st,effect.getContent()).remove(st,target.getContent(),false)) {
			return new BCInteger(null,1);
		}
		else { return new BCInteger(null,0); }
	}

	@GSFunction(description="Gets the MetaData attached to a named effect",
				parameters="String - Name of the effect to query",
				privileged = false,
				returns="String - The metadata attached to the effect, may be an empty string",
				notes="",
				category = SCRIPT_CATEGORY.EFFECTS)
	public static BCString gsGetEffectMetaData(final State st,
											   @Nonnull final GSVM vm,
											   @Nonnull final BCString effect) {
		net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.assertModule(st,"Effects");
		return new BCString(null,Effect.get(st,effect.getContent()).getMetaData());
	}
}
