package net.coagulate.GPHUD.Modules.Groups;

import java.util.HashSet;
import java.util.Set;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.Modules.StaticSideSubMenu;
import net.coagulate.GPHUD.State;

/** Dynamic groups menu
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class GroupModule extends ModuleAnnotation {
    
    public GroupModule(String name, ModuleDefinition def) throws SystemException, UserException {
        super(name, def);
    }

    @Override
    public Set<SideSubMenu> getSideSubMenus(State st) {
        Set<SideSubMenu> ret=new HashSet<>();
        int pri=1;
        for (String submenu:st.getCharacterGroupTypes()) {
            if (!submenu.isEmpty()) {
                ret.add(new StaticSideSubMenu(submenu, pri++, "/groups/type/"+submenu, ""));
            }
        }
        ret.add(new StaticSideSubMenu("Other",pri++,"/groups/type/BLANK",""));
        return ret;
    }

}
