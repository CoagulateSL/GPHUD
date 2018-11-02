package net.coagulate.GPHUD.Modules.Experience;

import java.util.HashSet;
import java.util.Set;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Dynamic events menu
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ExperienceModule extends ModuleAnnotation {
    
    public ExperienceModule(String name, ModuleDefinition def) throws SystemException, UserException {
        super(name, def);
    }

    @Override
    public Set<CharacterAttribute> getAttributes(State st) {
        Set<CharacterAttribute> ret=new HashSet<>();
        if (st.hasModule("Events")) { ret.add(new EventXP(-1)); }
        ret.add(new VisitXP(-1));
        if (st.hasModule("Faction")) { ret.add(new FactionXP(-1)); }
        return ret;
    }
    

}
