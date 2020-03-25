package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class EffectsCommands {

	@Command.Commands(description="Applies an effect to a character",requiresPermission="Effects.Apply",context=Command.Context.AVATAR)
	public static Response apply(@Nonnull final State st,
	                             @Argument.Arguments(description="Character to apply effect to",type=Argument.ArgumentType.CHARACTER)
	                             @Nonnull final Char target,
	                             @Argument.Arguments(description="Name of effect to apply",type=Argument.ArgumentType.EFFECT)
	                             @Nonnull final Effect effect,
	                             @Argument.Arguments(description="Duration for effect (in seconds)",type=Argument.ArgumentType.INTEGER)
	                             final int seconds) {
		if (effect.apply(st,true,target,seconds)) {
			return new OKResponse("Effect applied ");
		} else {
			return new OKResponse("Effect already exists on target for a longer duration");
		}
	}

}
