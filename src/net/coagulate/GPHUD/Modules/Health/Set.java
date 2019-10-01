package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.SayResponse;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import static net.coagulate.GPHUD.Data.Audit.audit;

public class Set {

	@Command.Commands(description = "Set own health",context = Command.Context.CHARACTER)
	public static Response set(State st,
	                           @Argument.Arguments(description = "Ammount of health to set to",type = Argument.ArgumentType.INTEGER)
                               Integer target
	                           )
	{
		// is this command enabled?
		if (st.getKV("Health.allowSelfSet").boolValue()==false) {
			return new ErrorResponse("Administration has not enabled this command at this instance.");
		}
		if (st.getKV("Health.allowNegative").boolValue()==false && target<0) {
			return new ErrorResponse("You can not set your health to a negative value");
		}
		if (target>st.getKV("Health.initialHealth").intValue()) {
			return new ErrorResponse("You can not set your health higher than "+st.getKV("Health.InitialHealth").intValue());
		}
		// since health is actually a cumulative thing, lets see what the difference between the char's health and the total health is
		String oldvalue=st.getKV("Health.health").value();
		// so we want to set to the target - the baseline i guess
		int setto=target;
		st.setKV(st.getCharacter(),"health.health",setto+"");
		st.purgeCache(st.getCharacter());
		audit(st,Audit.OPERATOR.CHARACTER,st.getAvatar(),st.getCharacter(),"Set","Health",oldvalue,target+"","Set character their own health");
		return new SayResponse("set their health to "+target,st.getCharacter().getName());
	}
}
