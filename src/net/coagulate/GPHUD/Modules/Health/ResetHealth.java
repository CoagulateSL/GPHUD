package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.SayResponse;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Roller.Roller;
import net.coagulate.GPHUD.State;

import java.util.List;

/**
 * @author Iain Price
 */
public class ResetHealth {

	@Command.Commands(context = Command.Context.CHARACTER, description = "Reset the character's health to the initial value")
	public static Response resetHealth(State st) {
		if (!st.getKV("health.allowreset").boolValue()) {
			throw new UserException("Resetting of your health is not permitted.");
		}
		int oldhealth = st.getKV("health.health").intValue();
		int newvalue = st.getKV("health.initialhealth").intValue();
		st.setKV(st.getCharacter(), "health.health", newvalue + "");
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, st.getCharacter(), "SET", "Health.Health", oldhealth + "", newvalue + "", "Character reset their health");
		return new SayResponse("reset health from " + oldhealth + " to " + newvalue, st.getCharacter().getName());
	}

	@Command.Commands(context = Command.Context.CHARACTER, description = "Heal a target player")
	public static Response healRoll(State st,
	                                @Argument.Arguments(description = "Target to heal", type = Argument.ArgumentType.CHARACTER_NEAR)
			                                Char target,
	                                @Argument.Arguments(description = "Number of dice to roll", mandatory = false, type = Argument.ArgumentType.INTEGER)
			                                Integer dice,
	                                @Argument.Arguments(description = "Sides on dice", mandatory = false, type = Argument.ArgumentType.INTEGER)
			                                Integer sides,
	                                @Argument.Arguments(description = "Bias to roll", mandatory = false, type = Argument.ArgumentType.INTEGER)
			                                Integer bias,
	                                @Argument.Arguments(description = "Reason for heal", type = Argument.ArgumentType.TEXT_ONELINE, mandatory = false, max = 512)
			                                String reason
	) {
		if (target == null) { throw new UserException("No target selected"); }
		if (dice == null) { dice = st.getKV("roller.defaultcount").intValue(); }
		if (sides == null) { sides = st.getKV("roller.defaultsides").intValue(); }
		if (bias == null) { bias = 0; }
		if (reason == null) { reason = "No Reason"; }

		int total = 0;
		String allrolls = "";
		List<Integer> rolls = Roller.roll(st, dice, sides);
		for (int num : rolls) {
			if (!"".equals(allrolls)) { allrolls += ", "; }
			total = total + num;
			allrolls = allrolls + num;
		}
		total = total + bias;


		String event = "rolled to heal " + target.getName() + ", using " + dice + "d" + sides;
		if (bias != 0) { event += "(+bias " + bias + ")"; }
		event += ", totalling " + total + " (" + allrolls + (bias == 0 ? "" : "+" + bias) + "), ";
		event += "for " + reason;
		Damage.heal(st, target, total, reason);
		event += " - Health now " + st.getRawKV(target, "Health.Health");
		SayResponse say = new SayResponse(event, st.getCharacter().getName());
		return say;
	}

}
