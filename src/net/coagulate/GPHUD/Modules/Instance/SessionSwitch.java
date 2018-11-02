package net.coagulate.GPHUD.Modules.Instance;

import java.util.Set;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Outputs.Separator;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Characters.View;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Lets a user/avatar switch instance/character
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SessionSwitch {
    
    @URLs(url="/switch/instance")
    public static void switchInstance(State st,SafeMap values) throws UserException {
        Form f=st.form;
        f.add(new TextHeader("Select Instance"));
        f.add(new Separator());
        for (Instance i:Instance.getInstances()) {
            String id=i.getId()+"";
            f.add(new Button("Select Instance - "+id,"Select Instance - "+i.getName()));
            if (!values.get("Select Instance - "+id).isEmpty()) {
                st.setInstance(i);
                st.cookie.setInstance(i);
                st.setCharacter(null);
                st.cookie.setCharacter(st.getCharacterNullable());
                st.avatar().setLastInstance(i);
                throw new RedirectionException("/switch/character");
            }
            ViewInstance.viewInstance(st, values, i);
            f.add(new Separator());
        }
    }
    
    @URLs(url="/switch/character")
    public static void switchCharacter(State st,SafeMap values) throws UserException, SystemException {
        Form f=st.form;
        if (!values.get("charid").isEmpty()) {
            Char c=Char.get(Integer.parseInt(values.get("charid")));
            c.validate(st);
            if (c.getOwner()!=st.avatar()) { throw new UserException("You do not own this character"); }
            st.setCharacter(c);
            st.cookie.setCharacter(st.getCharacter());
            throw new RedirectionException("/");
        } 
        f.noForm();
        f.add(new TextHeader("Select Character"));
        Set<Char> chars = st.avatar().getCharacters(st.getInstance());
        if (chars.isEmpty()) { f.add("You have no characters at this instance, please select a new instance or navigate via the left side menu."); return; }
        f.add(new Separator());
        for (Char c:chars) {
            String id=c.getId()+"";
            String name=c.getName();
            f.add(new Form(st, true, "", "Select Character - "+name,"charid",id));
            View.viewCharacter(st, values, c,true);
            f.add(new Separator());    
        }
    }
    
}
