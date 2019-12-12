package net.coagulate.GPHUD.Modules.Roller;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.SayResponse;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Health.Damage;
import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.State;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Roller utility class and module metadata.
 *
 * @author iain
 */
public class Roller {

	public static List<Integer> roll(State st, Integer dice, Integer sides) throws UserException, SystemException {
		return roll(st, st.getCharacter(), dice, sides);
	}

	public static List<Integer> roll(State st, Char character, Integer dice, Integer sides) throws UserException, SystemException {
		List<Integer> rolls = new ArrayList<>();
		if (dice == null) { dice = st.getKV("roller.defaultcount").intValue(); }
		if (sides == null) { sides = st.getKV("roller.defaultsides").intValue(); }
		if (dice > 100) { throw new UserException("Too many dice."); }
		for (int i = 0; i < dice; i++) {
			int num = ThreadLocalRandom.current().nextInt(1, sides + 1);
			rolls.add(num);
		}
		return rolls;
	}

	/**
	 * Perform a dice roll only, without bias or summation.
	 *
	 * @param st     Session State
	 * @param dice   Number of dice to roll - defaults to 1
	 * @param sides  Sides on dice to roll - defaults to 100
	 * @param reason Reason to log against the roll - defaults to "No Reason"
	 * @return A response hudMessage containing the roll's outcome.
	 */
	@Commands(context = Context.CHARACTER, description = "Roll a given number of stated sided dice with a bias and reason", permitUserWeb = false)
	public static Response rollOnly(State st,
	                                @Arguments(description = "Number of dice to roll", type = ArgumentType.INTEGER, mandatory = false)
			                                Integer dice,
	                                @Arguments(description = "Number of sides on dice", type = ArgumentType.INTEGER, mandatory = false)
			                                Integer sides,
	                                @Arguments(description = "Logged reason for the roll", type = ArgumentType.TEXT_ONELINE, max = 512)
			                                String reason) throws UserException, SystemException {
		if (dice == null) { dice = st.getKV("roller.defaultcount").intValue(); }
		if (sides == null) { sides = st.getKV("roller.defaultsides").intValue(); }
		if (reason == null) { reason = "No Reason"; }
		if (dice > 100) { throw new UserException("Too many dice."); }
		String event = "rolled " + dice + "d" + sides + " ";
		event += "for " + reason + ", and rolled ";
		String allrolls = "";
		List<Integer> rolls = roll(st, dice, sides);
		for (int num : rolls) {
			if (!"".equals(allrolls)) { allrolls += ", "; }
			allrolls = allrolls + num;
		}

		event += allrolls;
		Audit.audit(st, Audit.OPERATOR.CHARACTER, null, null, "RollOnly", null, null, "", event);
		return new SayResponse(event, st.getCharacter().getName());
	}

	/**
	 * Perform a dice roll.
	 *
	 * @param st     Session State
	 * @param dice   Number of dice to roll - defaults to 1
	 * @param sides  Sides on dice to roll - defaults to 100
	 * @param bias   Bias to add to final roll - defaults to 0
	 * @param reason Reason to log against the roll - defaults to "No Reason"
	 * @return A response hudMessage containing the roll's outcome.
	 */
	@Commands(context = Context.CHARACTER, description = "Roll a given number of stated sided dice with a bias and reason", permitUserWeb = false)
	public static Response roll(State st,
	                            @Arguments(description = "Number of dice to roll", type = ArgumentType.INTEGER, mandatory = false)
			                            Integer dice,
	                            @Arguments(description = "Number of sides on dice", type = ArgumentType.INTEGER, mandatory = false)
			                            Integer sides,
	                            @Arguments(description = "Optional bias to add to result", mandatory = false, type = ArgumentType.INTEGER)
			                            Integer bias,
	                            @Arguments(description = "Logged reason for the roll", type = ArgumentType.TEXT_ONELINE, max = 512)
			                            String reason) throws UserException, SystemException {
		if (dice == null) { dice = st.getKV("roller.defaultcount").intValue(); }
		if (sides == null) { sides = st.getKV("roller.defaultsides").intValue(); }
		if (bias == null) { bias = 0; }
		if (reason == null) { reason = "No Reason"; }
		if (dice > 100) { throw new UserException("Too many dice."); }
		String event = "rolled " + dice + "d" + sides + " ";
		if (bias != 0) { event += "(with bias " + bias + ") "; }
		event += "for " + reason + ", and rolled ";
		int total = 0;
		String allrolls = "";
		List<Integer> rolls = roll(st, dice, sides);
		for (int num : rolls) {
			if (!"".equals(allrolls)) { allrolls += ", "; }
			total = total + num;
			allrolls = allrolls + num;
		}

		if (bias != 0) {
			allrolls = allrolls + "+" + bias;
			total += bias;
		}
		event += total + " (" + allrolls + ")";
		st.roll = total;
		Audit.audit(st, Audit.OPERATOR.CHARACTER, null, null, "Roll", null, null, "" + total, event);
		return new SayResponse(event, st.getCharacter().getName());
	}

