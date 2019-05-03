package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import java.util.Set;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Implements Visit XP awards.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class VisitXP extends QuotaedXP {
	public VisitXP(int id) { super(id); }

	public void runAwards(State st, Char ch) {
		try {
			Module m = Modules.get(null, "Experience");
			if (m.isEnabled(st)) {
				st.setCharacter(ch);
				int perweek = st.getKV("Experience.VisitXPPerCycle").intValue();
				int duration = st.getKV("Experience.VisitXPDuration").intValue();
				int points = st.getKV("Experience.VisitXPPoints").intValue();
				int since = getUnixTime() - (Experience.getCycle(st));
				int timethisweek = ch.sumVisits(since);
				timethisweek = timethisweek / 60;
				int xpthisweek = ch.sumPoolSince(Modules.getPool(null, "Experience.VisitXP"), since);
				//System.out.println("Sum total visit time for "+ch+" is "+timethisweek);
				//System.out.println("Sum visit xp in that time period is "+xpthisweek);
				//System.out.println("Config is "+points+" per "+duration+" total "+perweek);
				int wanttogive = (timethisweek / duration) * points - xpthisweek;
				//System.out.println("Uncapped wanttogive "+wanttogive);
				if ((xpthisweek + wanttogive) > perweek) { wanttogive = perweek - xpthisweek; }
				//System.out.println("Capped wanttogive "+wanttogive);
				if (wanttogive <= 0) { return; }
				Pool visitxp = Modules.getPool(st, "experience.visitxp");
				ch.addPool(st, visitxp, wanttogive, "Awarded XP for time on sim");
				State fakestate = new State();
				fakestate.setInstance(st.getInstance());
				fakestate.setAvatar(User.getSystem());
				ch.hudMessage("You were awared 1 point of Visit XP, you will be eligable for your next point " + nextFree(st));
				Audit.audit(fakestate, Audit.OPERATOR.AVATAR, null, ch, "Pool Add", "VisitXP", null, "" + wanttogive, "Awarded XP for time on sim");
			}
		} catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Exception running awards for character " + ch.getNameSafe() + " #" + ch.getId(), e);
		}
	}

	public void runAwards(Instance i) {
		try {
			State st = new State();
			st.setInstance(i);
			st.setAvatar(User.getSystem());
			for (Region r : i.getRegions()) {
				Set<Char> visitors = r.getOpenVisits();
				for (Char visitor : visitors) {
					runAwards(st, visitor);
				}
			}
		} catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Exception running awards for instance " + i.getNameSafe() + " #" + i.getId(), e);
		}
	}

	public void runAwards() {
		try {
			Results results = GPHUD.getDB().dq("select instanceid from instancekvstore where k like 'experience.enabled' and (v is null or v like 'true')");
			for (ResultsRow r : results) {
				Instance i = Instance.get(r.getInt());
				runAwards(i);
			}
		} catch (Exception e) {
			GPHUD.getLogger().log(SEVERE, "Exception running awards outer task", e);
		}
	}

	public String getName() { return "VisitXP"; }

	public String poolName(State st) {return "experience.visitxp";}

	public String quotaKV(State st) {return "Experience.VisitXPPerCycle";}

	public String periodKV(State st) { return "Experience.XPCycleDays"; }

	public Module getModule() { return Modules.get(null, "Experience"); }


}
