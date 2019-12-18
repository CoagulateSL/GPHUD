package net.coagulate.GPHUD.Modules.Faction;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Groups.GroupCommands;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.State;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * Faction controls for the FactionCommands.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class FactionCommands {

	@Nonnull
	@Template(name = "FACTION", description = "Current faction name")
	public static String getFactionName(@Nonnull final State st, final String key) {
		final CharacterGroup faction = st.getCharacter().getGroup("Faction");
		if (faction == null) { return ""; }
		return faction.getName();
	}

	@Nonnull
	@Commands(description = "Award a point of faction XP to the target character", context = Context.CHARACTER)
	public static Response awardXP(@Nonnull final State st,
	                               @Nonnull @Arguments(description = "Character to award a point of XP to", type = ArgumentType.CHARACTER_FACTION) final
	                               Char target,
	                               @Arguments(description = "Reason for the award", type = ArgumentType.TEXT_ONELINE, max = 512) final
	                                   String reason) {
		// things to check...
		// players are in the same faction
		final CharacterGroup ourfaction = st.getCharacter().getGroup("Faction");
		final CharacterGroup theirfaction = target.getGroup("Faction");
		if (ourfaction==null) { return new ErrorResponse("You are not in a faction"); }
		if (theirfaction==null) { return new ErrorResponse(target.getName()+" is not in a faction"); }
		if (ourfaction != theirfaction) {
			return new ErrorResponse("You must be in the same faction as the person you want to award XP to");
		}
		if (!ourfaction.isAdmin(st.getCharacter())) {
			return new ErrorResponse("You do not have admin permissions in your faction");
		}
		final float period = st.getKV("Faction.XPCycleLength").floatValue();
		final int maxxp = st.getKV("Faction.XPPerCycle").intValue();
		final Pool factionxp = Modules.getPool(st, "Faction.FactionXP");
		final int awarded = target.sumPoolDays(factionxp, period);
		if (awarded >= maxxp) {
			return new ErrorResponse("This character has already reached their Faction XP Limit.  They will next be eligable for a point in " + target.poolNextFree(factionxp, maxxp, period));
		}
		// else award xp :P
		Audit.audit(st, Audit.OPERATOR.CHARACTER, null, target, "Pool Add", "FactionXP", null, "1", reason);
		target.addPool(st, factionxp, 1, reason);
		if (target != st.getCharacter()) {
			target.hudMessage("You were granted 1 point of Faction XP by " + st.getCharacter().getName() + " for " + reason);
		}
		return new OKResponse("Granted a point of Faction XP to " + target.getName());
	}

	@Nonnull
	@Commands(description = "Invite a player to your faction", context = Context.CHARACTER)
	public static Response invite(@Nonnull final State st,
	                              @Nonnull @Arguments(description = "Character to invite", type = ArgumentType.CHARACTER) final
	                              Char target) {
		final CharacterGroup ourfaction = st.getCharacter().getGroup("Faction");
		if (ourfaction == null) { return new ErrorResponse("You are not in a faction"); }
		return GroupCommands.invite(st, ourfaction, target);
	}

	@Nonnull
	@Commands(context = Context.CHARACTER, description = "Eject a member from your faction")
	public static Response eject(final @NotNull State st,
	                             @Arguments(description = "Character to remove from the faction", type = ArgumentType.CHARACTER_FACTION) final @NotNull
	                             Char member) {
		final CharacterGroup faction = st.getCharacter().getGroup("Faction");
		if (faction == null) { return new ErrorResponse("You are not in a faction!"); }
		return GroupCommands.eject(st, faction, member);
	}

}
