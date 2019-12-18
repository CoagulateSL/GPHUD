package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Attribute;
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
import net.coagulate.GPHUD.State;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class GroupCommands {

	@Nonnull
	@Commands(context=Context.CHARACTER, description="Join an open group")
	public static Response join(final @NotNull State st,
	                            @Arguments(description="Name of group to join", type=ArgumentType.CHARACTERGROUP) final @NotNull CharacterGroup group)
	{

		// is group open?
		if (!group.isOpen()) {
			return new ErrorResponse("Can not join group "+group.getNameSafe()+", it is not open, you will need to be invited instead.");
		}
		// does the group have a type
		if (group.getType()!=null && !group.getType().isEmpty()) {
			// it's a typed group, find the attribute it maps to
			final Attribute attribute=Attribute.findGroup(st.getInstance(),group.getType());
			if (!attribute.getSelfModify()) {
				return new ErrorResponse("You can not change your "+attribute.getNameSafe()+" after character creation.");
			}
			// since its a typed group they /can/ self modify, we need to remove them from the old group first
			final CharacterGroup existinggroup=st.getCharacter().getGroup(group.getType());
			if (existinggroup!=null) {
				// try remove, in a weird way, note we bypass the permissions check on the target call here
				final Response remove=Management.remove(st,existinggroup,st.getCharacter());
				if (remove instanceof ErrorResponse) {
					return new ErrorResponse("Can't switch group - would require you to leave "+existinggroup.getNameSafe()+" but this failed: "+((ErrorResponse) remove)
							.getMessage(st));
				}
				// ok, we removed them from the old group
			}
		}
		// well then, add tim
		final Response add=Management.add(st,group,st.getCharacter());
		if (add instanceof ErrorResponse) {
			return new ErrorResponse("Failed to add to new group: "+((ErrorResponse) add).getMessage(st));
		}
		return new OKResponse("You are now a member of "+group.getNameSafe());
	}

	@Nonnull
	@Commands(description="Invite a character to a group", context=Context.CHARACTER)
	public static Response invite(@Nonnull final State st,
	                              @Nonnull @Arguments(type=ArgumentType.CHARACTERGROUP, description="Group to invite to") final CharacterGroup group,
	                              @Arguments(description="Character to invite", type=ArgumentType.CHARACTER) final @NotNull Char target)
	{
		if (!group.isAdmin(st.getCharacter())) {
			return new ErrorResponse("You are not an owner/admin for "+group.getName());
		}
		final JSONObject invite=new JSONObject();
		invite.put("message","factioninvite");
		invite.put("from",st.getCharacter().getId());
		invite.put("to",group.getId());
		target.queueMessage(invite,60*60*48);
		Audit.audit(st,
		            Audit.OPERATOR.CHARACTER,
		            null,
		            target,
		            "Faction Invite",
		            group.getName(),
		            null,
		            null,
		            "Group invite sent"
		           );
		return new OKResponse("Invite message sent to "+target.getName()+" for "+group.getName()+", they have 48 hours to accept.");
	}

	@Nonnull
	@Commands(context=Context.CHARACTER, description="Eject a member from a group")
	public static Response eject(final @NotNull State st,
	                             @Arguments(type=ArgumentType.CHARACTERGROUP, description="Group to eject from") final @NotNull CharacterGroup group,
	                             @Arguments(description="Character to eject from the group", type=ArgumentType.CHARACTER_FACTION) final @NotNull Char member)
	{
		if (!group.isAdmin(st.getCharacter())) {
			return new ErrorResponse("You are not a lead/admin for "+group.getName());
		}
		// refuse if they're not in this group (!)
		if (!group.hasMember(member)) { return new ErrorResponse(member.getName()+" is not in "+group.getName()); }
		// refuse if they're the groupleader
		if (group.getOwner()==member) {
			return new ErrorResponse("Will not eject "+member.getName()+" from "+group.getName()+", they are the owner.");
		}
		// refuse if they're an admin.  leader can demote them I hope
		if (group.isAdmin(member)) {
			return new ErrorResponse("Will not eject "+member.getName()+" from "+group.getName()+", they are an administrator and must be demoted first.");
		}
		try { group.removeMember(member); } catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to remove member - "+e.getMessage());
		}
		member.hudMessage("You have been removed from "+group.getName()+" by "+st.getCharacter().getName());
		Audit.audit(st,
		            Audit.OPERATOR.CHARACTER,
		            null,
		            member,
		            "RemoveMember",
		            group.getName(),
		            group.getName(),
		            null,
		            "Removed member from group"
		           );
		return new OKResponse(member.getName()+" was removed from "+group.getName());
	}

}
