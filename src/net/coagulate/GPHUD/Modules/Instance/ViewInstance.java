package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Outputs.Color;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

/** Allows viewing of an Instance object.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class ViewInstance {
    
    @URLs(url = "/instances/view/*")
    public static void viewInstance(State st,SafeMap values) throws UserException, SystemException {
        //System.out.println(st.uri);
        String split[]=st.getDebasedURL().split("/");
        //System.out.println(split.length);
        String id=split[split.length-1];
        Instance i=Instance.get(Integer.parseInt(id));
        viewInstance(st,values,i);
    }    
    
    public static void viewInstance(State st,SafeMap values,Instance i) throws UserException {
        String tz=st.avatar().getTimeZone();
        boolean full=false;
        if (st.isSuperUser()) { full=true; }
        Table map=new Table(); st.form.add(map);
        map.openRow().add("Name").add(i.getName());
        map.openRow().add("Owner").add(i.getOwner().getGPHUDLink());
        for (Region r:i.getRegions()) {
            map.openRow().add("Region").add(r).add(r.getOnlineStatus(tz));
            map.openRow().add("").add("").add("Server "+r.getServerVersion(true));
            map.openRow().add("").add("").add("HUD "+r.getHUDVersion(true));
            if (r.needsUpdate()) { map.openRow().add("").add("").add(new Color("orange","Update Required")); }
        }
    }

         
}
