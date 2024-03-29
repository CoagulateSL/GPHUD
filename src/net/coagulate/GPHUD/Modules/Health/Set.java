package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.SayResponse;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

import static net.coagulate.GPHUD.Data.Audit.audit;

public class Set {
	
	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(description="Set own health", context=Command.Context.CHARACTER)
	public static Response set(@Nonnull final State st,
	                           @Argument.Arguments(name="target",
	                                               description="Ammount of health to set to",
	                                               type=Argument.ArgumentType.INTEGER) final Integer target) {
		// is this command enabled?
		if (!st.getKV("Health.allowSelfSet").boolValue()) {
			return new ErrorResponse("Administration has not enabled this command at this instance.");
		}
		if (!st.getKV("Health.allowNegative").boolValue()&&target<0) {
			return new ErrorResponse("You can not set your health to a negative value");
		}
		if (target>st.getKV("Health.initialHealth").intValue()) {
			return new ErrorResponse(
					"You can not set your health higher than "+st.getKV("Health.InitialHealth").intValue());
		}
		// since health is actually a cumulative thing, lets see what the difference between the char's health and the total health is
		final String oldvalue=st.getKV("Health.health").value();
		// so we want to set to the target - the baseline i guess
		final int setto=target;
		st.setKV(st.getCharacter(),"health.health",String.valueOf(setto));
		st.purgeCache(st.getCharacter());
		audit(st,
		      Audit.OPERATOR.CHARACTER,
		      st.getAvatarNullable(),
		      st.getCharacter(),
		      "Set",
		      "Health",
		      oldvalue,
		      String.valueOf(target),
		      "Set character their own health");
		return new SayResponse("set their health to "+target,st.getCharacter().getName());
	}
}
