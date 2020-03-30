package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Set;

public class EffectsCommands {

	@Command.Commands(description="Administratively applies an effect to a character",requiresPermission="Effects.Apply",context=Command.Context.AVATAR)
	public static Response apply(@Nonnull final State st,
	                             @Argument.Arguments(description="Character to apply effect to",type=Argument.ArgumentType.CHARACTER)
	                             @Nonnull final Char target,
	                             @Argument.Arguments(description="Name of effect to apply",type=Argument.ArgumentType.EFFECT)
	                             @Nonnull final Effect effect,
	                             @Argument.Arguments(description="Duration for effect (in seconds)",type=Argument.ArgumentType.INTEGER)
	                             final int seconds) {
		if (effect.apply(st,true,target,seconds)) {
			return new OKResponse("Effect applied");
		} else {
			return new OKResponse("Effect already exists on target for a longer duration");
		}
	}

	@Command.Commands(description="Administratively removes an effect from a character",requiresPermission="Effects.Remove",context=Command.Context.AVATAR)
	public static Response remove(@Nonnull final State st,
	                              @Argument.Arguments(description="Character to remove effect from",type=Argument.ArgumentType.CHARACTER)
                                  @Nonnull final Char target,
	                              @Argument.Arguments(description="Effect to remove",type=Argument.ArgumentType.EFFECT)
                                  @Nonnull final Effect effect) {
		if (effect.remove(st,target,true)) {
			return new OKResponse("Effect removed");
		} else {
			return new OKResponse("Effect didn't exist on character to remove");
		}
	}

	@Command.Commands(description="Show effects applied to your character",context=Command.Context.CHARACTER)
	public static Response show(@Nonnull final State st) {
		Effect.expirationCheck(st,st.getCharacter());
		final Set<Effect> effects=Effect.get(st,st.getCharacter());
		if (effects.isEmpty()) { return new OKResponse("You have no effects applied"); }
		String response="Current Effects:";
		for (final Effect effect:effects) {
			response+="\n"+effect.getName()+" (";
			response+=effect.humanRemains(st.getCharacter());
			response+=")";
		}
		return new OKResponse(response);
	}
}
