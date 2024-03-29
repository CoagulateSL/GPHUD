package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Data.*;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Module;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import java.util.Set;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Implements Visit XP awards.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class VisitXP extends QuotaedXP {
	public VisitXP(final int id) {
		super(id);
	}
	
	public void runAwards(@Nonnull final Instance i) {
		try {
			final State st=new State();
			st.setInstance(i);
			st.setAvatar(User.getSystem());
			for (final Region r: Region.getRegions(i,false)) {
				final Set<Char> visitors=r.getOpenVisits();
				for (final Char visitor: visitors) {
					runAwards(st,visitor);
				}
			}
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Exception running awards for instance "+i.getNameSafe()+" #"+i.getId(),e);
		}
	}
	
	// ---------- INSTANCE ----------
	public void runAwards(@Nonnull final State st,@Nonnull final Char ch) {
		try {
			final Module m=Modules.get(null,"Experience");
			if (m.isEnabled(st)) {
				st.setCharacter(ch);
				int maxLevel=st.getKV("Experience.MaxLevel").intValue();
				if (maxLevel==0) {
					maxLevel=1000;
				}
				if (st.getCharacter().getLevel(st)>=maxLevel) {
					return;
				}
				final int perweek=st.getKV("Experience.VisitXPPerCycle").intValue();
				final int duration=st.getKV("Experience.VisitXPDuration").intValue();
				final int points=st.getKV("Experience.VisitXPPoints").intValue();
				final int since=getUnixTime()-(Experience.getCycle(st));
				int timethisweek=Visit.sumVisits(ch,since);
				timethisweek=timethisweek/60;
				final int xpthisweek=CharacterPool.sumPoolSince(ch,Modules.getPool(null,"Experience.VisitXP"),since);
				//System.out.println("Sum total visit time for "+ch+" is "+timethisweek);
				//System.out.println("Sum visit xp in that time period is "+xpthisweek);
				//System.out.println("Config is "+points+" per "+duration+" total "+perweek);
				if (duration==0) {
					return;
				}
				int wanttogive=(timethisweek/duration)*points-xpthisweek;
				//System.out.println("Uncapped wanttogive "+wanttogive);
				if ((xpthisweek+wanttogive)>perweek) {
					wanttogive=perweek-xpthisweek;
				}
				//System.out.println("Capped wanttogive "+wanttogive);
				if (wanttogive<=0) {
					return;
				}
				final Pool visitxp=Modules.getPool(st,"experience.visitxp");
				CharacterPool.addPool(st,ch,visitxp,wanttogive,"Awarded XP for time on sim");
				final State fakestate=new State();
				fakestate.setInstance(st.getInstance());
				fakestate.setAvatar(User.getSystem());
				ch.hudMessage(
						"You were awarded 1 point of Visit XP, you will be eligible for your next point "+nextFree(st));
				Audit.audit(fakestate,
				            Audit.OPERATOR.AVATAR,
				            null,
				            ch,
				            "Pool Add",
				            "VisitXP",
				            null,
				            String.valueOf(wanttogive),
				            "Awarded XP for time on sim");
			}
		} catch (@Nonnull final Exception e) {
			GPHUD.getLogger().log(SEVERE,"Exception running awards for character "+ch.getNameSafe()+" #"+ch.getId(),e);
		}
	}
	
	public Module getModule() {
		return Modules.get(null,"Experience");
	}
	
	@Nonnull
	public String periodKV(final State st) {
		return "Experience.XPCycleDays";
	}
	
	@Nonnull
	public String poolName(final State st) {
		return "experience.visitxp";
	}
	
	@Nonnull
	public String quotaKV(final State st) {
		return "Experience.VisitXPPerCycle";
	}
	
	@Nonnull
	public String getName() {
		return "VisitXP";
	}
	
	
}
