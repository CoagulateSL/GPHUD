package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.EXPERIENCE;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.State;

/**  Generic utilities.
 *
 * @author iain
 */
public abstract class Experience {
    @Template(name = "TOTALXP",description = "Total experience")
    public static String getTotalXP(State st,String key) {
        if (!st.hasModule("Experience")) { return ""; }
        if (st.getCharacterNullable()==null) { return ""; }
        return getExperience(st, st.getCharacter())+"";
    }
    @Template(name="LEVEL",description="Current Level")
    public static String getLevel(State st,String key) {
        if (!st.hasModule("Experience")) { return ""; }
        if (st.getCharacterNullable()==null) { return ""; }
        return toLevel(st,getExperience(st, st.getCharacter()))+"";
    }
    public static int toLevel(State st,int xp) throws UserException, SystemException {
        if (!st.hasModule("Experience")) { return 0; }
        int step=st.getKV("Experience.LevelXPStep").intValue();
        int tolevel=0;
        for (int i=0;i<=1000;i++) {
                tolevel=(int) (tolevel+Math.floor(((float)i)/((float)step))+1);
                if (tolevel>xp) { return i; }
        }
        return 1000;

    }

    public static int getExperience(State st, Char character) throws UserException, SystemException {
        int sum=0;
        if (Modules.get(null,"experience").isEnabled(st)) { sum+=character.sumPool(Modules.getPool(st,"Experience.VisitXP")); }
        if (Modules.get(null,"faction").isEnabled(st)) { sum+=character.sumPool(Modules.getPool(st,"Faction.FactionXP")); }
        if (Modules.get(null,"Events").isEnabled(st)) { sum+=character.sumPool(Modules.getPool(st,"Events.EventXP")); }
        for (Attribute a:st.getAttributes()) {
            if (a.getType()==EXPERIENCE) { sum+=character.sumPool(Modules.getPool(st,"Experience."+a.getName()+"XP")); }
        }
        return sum;
    }

    public static String getCycleLabel(State st) throws UserException, SystemException {
        if (Modules.get(null,"Experience").isEnabled(st)) {
            return Math.round(st.getKV("Experience.XPCycleDays").floatValue())+" days";
        } else { return "week"; }
    }
    public static int getCycle(State st) throws UserException, SystemException {
        if (Modules.get(null,"Experience").isEnabled(st)) {
            return (int)(60*60*24*st.getKV("Experience.XPCycleDays").floatValue());
        } else {
            return 60*60*24*7;
        }
    }
    
}
