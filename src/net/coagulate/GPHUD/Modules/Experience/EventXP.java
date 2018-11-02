package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

/** Implements Visit XP awards.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class EventXP extends QuotaedXP {
    public EventXP(int id) { super(id); }
    public String getName() { return "EventXP"; }
    public String poolName(State st) {return "Events.EventXP";}
    public String quotaKV(State st) {return "Events.EventXPLimit";}
    public String periodKV(State st) { return "Events.EventXPPeriod"; }
    public Module getModule() { return Modules.get(null,"Events"); }
   
}
