package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Message;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.TextError;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/** Deal with messages via a web interface.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Messages {
    @URLs(url="/hud/listmessages")
    public static void messagesListHUD(State st,SafeMap values) throws SystemException, UserException {
        messagesList(st,values);
    }
    @URLs(url = "/messages/list")
    public static void messagesList(State st,SafeMap values) throws SystemException, UserException {
        Message m=st.getCharacter().getMessage();
        Form f=st.form;
        if (m==null) { f.add(new TextError("You have no messages.")); return; }
        m.setActive();
        JSONObject j=new JSONObject(m.getJSON());
        String message=j.optString("message","");
        if (message.equalsIgnoreCase("factioninvite")) { displayFactionInvite(st,values,j); return; }
        throw new SystemException("Malformed message "+m.getId()+", contains no message");
        
    }

    private static void displayFactionInvite(State st, SafeMap values, JSONObject j) throws UserException, SystemException {
        Form f=st.form;
        Char from=Char.get(j.getInt("from"));
        CharacterGroup to=CharacterGroup.get(j.getInt("to"));
        f.add(new TextSubHeader("Faction Invite"));
        f.add("You have been invited to join the faction "+to.getName()+" by "+from.getName());
        f.add(new Paragraph());
        Modules.simpleHtml(st, "gphudclient.acceptrejectmessage", values);
        
    }
}
