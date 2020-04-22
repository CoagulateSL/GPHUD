package net.coagulate.GPHUD.Modules.Roller;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Roller utility class and module metadata.
 *
 * @author iain
 */
public class Roller {

	// ---------- STATICS ----------
	@Nonnull
	public static int rollSummed(
			@Nonnull
			final State st,
			final Integer dice,
			final Integer sides) {
		int total=0;
		for (Integer i: roll(st,dice,sides)) { total=total+i; }
		return total;
	}

	@Nonnull
	public static List<Integer> roll(
			@Nonnull
			final State st,
			final Integer dice,
			final Integer sides) {
		return roll(st,st.getCharacter(),dice,sides);
	}

	@Nonnull
	public static List<Integer> roll(
			@Nonnull
			final State st,
			final Char character,
			@Nullable
					Integer dice,
			@Nullable
					Integer sides) {
		final List<Integer> rolls=new ArrayList<>();
		if (dice==null) { dice=st.getKV("roller.defaultcount").intValue(); }
		if (sides==null) { sides=st.getKV("roller.defaultsides").intValue(); }
		if (dice>100) { throw new UserConfigurationException("Too many dice."); }
		if (sides<1) { throw new UserConfigurationException("Number of sides must be at least 1"); }
		for (int i=0;i<dice;i++) {
			final int num=ThreadLocalRandom.current().nextInt(1,sides+1);
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
	 *
	 * @return A response hudMessage containing the roll's outcome.
	 */
	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Roll a given number of stated sided dice with a bias and reason",
	          permitUserWeb=false,
	          permitExternal=false)
	public static Response rollOnly(
			@Nonnull
			final State st,
			@Nullable
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer dice,
			@Nullable
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer sides,
			@Nullable
			@Arguments(description="Logged reason for the roll",
			           type=ArgumentType.TEXT_ONELINE,
			           max=512)
					String reason) {
		if (dice==null) { dice=st.getKV("roller.defaultcount").intValue(); }
		if (sides==null) { sides=st.getKV("roller.defaultsides").intValue(); }
		if (reason==null) { reason="No Reason"; }
		if (dice>100) { throw new UserConfigurationException("Too many dice."); }
		String event="rolled "+dice+"d"+sides+" ";
		event+="for "+reason+", and rolled ";
		String allrolls="";
		final List<Integer> rolls=roll(st,dice,sides);
		for (final int num: rolls) {
			if (!"".equals(allrolls)) { allrolls+=", "; }
			allrolls=allrolls+num;
		}

		event+=allrolls;
		Audit.audit(st,Audit.OPERATOR.CHARACTER,null,null,"RollOnly",null,null,"",event);
		return new SayResponse(event,st.getCharacter().getName());
	}

	/**
	 * Perform a dice roll.
	 *
	 * @param st     Session State
	 * @param dice   Number of dice to roll - defaults to 1
	 * @param sides  Sides on dice to roll - defaults to 100
	 * @param bias   Bias to add to final roll - defaults to 0
	 * @param reason Reason to log against the roll - defaults to "No Reason"
	 *
	 * @return A response hudMessage containing the roll's outcome.
	 */
	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Roll a given number of stated sided dice with a bias and reason",
	          permitUserWeb=false,
	          permitExternal=false)
	public static Response roll(
			@Nonnull
			final State st,
			@Nullable
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer dice,
			@Nullable
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer sides,
			@Nullable
			@Arguments(description="Optional bias to add to result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
					Integer bias,
			@Nullable
			@Arguments(description="Logged reason for the roll",
			           type=ArgumentType.TEXT_ONELINE,
			           max=512)
					String reason) {
		if (dice==null) { dice=st.getKV("roller.defaultcount").intValue(); }
		if (sides==null) { sides=st.getKV("roller.defaultsides").intValue(); }
		if (bias==null) { bias=0; }
		if (reason==null) { reason="No Reason"; }
		if (dice>100) { throw new UserConfigurationException("Too many dice."); }
		String event="rolled "+dice+"d"+sides+" ";
		if (bias!=0) { event+="(with bias "+bias+") "; }
		event+="for "+reason+", and rolled ";
		int total=0;
		String allrolls="";
		final List<Integer> rolls=roll(st,dice,sides);
		for (final int num: rolls) {
			if (!"".equals(allrolls)) { allrolls+=", "; }
			total=total+num;
			allrolls=allrolls+num;
		}

		if (bias!=0) {
			allrolls=allrolls+"+"+bias;
			total+=bias;
		}
		event+=total+" ("+allrolls+")";
		st.roll=total;
		Audit.audit(st,Audit.OPERATOR.CHARACTER,null,null,"Roll",null,null,""+total,event);
		return new SayResponse(event,st.getCharacter().getName());
	}

	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Default roll, only requests a reason",
	          permitUserWeb=false,
	          permitExternal=false)
	public static Response rollDefault(
			@Nonnull
			final State st,
			@Arguments(description="Logged reason for the roll",
			           type=ArgumentType.TEXT_ONELINE,
			           max=512)
			final String reason) {
		return roll(st,null,null,0,reason);
	}

