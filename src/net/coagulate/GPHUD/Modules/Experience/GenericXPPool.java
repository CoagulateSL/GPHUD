package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;

/**
 *
 * @author Iain Price
 */
public class GenericXPPool extends Pool {
    private final String myname;
    public GenericXPPool(String name) { super(); myname=name; }
    @Override public boolean isGenerated() { return true; }
    @Override public String description() { return myname+" pool"; }
    @Override public String fullName() { return "Experience."+myname; }
    @Override public String name() { return myname; }
    
    public void awardXP(State st,Char target,String reason,int ammount) {
        float period=st.getKV(fullName()+"XPPeriod").floatValue();
        int maxxp=st.getKV(fullName()+"XPLimit").intValue();
        Pool pool=Modules.getPool(st,"Experience."+myname+"XP");
        int awarded=target.sumPoolDays(pool, period);
        if (awarded>=maxxp) {
            throw new UserException("This character has already reached their "+pool.name()+" XP limit.  They will next be eligable for a point in "+target.poolNextFree(pool,maxxp,period));
        }
        if ((awarded+ammount)>maxxp) {
            throw new UserException("This will push the character beyond their "+pool.name()+" XP limit, they can be awarded "+(maxxp-awarded)+" XP right now");
        }
        // else award xp :P
        Audit.audit(st, Audit.OPERATOR.CHARACTER, null, target, "Pool Add", pool.name()+"XP", null, ammount+"", reason);
        target.addPool(st, pool, ammount, reason);
        if (target!=st.getCharacter()) { target.hudMessage("You were granted "+ammount+" point"+(ammount==1?"":"s")+" of "+pool.name()+" XP by (("+st.getAvatar().getName()+")) for "+reason); }
    }    
    
}
