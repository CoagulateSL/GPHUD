package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * @author Iain Price
 */
public class Visit {
	// ---------- STATICS ----------

	/**
	 * Start a visit record.
	 * Ends any existing visit records for this avatar.
	 *
	 * @param st        State
	 * @param character character that is visiting
	 * @param region    region that is being visited
	 */
	public static void initVisit(@Nonnull final State st,
	                             @Nonnull final Char character,
	                             @Nonnull final Region region) {
		final User avatar=st.getAvatar();
		final int updates=db().dqinn("select count(*) from visits where avatarid=? and endtime is null",avatar.getId());
		if (updates>0) {
			st.logger().fine("Force terminating "+updates+" visits");
			db().d("update visits set endtime=? where avatarid=? and endtime is null",getUnixTime(),avatar.getId());
			final Set<User> avatarset=new HashSet<>();
			avatarset.add(avatar);
			for (final Region reg: Region.getRegions(st,false)) { reg.departingAvatars(st,avatarset); }
		}
		st.logger().fine("Starting visit for "+character.getNameSafe()+" at "+region.getNameSafe()+" on avatar "+avatar.getName());
		db().d("insert into visits(avatarid,characterid,regionid,starttime) values(?,?,?,?)",avatar.getId(),character.getId(),region.getId(),getUnixTime());
	}

	/**
	 * Close the visits for a character.
	 *
	 * @param state State infers character
	 */

	public static void closeVisits(@Nonnull final State state) {
		if (state.getInstance()!=state.getCharacter().getInstance()) { throw new SystemConsistencyException("State character instanceid mismatch"); }
		closeVisits(state.getCharacter(),state.getRegion());
	}

	/** Close the visits for a character
	 * @param character The character
	 */
	public static void closeVisits(@Nonnull final Char character,@Nullable final Region region) {
		db().d("update eventvisits set endtime=UNIX_TIMESTAMP() where characterid=?",character.getId());
		if (region!=null) {
			db().d("update visits set endtime=UNIX_TIMESTAMP() where characterid=? and regionid=? and endtime is null",character.getId(),region.getId());
		} else {
			db().d("update visits set endtime=UNIX_TIMESTAMP() where characterid=? and endtime is null",character.getId());
		}
	}


	/**
	 * Sum visit time on sim.
	 *
	 * @param character Who to sum visit times for
	 * @param since     Ignore visits that end before this time (Unix Time)
	 *
	 * @return Total number of seconds the character has visited the sim for since the specified time
	 */
	public static int sumVisits(@Nonnull final Char character,
	                            final int since) {
		final int now=getUnixTime();
		final Results visits=db().dq("select starttime,endtime from visits where characterid=? and (endtime is null or endtime>?)",character.getId(),since);
		int seconds=0;
		for (final ResultsRow r: visits) {
			Integer end=r.getIntNullable("endtime");
			final Integer start=r.getIntNullable("starttime");
			if (end==null) { end=now; }
			if (start!=null) { seconds=seconds+(end-start); }
		}
		return seconds;
	}

	public static Table statusDump(State st) {
		Table t=new Table().border();
		t.header("Avatar");
		t.header("Character");
		t.header("Region");
		t.header("Start time");
		t.header("End Time (empty, visit is open)");
		for (ResultsRow row: db().dq("select visits.* from visits inner join characters on visits.characterid=characters.characterid where characters.instanceid=? and visits.endtime is null",st.getInstance().getId())) {
			t.openRow();
			Integer avatar=row.getIntNullable("avatarid");
			t.add(avatar==null?"Null?":User.get(avatar).getName()+"[#"+avatar+"]");
			t.add(Char.get(row.getInt("characterid")).getName()+"[#"+row.getInt("characterid")+"]");
			Integer region=row.getIntNullable("regionid");
			t.add(region==null?"Null?":Region.get(region,true).getName()+"[#"+region+"]");
			t.add(UnixTime.fromUnixTime(row.getIntNullable("starttime"),st.getAvatar().getTimeZone()));
			t.add(UnixTime.fromUnixTime(row.getIntNullable("endtime"),st.getAvatar().getTimeZone()));
		}
		return t;
	}

	// ----- Internal Statics -----
	private static DBConnection db() { return GPHUD.getDB(); }
}