	@Commands(context = Context.CHARACTER, description = "Default roll, only requests a reason", permitUserWeb = false)
	public static Response rollDefault(State st,
	                                   @Arguments(description = "Logged reason for the roll", type = ArgumentType.TEXT_ONELINE, max = 512)
			                                   String reason) throws UserException, SystemException {
		return roll(st, null, null, 0, reason);
	}

	@Templater.Template(name = "ROLL", description = "Any made roll")
	public static String getRoll(State st, String key) {
		if (st.roll == null) { throw new UserException("No roll made!"); }
		return st.roll.toString();
	}

	@Templater.Template(name = "TARGET:ROLL", description = "Any TARGET made roll")
	public static String getTargetRoll(State st, String key) {
		if (st.getTarget() == null) { throw new UserException("No target!"); }
		State target = st.getTarget();
		if (target.roll == null) { throw new UserException("Target not rolled!"); }
		return target.roll.toString();
	}


	@Commands(context = Context.CHARACTER, description = "Roll against another player", permitUserWeb = false)
	public static Response rollAgainst(State st,
	                                   @Arguments(description = "Character to roll against", type = ArgumentType.CHARACTER_NEAR)
			                                   Char target,
	                                   @Arguments(description = "Number of dice to roll", type = ArgumentType.INTEGER, mandatory = false)
			                                   Integer dice,
	                                   @Arguments(description = "Number of sides on dice", type = ArgumentType.INTEGER, mandatory = false)
			                                   Integer sides,
	                                   @Arguments(description = "Optional bias to add to result", mandatory = false, type = ArgumentType.INTEGER)
			                                   Integer bias,
	                                   @Arguments(description = "Number of dice to roll", type = ArgumentType.INTEGER, mandatory = false)
			                                   Integer targetdice,
	                                   @Arguments(description = "Number of sides on dice", type = ArgumentType.INTEGER, mandatory = false)
			                                   Integer targetsides,
	                                   @Arguments(description = "Optional bias to add to result", mandatory = false, type = ArgumentType.INTEGER)
			                                   Integer targetbias,
	                                   @Arguments(description = "Logged reason for the roll", type = ArgumentType.TEXT_ONELINE, max = 512)
			                                   String reason) throws UserException, SystemException {
		if (target == null) { throw new UserException("No target selected"); }
		if (dice == null) { dice = st.getKV("roller.defaultcount").intValue(); }
		if (sides == null) { sides = st.getKV("roller.defaultsides").intValue(); }
		if (bias == null) { bias = 0; }
		if (reason == null) { reason = "No Reason"; }
		if (targetdice == null) { targetdice = dice; }
		if (targetsides == null) { targetsides = sides; }
		if (targetbias == null) { targetbias = bias; }
		if (dice > 100) { throw new UserException("Too many dice."); }
		if (targetdice > 100) { throw new UserException("Too many dice for target."); }
		st.setTarget(target);

		int total = 0;
		String allrolls = "";
		int targettotal = 0;
		String targetallrolls = "";
		int attempts = 100;
		while (total == targettotal && attempts > 0) {
			total = 0;
			allrolls = "";
			targettotal = 0;
			targetallrolls = "";
			List<Integer> rolls = roll(st, dice, sides);
			for (int num : rolls) {
				if (!"".equals(allrolls)) { allrolls += ", "; }
				total = total + num;
				allrolls = allrolls + num;
			}
			total = total + bias;
			List<Integer> targetrolls = roll(st.getTarget(), targetdice, targetsides);
			for (int num : targetrolls) {
				if (!"".equals(targetallrolls)) { targetallrolls += ", "; }
				targettotal = targettotal + num;
				targetallrolls = targetallrolls + num;
			}
			targettotal = targettotal + targetbias;
		}
		if (attempts < 2) { throw new UserException("Unable to resolve a non draw result in 100 rolls"); }


		String event = "rolled against " + target.getName() + ", using " + dice + "d" + sides;
		if (bias != 0) { event += "(+bias " + bias + ")"; }
		event += ", scoring " + total + " (" + allrolls + (bias == 0 ? "" : "+" + bias) + "), versus ";
		event += targetdice + "d" + targetsides;
		if (targetbias != 0) { event += "(+bias " + targetbias + ")"; }
		event += ", scoring " + targettotal + " (" + targetallrolls + (targetbias == 0 ? "" : "+" + targetbias) + ") ";
		event += "for " + reason;
		if (total > targettotal) {
			event += ", and SUCCEEDED!  ";
		} else {
			event += ", and FAILED!  ";
		}
		event += "(" + total + "v" + targettotal + ")";
		st.roll = total;
		st.getTarget().roll = targettotal;
		Audit.audit(st, Audit.OPERATOR.CHARACTER, st.getTarget().avatar(), st.getTarget().getCharacter(), "Roll", null, null, "" + total, event);
		return new SayResponse(event, st.getCharacter().getName());
	}


