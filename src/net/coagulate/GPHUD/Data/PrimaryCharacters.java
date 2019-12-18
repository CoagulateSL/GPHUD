package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author Iain Price
 */
public class PrimaryCharacters {

	@Nonnull
	private static Char getPrimaryCharacter_internal(@Nonnull final Instance instance,
	                                                 @Nonnull final User avatar)
	{
		final int primary=GPHUD.getDB()
		                       .dqinn("select entityid from primarycharacters where avatarid=? and instanceid=?",
		                              avatar.getId(),
		                              instance.getId()
		                             );
		final Char c=Char.get(primary);
		if (c.retired()) {
			GPHUD.getDB()
			     .d("delete from primarycharacters where avatarid=? and instanceid=?",avatar.getId(),instance.getId());
			throw new NoDataException("Primary character is retired");
		}
		return c;
	}

	/**
	 * Get a primary character for this avatar in a particular instance, or create one.
	 *
	 * @param st Session state containing instance.
	 *
	 * @return the primary character, or null (?)
	 */
	@Nullable
	public static Char getPrimaryCharacter(@Nonnull final State st,
	                                       final boolean autocreate)
	{
		final Instance instance=st.getInstance();
		final User avatar=st.getAvatar();
		try {
			return getPrimaryCharacter_internal(instance,avatar);
		} catch (@Nonnull final NoDataException e) {
			// hmm, well, lets make them one then.
			Set<Char> characterset=Char.getCharacters(instance,avatar);
			if (characterset.isEmpty()) {
				if (!autocreate) { return null; }
				// make them a character
				st.logger().info("Created default character for "+avatar);
				Char.create(st,avatar.getName());
				characterset=Char.getCharacters(instance,avatar);
				if (characterset.isEmpty()) {
					st.logger().severe("Created character for avatar but avatar has no characters still");
					throw new NoDataException("Could not create a character for this avatar");
				}
				Audit.audit(st,
				            Audit.OPERATOR.AVATAR,
				            avatar,
				            Char.get(characterset.iterator().next().getId()),
				            "Create",
				            "Character",
				            null,
				            avatar.getName(),
				            "Automatically generated character upon login with no characters."
				           );
			}
			GPHUD.getDB()
			     .d("insert into primarycharacters(avatarid,instanceid,entityid) values(?,?,?)",
			        avatar.getId(),
			        instance.getId(),
			        characterset.iterator().next().getId()
			       );
			return getPrimaryCharacter_internal(instance,avatar);
		}
	}

	public static void setPrimaryCharacter(@Nonnull final State st,
	                                       @Nonnull final Char c)
	{
		c.validate(st);
		GPHUD.getDB()
		     .d("delete from primarycharacters where avatarid=? and instanceid=?",
		        st.getAvatar().getId(),
		        st.getInstance().getId()
		       );
		GPHUD.getDB()
		     .d("insert into primarycharacters(avatarid,instanceid,entityid) values(?,?,?)",
		        st.getAvatar().getId(),
		        st.getInstance().getId(),
		        c.getId()
		       );
	}

	public static void purge(@Nonnull final Char ch) {
		GPHUD.getDB().d("delete from primarycharacters where entityid=?",ch.getId());
	}
}