	@Nonnull
	@Templater.Template(name="ROLL",
	                    description="Any made roll")
	public static String getRoll(
			@Nonnull
			final State st,
			final String key) {
		if (st.roll==null) { throw new UserInputStateException("No roll made!"); }
		return st.roll.toString();
	}

	@Nonnull
	@Templater.Template(name="TARGET:ROLL",
	                    description="Any TARGET made roll")
	public static String getTargetRoll(
			@Nonnull
			final State st,
			final String key) {
		if (!st.hasModule("Roller")) { return ""; }
		if (st.getTargetNullable()==null) { throw new UserInputStateException("No target!"); }
		final State target=st.getTargetNullable();
		if (target.roll==null) { throw new UserInputStateException("Target not rolled!"); }
		return target.roll.toString();
	}


	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Roll against another player",
	          permitUserWeb=false,
	          permitExternal=false)
	public static Response rollAgainst(
			@Nonnull
			final State st,
			@Nullable
			@Arguments(description="Character to roll against",
			           type=ArgumentType.CHARACTER_NEAR)
			final Char target,
			@Nullable
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer dice,
			@Nullable
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer sides,
			@Nullable
			@Arguments(description="Optional bias to add to result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
					Integer bias,
			@Nullable
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer targetdice,
			@Nullable
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
					Integer targetsides,
			@Nullable
			@Arguments(description="Optional bias to add to result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
					Integer targetbias,
			@Nullable
			@Arguments(description="Logged reason for the roll",
			           type=ArgumentType.TEXT_ONELINE,
			           max=512)
					String reason) {
		if (target==null) { throw new UserInputStateException("No target selected"); }
		if (dice==null) { dice=st.getKV("roller.defaultcount").intValue(); }
		if (sides==null) { sides=st.getKV("roller.defaultsides").intValue(); }
		if (bias==null) { bias=0; }
		if (reason==null) { reason="No Reason"; }
		if (targetdice==null) { targetdice=dice; }
		if (targetsides==null) { targetsides=sides; }
		if (targetbias==null) { targetbias=bias; }
		if (dice>100) { throw new UserConfigurationException("Too many dice."); }
		if (targetdice>100) { throw new UserConfigurationException("Too many dice for target."); }
		st.setTarget(target);

		int total=0;
		String allrolls="";
		int targettotal=0;
		String targetallrolls="";
		int attempts=100;
		while (total==targettotal && attempts>0) {
			total=0;
			allrolls="";
			targettotal=0;
			targetallrolls="";
			attempts--;
			final List<Integer> rolls=roll(st,dice,sides);
			for (final int num: rolls) {
				if (!"".equals(allrolls)) { allrolls+=", "; }
				total=total+num;
				allrolls=allrolls+num;
			}
			total=total+bias;
			final List<Integer> targetrolls=roll(st.getTarget(),targetdice,targetsides);
			for (final int num: targetrolls) {
				if (!"".equals(targetallrolls)) { targetallrolls+=", "; }
				targettotal=targettotal+num;
				targetallrolls=targetallrolls+num;
			}
			targettotal=targettotal+targetbias;
		}
		if (attempts<2) { throw new UserConfigurationException("Unable to resolve a non draw result in 100 rolls"); }


		String event="rolled against "+target.getName()+", using "+dice+"d"+sides;
		if (bias!=0) { event+="(+bias "+bias+")"; }
		event+=", scoring "+total+" ("+allrolls+(bias==0?"":"+"+bias)+"), versus ";
		event+=targetdice+"d"+targetsides;
		if (targetbias!=0) { event+="(+bias "+targetbias+")"; }
		event+=", scoring "+targettotal+" ("+targetallrolls+(targetbias==0?"":"+"+targetbias)+") ";
		event+="for "+reason;
		if (total>targettotal) {
			event+=", and SUCCEEDED!  ";
		}
		else {
			event+=", and FAILED!  ";
		}
		event+="("+total+"v"+targettotal+")";
		st.roll=total;
		st.getTarget().roll=targettotal;
		Audit.audit(st,Audit.OPERATOR.CHARACTER,st.getTarget().getAvatarNullable(),st.getTarget().getCharacter(),"Roll",null,null,""+total,event);
		return new SayResponse(event,st.getCharacter().getName());
	}


	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Roll against another player",
	          permitUserWeb=false,
	          permitExternal=false)
	public static Response rollDamageAgainst(
			@Nonnull
			final State st,
			@Nonnull
			@Arguments(description="Character to roll against",
			           type=ArgumentType.CHARACTER_NEAR)
			final Char target,
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer dice,
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer sides,
			@Arguments(description="Optional bias to add to result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
			final Integer bias,
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer targetdice,
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer targetsides,
			@Arguments(description="Optional bias to add to result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
			final Integer targetbias,
			@Nullable
			@Arguments(description="Damage to apply to target if they lose",
			           mandatory=false,
			           type=ArgumentType.TEXT_ONELINE,
			           delayTemplating=true,
			           max=1024)
					String damage,
			@Arguments(description="Logged reason for the roll",
			           type=ArgumentType.TEXT_ONELINE,
			           max=512)
			final String reason) {
		//System.out.println("DAMAGE RECEIVED AS "+damage);
		if (!st.hasModule("Health")) {
			throw new UserConfigurationException("Health module is required to use rollDamageAgainst");
		}
		if (damage==null) { damage="1"; }
		final Response response=rollAgainst(st,target,dice,sides,bias,targetdice,targetsides,targetbias,reason);
		if (st.roll==null || st.getTarget().roll==null) {
			throw new SystemImplementationException("Wierdness with null roll results");
		}
		if (st.roll>st.getTarget().roll) {
			damage=damage.replaceAll("==","--");
			final String output=Templater.template(st,damage,true,true);
			if (output==null) { throw new NullPointerException("output from damage template was null?"); }
			Damage.apply(st,target,Integer.parseInt(output),reason);
			final SayResponse say=(SayResponse) response;
			say.setText(say.getText()+" - Health now "+st.getRawKV(target,"Health.Health"));
		}
		return response;
	}

	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Roll against another player then roll for damage",
	          permitUserWeb=false,
	          permitExternal=false)
	public static Response rollRolledDamageAgainst(
			@Nonnull
			final State st,
			@Nonnull
			@Arguments(description="Character to roll against",
			           type=ArgumentType.CHARACTER_NEAR)
			final Char target,
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer dice,
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer sides,
			@Arguments(description="Optional bias to add to result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
			final Integer bias,
			@Arguments(description="Number of dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer targetdice,
			@Arguments(description="Number of sides on dice",
			           type=ArgumentType.INTEGER,
			           mandatory=false)
			final Integer targetsides,
			@Arguments(description="Optional bias to add to result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
			final Integer targetbias,
			@Arguments(description="Number of damage dice to roll",
			           type=ArgumentType.INTEGER,
			           mandatory=true)
			final Integer damagedice,
			@Arguments(description="Number of sides on damage dice",
			           type=ArgumentType.INTEGER,
			           mandatory=true)
			final Integer damagesides,
			@Arguments(description="Optional bias to add to damage result",
			           mandatory=false,
			           type=ArgumentType.INTEGER)
					Integer damagebias,
			@Arguments(description="Logged reason for the roll",
			           type=ArgumentType.TEXT_ONELINE,
			           max=512)
			final String reason) {
		if (!st.hasModule("Health")) {
			throw new UserConfigurationException("Health module is required to use rollDamageAgainst");
		}
		final Response response=rollAgainst(st,target,dice,sides,bias,targetdice,targetsides,targetbias,reason);
		if (st.roll==null || st.getTarget().roll==null) {
			throw new SystemImplementationException("Wierdness with null roll results");
		}
		if (st.roll>st.getTarget().roll) {
			if (damagebias==null) { damagebias=0; }
			int damage=rollSummed(st,damagedice,damagesides);
			damage=damage+damagebias;
			Damage.apply(st,target,damage,reason);
			final SayResponse say=(SayResponse) response;
			say.setText(say.getText()+" damaging for "+damage+" - Health now "+st.getRawKV(target,"Health.Health"));
		}
		return response;
	}

}
