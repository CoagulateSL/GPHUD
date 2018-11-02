package net.coagulate.GPHUD.Modules.GPHUDClient;

import java.util.ArrayList;
import java.util.List;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Message;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import org.json.JSONObject;

/** Read your messages.
 *
 * @author Iain Price <gphud@predestined.net
 */
public abstract class GetMessages {
    
    @Commands(context = Context.CHARACTER,description = "Get a message",permitConsole = false,permitHUDWeb = false,permitUserWeb = false)
    public static Response getMessage(State st) throws SystemException, UserException {
        Message m=st.getCharacter().getMessage();
        if (m==null) { return new ErrorResponse("You have no outstanding messages."); }
        m.setActive();
        
        JSONObject j=new JSONObject(m.getJSON());
        String message=j.optString("message",""); 
        if (message.equalsIgnoreCase("factioninvite")) { return processFactionInvite(st,j); }
        throw new SystemException("Unable to find a message parser in GPHUDClient for message type '"+message+"'");
    }

    private static Response processFactionInvite(State st, JSONObject j) throws UserException, SystemException {
        Char from=Char.get(j.getInt("from"));
        CharacterGroup faction=CharacterGroup.get(j.getInt("to"));
        JSONObject template=Modules.getJSONTemplate(st, "gphudclient.acceptrejectmessage");
        template.put("arg0description","You have been invited to join faction "+faction.getName()+" by "+from.getName());
        return new JSONResponse(template);
    }
      public static List<String> getAcceptReject(State st) throws UserException {
        List<String> options=new ArrayList<>();
        options.add("Accept");
        options.add("Reject");
        return options;
    }

    @Commands(context=Context.CHARACTER,description="Accept/Reject a message",permitConsole = false, permitHUDWeb = false,permitUserWeb = false)
    public static Response acceptRejectMessage(State st,
            @Arguments(type = ArgumentType.CHOICE,description = "Accept or Reject the message",choiceMethod = "getAcceptReject")
                String response) throws SystemException, UserException
    {
        Message m=st.getCharacter().getActiveMessage();
        if (m==null) { return new ErrorResponse("You have no active message."); }
        
        JSONObject j=new JSONObject(m.getJSON());
        String message=j.optString("message",""); 
        if (message.equalsIgnoreCase("factioninvite")) { return processFactionInviteResponse(st,m,j,response); }
        throw new SystemException("Unable to find a message RESPONSE parser in GPHUDClient for message type '"+message+"'");
    }

    private static Response processFactionInviteResponse(State st, Message m,JSONObject j, String response) throws UserException {
        boolean accepted;
        if (response.equalsIgnoreCase("Accept")) { accepted=true; } else
        {
            if (response.equalsIgnoreCase("Reject")) { accepted=false; } else { throw new UserException("Expected Accept or Reject response"); }
        }
        CharacterGroup targetfaction=CharacterGroup.get(j.getInt("to"));
        m.delete();
        st.getCharacter().pushMessageCount();
        if (!accepted) {
            Audit.audit(st, Audit.OPERATOR.CHARACTER, null, null, Char.get(j.getInt("from")), "Invite Declined", targetfaction.getName(), null, null, "Declined invite to faction");
            return new OKResponse("Invitation rejected");
        }
        CharacterGroup currentfaction=st.getCharacter().getGroup("Faction");
        if (currentfaction==targetfaction) { 
            Audit.audit(st, Audit.OPERATOR.CHARACTER, null, null, Char.get(j.getInt("from")), "Invite Invalid", targetfaction.getName(), null, null, "Invite to faction character is already in");
            return new OKResponse("Invitation invalid, you are already in this faction");
        }
        if (currentfaction!=null && currentfaction.getOwner()==st.getCharacter()) {
            Audit.audit(st, Audit.OPERATOR.CHARACTER, null, null, Char.get(j.getInt("from")), "Invite Invalid", targetfaction.getName(), null, null, "Invite to faction but leader of current faction");
            return new OKResponse("Invitation invalid, you are leader of "+currentfaction.getName());
        }
        if (currentfaction!=null) { 
            try { currentfaction.removeMember(st.getCharacter()); }
            catch (UserException e) {
                Audit.audit(st, Audit.OPERATOR.CHARACTER, null, null, Char.get(j.getInt("from")), "Invite Invalid", targetfaction.getName(), null, null, "Invite to faction failed, leaving old faction errored - "+e.getMessage());
                return new ErrorResponse("Unable to leave existing faction - "+e.getMessage());
            }
        }
        try { targetfaction.addMember(st.getCharacter()); }
        catch (UserException e) {
            return new ErrorResponse("Unable to join new faction - "+e.getMessage());
        }
        Audit.audit(st, Audit.OPERATOR.CHARACTER, null, null, Char.get(j.getInt("from")), "Invite Accepted", targetfaction.getName(), null, null, "Accepted faction invite");
        return new OKResponse("Faction invite accepted");
    }

}
