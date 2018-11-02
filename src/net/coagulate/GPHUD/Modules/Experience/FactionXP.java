package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

/** Implements Visit XP awards.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class FactionXP extends QuotaedXP {
    public FactionXP(int id) { super(id); }
    public String getName() { return "FactionXP"; }
    public  String poolName(State st) {return "Faction.FactionXP";}
    public  String quotaKV(State st) {return "Faction.XPPerCycle";}
    public  String periodKV(State st) { return "Faction.XPCycleLength"; }
    public Module getModule() { return Modules.get(null,"Faction"); }
   
}
