package net.coagulate.GPHUD.Modules.Configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.SideSubMenu;
import net.coagulate.GPHUD.State;

/** Represents the Configuration module, extending the annotated types to support dynamic (module based) side sub menus.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConfigurationModule extends ModuleAnnotation {
    
    public ConfigurationModule(String name, ModuleDefinition def) throws SystemException, UserException {
        super(name, def);
        submenus=null;
    }

    Set<SideSubMenu> submenus;
    
    @Override
    public Set<SideSubMenu> getSideSubMenus(State st) {
        if (submenus==null) { 
            submenus=new HashSet<>();
            Map<String,SideSubMenu> map=new TreeMap<>();
            for (Module m:Modules.getModules()) {
                if (m.isEnabled(st)) {
                    if (m.alwaysHasConfig() || !m.getKVDefinitions(st).isEmpty()) {
                        map.put(m.getName(),new ConfigurationSideSubMenu(m));
                        submenus.add(map.get(m.getName()));
                    }
                }
            }
            int priority=1;
            for (String name:map.keySet()) {
                ((ConfigurationSideSubMenu)map.get(name)).setPriority(priority);
                priority++;
            }
        }
            
        return submenus;
    }
    
    
}
