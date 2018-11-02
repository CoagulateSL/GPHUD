package net.coagulate.GPHUD.Modules.User;

import java.util.Set;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Avatar;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.User;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/** Views a User object.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ViewUser {

    @URLs(url = "/users/view/*")
    public static void viewUser(State st,SafeMap values) throws UserException, SystemException {
        String split[]=st.getDebasedURL().split("/");
        String id=split[split.length-1];
        User u=User.get(Integer.parseInt(id));
        viewUser(st,values,u);
    }    
    
    public static void viewUser(State st,SafeMap values,User u) {
        boolean full=false;
        if (st.isSuperUser()) { full=true; }
        Table map=new Table(); st.form.add(map);
        map.openRow().add("Username").add(u.getName());
        map.openRow().add("SuperUser").add(""+u.isSuperAdmin());
        map.openRow().add("DeveloperKey").add(""+u.hasDeveloperKey());
        if (st.user==u) {
            map.openRow().add(new Cell(new Link("Change Password","../changepassword"),2));
        }
        if (st.isSuperUser()) {
            map.openRow().add(new Cell(new Link("SHUTDOWN SERVER","../../introspection/cleanshutdown")));
        }
        if (!st.isSuperUser()) { st.form.add("This is all you are permitted to view."); }
        map.openRow().add("<b>Avatar</b>").add("<b>Character</b>").add("<b>Instance</b>");
        Set<Avatar> avatars=u.getAvatars();
        for (Avatar a:avatars) { 
            map.openRow().add(a);
            for (Char c:a.getCharacters()) { map.openRow().add("").add(c).add(c.getInstance()); }
        }
        Results rows=net.coagulate.GPHUD.Data.Audit.getAudit(null, u,null,null);
        Table table = net.coagulate.GPHUD.Data.Audit.formatAudit(rows,st.avatar().getTimeZone());
        st.form.add(table);        
    }
    
}
