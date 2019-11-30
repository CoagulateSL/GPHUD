package net.coagulate.GPHUD.Data;

import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * @author Iain Price
 */
public class Visits {
	/**
	 * Start a visit record.
	 * Ends any existing visit records for this avatar.
	 *
	 * @param st        State
	 * @param character character that is visiting
	 * @param region    region that is being visited
	 */
	public static void initVisit(@Nonnull State st, @Nonnull Char character, @Nonnull Region region) {
		User avatar = st.getAvatarNullable();
		int updates = GPHUD.getDB().dqi( "select count(*) from visits where avatarid=? and endtime is null", avatar.getId());
		if (updates > 0) {
			st.logger().fine("Force terminating " + updates + " visits");
			GPHUD.getDB().d("update visits set endtime=? where avatarid=? and endtime is null", getUnixTime(), avatar.getId());
			Set<User> avatarset = new HashSet<>();
			avatarset.add(avatar);
			for (Region reg : st.getInstance().getRegions(false)) { reg.departingAvatars(st, avatarset); }
		}
		st.logger().fine("Starting visit for " + character.getNameSafe() + " at " + region.getNameSafe() + " on avatar " + avatar.getName());
		GPHUD.getDB().d("insert into visits(avatarid,characterid,regionid,starttime) values(?,?,?,?)", avatar.getId(), character.getId(), region.getId(), getUnixTime());
	}


}
