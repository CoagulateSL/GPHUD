package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.Core.Database.TooMuchDataException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.Data.Audit.OPERATOR;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class GroupCommands {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Join an open group")
	public static Response join(@Nonnull final State st,
	                            @Arguments(name="group",description="Name of group to join",
	                                       type=ArgumentType.CHARACTERGROUP) @Nonnull final CharacterGroup group) {

		// is group open?
		if (!group.isOpen()) {
			return new ErrorResponse("Can not join group "+group.getNameSafe()+", it is not open, you will need to be invited instead.");
		}
		// does the group have a type
		if (group.getType()!=null && !group.getType().isEmpty()) {
			// it's a typed group, find the attribute it maps to
			final Attribute attribute;
			try { attribute=Attribute.findGroup(st.getInstance(),group.getType()); }
			catch (final TooMuchDataException e) {
				return new ErrorResponse("There is more than one attribute of type GROUP/"+group.getType()+" which will not work as group subtypes enforce unique membership"+"."+"  The administrator should remove one of the attributes.");
			}
			if (!attribute.getSelfModify()) {
				return new ErrorResponse("You can not change your "+attribute.getNameSafe()+" after character creation.");
			}
			// since its a typed group they /can/ self modify, we need to remove them from the old group first
			final CharacterGroup existinggroup=CharacterGroup.getGroup(st,group.getType());
			if (existinggroup!=null) {
				// try remove, in a weird way, note we bypass the permissions check on the target call here
				final Response remove=Management.remove(st,existinggroup,st.getCharacter());
				if (remove instanceof ErrorResponse) {
					return new ErrorResponse("Can't switch group - would require you to leave "+existinggroup.getNameSafe()+" but this failed: "+((ErrorResponse) remove).getMessage(
							st));
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
	@Commands(description="Invite a character to a group",
	          context=Context.CHARACTER)
	public static Response invite(@Nonnull final State st,
	                              @Nonnull @Arguments(type=ArgumentType.CHARACTERGROUP,
	                                                  name="group",description="Group to invite to") final CharacterGroup group,
	                              @Arguments(name="target",description="Character to invite",
	                                         type=ArgumentType.CHARACTER) @Nonnull final Char target) {
		if (!group.isAdmin(st.getCharacter())) {
			return new ErrorResponse("You are not an owner/admin for "+group.getName());
		}
		final JSONObject invite=new JSONObject();
		invite.put("message","factioninvite");
		invite.put("from",st.getCharacter().getId());
		invite.put("to",group.getId());
		Message.queue(target,60*60*48,invite);
		Audit.audit(st,Audit.OPERATOR.CHARACTER,null,target,"Faction Invite",group.getName(),null,null,"Group invite sent");
		return new OKResponse("Invite message sent to "+target.getName()+" for "+group.getName()+", they have 48 hours to accept.");
	}

	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Eject a member from a group")
	public static Response eject(@Nonnull final State st,
	                             @Arguments(type=ArgumentType.CHARACTERGROUP,
	                                        name="group",description="Group to eject from") @Nonnull final CharacterGroup group,
	                             @Arguments(name="member",description="Character to eject from the group",
	                                        type=ArgumentType.CHARACTER_FACTION) @Nonnull final Char member) {
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
		try { group.removeMember(member); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to remove member - "+e.getMessage());
		}
		member.hudMessage("You have been removed from "+group.getName()+" by "+st.getCharacter().getName());
		Audit.audit(st,Audit.OPERATOR.CHARACTER,null,member,"RemoveMember",group.getName(),group.getName(),null,"Removed member from group");
		return new OKResponse(member.getName()+" was removed from "+group.getName());
	}

	@Nonnull
	@Commands(context=Context.CHARACTER,
	          description="Leave a group")
	public static Response leave(@Nonnull final State st,
	                             @Arguments(type=ArgumentType.CHARACTERGROUP,
	                                        name="group",description="Group to leave") @Nonnull final CharacterGroup group) {
		// character must be in group
		if (!group.hasMember(st.getCharacter())) { return new ErrorResponse("You are not a member of group "+group.getName()); }
		// group must be open
		if (!group.isOpen()) { return new ErrorResponse("You can not leave a non-open group"); }
		// is it an attribute group
		if (!group.getTypeNotNull().isEmpty()) {
			final Attribute attr=Attribute.findGroup(st.getInstance(),group.getTypeNotNull());
			if (!attr.getSelfModify()) {
				return new ErrorResponse("You may not modify the group type "+group.getTypeNotNull()+", it is not self modifiable");
			}
		}
		// is open and not an attribute and is in group so leave...
		try { group.removeMember(st.getCharacter()); }
		catch (@Nonnull final UserException e) { return new ErrorResponse("Failed to leave group - "+e.getMessage()); }
		Audit.audit(st,OPERATOR.CHARACTER,null,st.getCharacter(),"LeaveGroup",group.getName(),group.getName(),null,"Character left group");
		return new OKResponse("You have left group "+group.getName());
	}
}
