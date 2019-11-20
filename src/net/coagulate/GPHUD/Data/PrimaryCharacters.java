package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import java.util.Set;

/**
 * @author Iain Price
 */
public class PrimaryCharacters {

	private static Char getPrimaryCharacter_internal(Instance instance, User avatar) {
		Integer primary = GPHUD.getDB().dqi(true, "select entityid from primarycharacters where avatarid=? and instanceid=?", avatar.getId(), instance.getId());
		Char c = Char.get(primary);
		if (c.retired()) {
			GPHUD.getDB().d("delete from primarycharacters where avatarid=? and instanceid=?", avatar.getId(), instance.getId());
			throw new NoDataException("Primary character is retired");
		}
		return c;
	}

	/**
	 * Get a primary character for this avatar in a particular instance, or create one.
	 *
	 * @param st Session state containing instance.
	 * @return
	 */
	public static Char getPrimaryCharacter(State st, boolean autocreate) {
		Instance instance = st.getInstance();
		User avatar = st.getAvatar();
		try {
			return getPrimaryCharacter_internal(instance, avatar);
		} catch (NoDataException e) {
			// hmm, well, lets make them one then.
			Set<Char> characterset = Char.getCharacters(instance, avatar);
			if (characterset.isEmpty()) {
				if (!autocreate) { return null; }
				// make them a character
				st.logger().info("Created default character for " + avatar.toString());
				Char.create(st, avatar.getName());
				characterset = Char.getCharacters(instance, avatar);
				if (characterset.isEmpty()) {
					st.logger().severe("Created character for avatar but avatar has no characters still");
					throw new NoDataException("Could not create a character for this avatar");
				}
				Audit.audit(st, Audit.OPERATOR.AVATAR, avatar, Char.get(characterset.iterator().next().getId()), "Create", "Character", null, avatar.getName(), "Automatically generated character upon login with no characters.");
			}
			GPHUD.getDB().d("insert into primarycharacters(avatarid,instanceid,entityid) values(?,?,?)", avatar.getId(), instance.getId(), characterset.iterator().next().getId());
			return getPrimaryCharacter_internal(instance, avatar);
		}
	}

	public static void setPrimaryCharacter(State st, Char c) {
		c.validate(st);
		GPHUD.getDB().d("delete from primarycharacters where avatarid=? and instanceid=?", st.avatar().getId(), st.getInstance().getId());
		GPHUD.getDB().d("insert into primarycharacters(avatarid,instanceid,entityid) values(?,?,?)", st.avatar().getId(), st.getInstance().getId(), c.getId());
	}

	public static void purge(Char ch) {
		GPHUD.getDB().d("delete from primarycharacters where entityid=?",ch.getId());
	}
}
