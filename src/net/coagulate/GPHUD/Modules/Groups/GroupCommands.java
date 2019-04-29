package net.coagulate.GPHUD.Modules.Groups;

import net.coagulate.Core.Tools.UserException;
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

public class GroupCommands {
    @Commands(description = "Invite a character to a group",context = Context.CHARACTER)
    public static Response invite(State st,
                                  @Arguments(type = ArgumentType.CHARACTERGROUP,description = "Group to invite to")
                                        CharacterGroup group,
                                  @NotNull @Arguments(description = "Character to invite",type = ArgumentType.CHARACTER)
                                          Char target)
    {
        if (!group.isAdmin(st.getCharacter())) { return new ErrorResponse("You are not an owner/admin for "+group.getName()); }
        JSONObject invite=new JSONObject();
        invite.put("message", "factioninvite");
        invite.put("from",st.getCharacter().getId());
        invite.put("to",group.getId());
        target.queueMessage(invite,60*60*48);
        Audit.audit(st, Audit.OPERATOR.CHARACTER, null, target, "Faction Invite", group.getName(), null, null,"Group invite sent");
        return new OKResponse("Invite message sent to "+target.getName()+" for "+group.getName()+", they have 48 hours to accept.");
    }
    @Commands(context = Context.CHARACTER,description = "Eject a member from a group")
    public static Response eject(@NotNull State st,
                                 @NotNull @Arguments(type = ArgumentType.CHARACTERGROUP,description = "Group to eject from")
                                         CharacterGroup group,
                                 @NotNull @Arguments(description = "Character to eject from the group",type = ArgumentType.CHARACTER_FACTION)
                                         Char member) {
        if (!group.isAdmin(st.getCharacter())) {return new ErrorResponse("You are not a lead/admin for "+group.getName()); }
        // refuse if they're not in this group (!)
        if (!group.hasMember(member)) { return new ErrorResponse(member.getName()+" is not in "+group.getName()); }
        // refuse if they're the groupleader
        if (group.getOwner()==member) { return new ErrorResponse("Will not eject "+member.getName()+" from "+group.getName()+", they are the owner."); }
        // refuse if they're an admin.  leader can demote them I hope
        if (group.isAdmin(member)) { return new ErrorResponse("Will not eject "+member.getName()+" from "+group.getName()+", they are an administrator and must be demoted first."); }
        try { group.removeMember(member); }
        catch (UserException e) { return new ErrorResponse("Failed to remove member - "+e.getMessage()); }
        member.hudMessage("You have been removed from "+group.getName()+" by "+st.getCharacter().getName());
        Audit.audit(st, Audit.OPERATOR.CHARACTER, null, member, "RemoveMember", group.getName(), group.getName(), null, "Removed member from group");
        return new OKResponse(member.getName()+" was removed from "+group.getName());
    }

}