	@Commands(context = Context.CHARACTER, description = "Roll against another player", permitUserWeb = false)
	public static Response rollDamageAgainst(State st,
	                                         @Arguments(description = "Character to roll against", type = ArgumentType.CHARACTER_NEAR)
			                                         Char target,
	                                         @Arguments(description = "Number of dice to roll", type = ArgumentType.INTEGER, mandatory = false)
			                                         Integer dice,
	                                         @Arguments(description = "Number of sides on dice", type = ArgumentType.INTEGER, mandatory = false)
			                                         Integer sides,
	                                         @Arguments(description = "Optional bias to add to result", mandatory = false, type = ArgumentType.INTEGER)
			                                         Integer bias,
	                                         @Arguments(description = "Number of dice to roll", type = ArgumentType.INTEGER, mandatory = false)
			                                         Integer targetdice,
	                                         @Arguments(description = "Number of sides on dice", type = ArgumentType.INTEGER, mandatory = false)
			                                         Integer targetsides,
	                                         @Arguments(description = "Optional bias to add to result", mandatory = false, type = ArgumentType.INTEGER)
			                                         Integer targetbias,
	                                         @Arguments(description = "Damage to apply to target if they lose", mandatory = false, type = ArgumentType.TEXT_ONELINE, delayTemplating = true, max = 1024)
			                                         String damage,
	                                         @Arguments(description = "Logged reason for the roll", type = ArgumentType.TEXT_ONELINE, max = 512)
			                                         String reason) throws UserException, SystemException {
		//System.out.println("DAMAGE RECEIVED AS "+damage);
		if (!st.hasModule("Health")) { throw new UserException("Health module is required to use rollDamageAgainst"); }
		if (damage == null) { damage = "1"; }
		Response response = rollAgainst(st, target, dice, sides, bias, targetdice, targetsides, targetbias, reason);
		if (st.roll > st.getTarget().roll) {
			damage = damage.replaceAll("==", "--");
			String output = Templater.template(st, damage, true, true);
			Damage.apply(st, target, Integer.parseInt(output), reason);
			SayResponse say = (SayResponse) response;
			say.setText(say.getText() + " - Health now " + st.getRawKV(target, "Health.Health"));
		}
		return response;
	}

}
