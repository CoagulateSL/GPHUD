package net.coagulate.GPHUD.Modules.Health;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.SayResponse;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Roller.Roller;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Iain Price
 */
public class ResetHealth {

	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(context=Command.Context.CHARACTER,
	                  description="Reset the character's health to the initial value",
	                  permitExternal=false)
	public static Response resetHealth(@Nonnull final State st) {
		if (!st.getKV("health.allowreset").boolValue()) {
			throw new UserAccessDeniedException("Resetting of your health is not permitted.", true);
		}
		final int oldhealth = st.getKV("health.health").intValue();
		final int newvalue = st.getKV("health.initialhealth").intValue();
		st.setKV(st.getCharacter(), "health.health", String.valueOf(newvalue));
		Audit.audit(true, st, Audit.OPERATOR.CHARACTER, null, st.getCharacter(), "SET", "Health.Health", String.valueOf(oldhealth), String.valueOf(newvalue), "Character reset their health");
		return new SayResponse("reset health from " + oldhealth + " to " + newvalue, st.getCharacter().getName());
	}

	@Nonnull
	@Command.Commands(context=Command.Context.CHARACTER,
	                  description="Have a chance to heal a target player",
	                  permitExternal=false)
	public static Response healRollChance(@Nonnull final State st,
	                                      @Nonnull @Argument.Arguments(name="target",description="Target to heal",
	                                                                   type=Argument.ArgumentType.CHARACTER_NEAR) final Char target,
	                                      @Argument.Arguments(name="chancedice",description="Number of dice to roll for chance to heal",
	                                                          type=Argument.ArgumentType.INTEGER) final Integer chancedice,
	                                      @Argument.Arguments(name="chancesides",description="Sides on dice to roll for change to heal",
	                                                          type=Argument.ArgumentType.INTEGER) final Integer chancesides,
	                                      @Argument.Arguments(name="chancebias",description="Bias to add to ammount to chance to heal",
	                                                          type=Argument.ArgumentType.INTEGER) final Integer chancebias,
	                                      @Argument.Arguments(name="chancethreshold",description="Score to beat to succeed at healing",
	                                                          type=Argument.ArgumentType.INTEGER) final Integer chancethreshold,
	                                      @Argument.Arguments(name="healdice",description="Number of dice to roll for ammount healed",
	                                                          mandatory=false,
	                                                          type=Argument.ArgumentType.INTEGER) final Integer healdice,
	                                      @Argument.Arguments(name="healsides",description="Sides on dice to roll for ammount healed",
	                                                          mandatory=false,
	                                                          type=Argument.ArgumentType.INTEGER) final Integer healsides,
	                                      @Argument.Arguments(name="healbias",description="Bias to add to ammount to heal roll",
	                                                          mandatory=false,
	                                                          type=Argument.ArgumentType.INTEGER) final Integer healbias,
	                                      @Argument.Arguments(name="reason",description="Reason for heal",
	                                                          type=Argument.ArgumentType.TEXT_ONELINE,
	                                                          mandatory=false,
	                                                          max=512) final String reason) {
		final List<Integer> chancerolls=Roller.roll(st,st.getCharacter(),chancedice,chancesides);
		int rollssum=chancebias;
		for (final int roll: chancerolls) { rollssum+=roll; }
		final StringBuilder chancerollsreport=new StringBuilder();
		boolean first=true;
		for (final int roll: chancerolls) {
			if (!first) { chancerollsreport.append("+"); }
			chancerollsreport.append(roll);
			first=false;
		}
		if (chancebias>0) {
			chancerollsreport.append(" + ").append(chancebias);
		}
		chancerollsreport.append("=").append(rollssum);
		// threshold met?
		if (rollssum<chancethreshold) {
			String failmsg="Attempted to heal "+target.getName()+" but failed! (";
			failmsg+=chancerollsreport+"<"+chancethreshold+")";
			Audit.audit(false,st,Audit.OPERATOR.CHARACTER,null,target,"HealChance","Result","","FAIL",chancerollsreport.toString());
			return new SayResponse(failmsg,st.getCharacter().getName());
		}
		chancerollsreport.append(">").append(chancethreshold);
		Audit.audit(false,st,Audit.OPERATOR.CHARACTER,null,target,"HealChance","Result","","PASS",chancerollsreport.toString());
		return healRoll(st,target,healdice,healsides,healbias,reason);
	}

	@Nonnull
	@Command.Commands(context=Command.Context.CHARACTER,
	                  description="Heal a target player",
	                  permitExternal=false)
	public static Response healRoll(@Nonnull final State st,
	                                @Nullable @Argument.Arguments(name="target",description="Target to heal",
	                                                              type=Argument.ArgumentType.CHARACTER_NEAR) final Char target,
	                                @Nullable @Argument.Arguments(name="dice",description="Number of dice to roll",
	                                                              mandatory=false,
	                                                              type=Argument.ArgumentType.INTEGER) Integer dice,
	                                @Nullable @Argument.Arguments(name="sides",description="Sides on dice",
	                                                              mandatory=false,
	                                                              type=Argument.ArgumentType.INTEGER) Integer sides,
	                                @Nullable @Argument.Arguments(name="bias",description="Bias to roll",
	                                                              mandatory=false,
	                                                              type=Argument.ArgumentType.INTEGER) Integer bias,
	                                @Nullable @Argument.Arguments(name="reason",description="Reason for heal",
	                                                              type=Argument.ArgumentType.TEXT_ONELINE,
	                                                              mandatory=false,
	                                                              max=512) String reason) {
		if (target==null) { throw new UserInputEmptyException("No target selected"); }
		if (dice==null) { dice=st.getKV("roller.defaultcount").intValue(); }
		if (sides==null) { sides=st.getKV("roller.defaultsides").intValue(); }
		if (bias==null) { bias=0; }
		if (reason==null) { reason="No Reason"; }

		int total=0;
		String allrolls="";
		final List<Integer> rolls=Roller.roll(st,dice,sides);
		for (final int num: rolls) {
			if (!"".equals(allrolls)) { allrolls+=", "; }
			total=total+num;
			allrolls=allrolls+num;
		}
		total=total+bias;


		String event="rolled to heal "+target.getName()+", using "+dice+"d"+sides;
		if (bias!=0) { event+="(+bias "+bias+")"; }
		event+=", totalling "+total+" ("+allrolls+(bias==0?"":"+"+bias)+"), ";
		event+="for "+reason;
		Damage.heal(st,target,total,reason);
		event+=" - Health now "+st.getRawKV(target,"Health.Health");
		return new SayResponse(event,st.getCharacter().getName());
	}

}
