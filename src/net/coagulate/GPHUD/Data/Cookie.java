package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.NullInteger;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Handles session cookies.
 * Session cookies are used in numerous places throughout HTTP interactions.
 * The user interface and GPHUD panel interface use sessions via cookies to store login data.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Cookie {
	
	public static final int        COOKIE_LIFESPAN=60*60*6;
	public static final int        COOKIE_REFRESH =((int)(2.0/3.0*COOKIE_LIFESPAN));
			// if cookie expires sooner than this many minutes from now
	private final       String     cookie;
	// i.e. 1/3rd of time gone (10 minutes), 20 minutes left, then refresh cookie.
	@Nullable private   ResultsRow r;
	
	/**
	 * Load existing cookie store.
	 *
	 * @param cookie Cookie to load
	 * @throws UserException if the cookie does not exist or has expired.
	 */
	public Cookie(@Nonnull final String cookie) {
		this.cookie=cookie;
		validateCookie();
	}
	
	// ---------- STATICS ----------
	
	/**
	 * Delete a cookie
	 *
	 * @param cookie Cookie to delete
	 */
	public static void delete(@Nonnull final String cookie) {
		db().d("delete from cookies where cookie=?",cookie);
	}
	
	/**
	 * Refreshes a cookie, if necessary.
	 *
	 * @param cookie Cookie to refresh.
	 */
	public static void refreshCookie(@Nonnull final String cookie) {
		final int refreshBefore=getUnixTime()+COOKIE_REFRESH;
		final int toupdate=db().dqiNotNull("select count(*) from cookies where cookie=? and expires<? and renewable=1",
		                                   cookie,
		                                   refreshBefore);
		if (toupdate==0) {
			return;
		}
		if (toupdate>1) {
			GPHUD.getLogger().warning("Unexpected anomoly, "+toupdate+" rows to update on cookie "+cookie);
		}
		db().d("update cookies set expires=? where cookie=?",getUnixTime()+COOKIE_LIFESPAN,cookie);
	}
	
	// ----- Internal Statics -----
	private static DBConnection db() {
		return GPHUD.getDB();
	}
	
	/**
	 * Counts the total number of valid cookies.
	 *
	 * @return Cookie count
	 */
	public static int countAll() {
		return db().dqiNotNull("select count(*) from cookies");
	}
	
	/**
	 * Create a new cookie session
	 *
	 * @param avatar    Avatar object to bind to, may be null
	 * @param character Character object to bind to, may be null
	 * @param instance  Instance object to bind to, may be null
	 * @param renewable Cookie can be refreshed
	 * @return the cookie string
	 */
	@Nonnull
	public static String generate(@Nullable final User avatar,
	                              @Nullable final Char character,
	                              @Nullable final Instance instance,
	                              final boolean renewable) {
		final String cookie=Tokens.generateToken();
		int expire=getUnixTime();
		expire=expire+COOKIE_LIFESPAN;
		int renewableint=0;
		if (renewable) {
			renewableint=1;
		}
		db().d("insert into cookies(cookie,expires,renewable,avatarid,characterid,instanceid) values(?,?,?,?,?,?)",
		       cookie,
		       expire,
		       renewableint,
		       getId(avatar),
		       getId(character),
		       getId(instance));
		return cookie;
	}
	
	private static Object getId(@Nullable final User r) {
		if (r==null) {
			return new NullInteger();
		}
		return r.getId();
	}
	
	private static Object getId(@Nullable final TableRow r) {
		if (r==null) {
			return new NullInteger();
		}
		return r.getId();
	}
	
	/**
	 * Load cookie, or return null if not available (expired)
	 *
	 * @param cookie Cookie to resolve
	 * @return Cookies object, or null if doesn't exist / expired.
	 */
	@Nullable
	public static Cookie loadOrNull(@Nullable final String cookie) {
		if (cookie!=null) {
			try {
				return new Cookie(cookie);
			} catch (@Nonnull final UserException ignored) {  // logged out possibly, or expired and cleaned up
			}
		}
		return null;
	}
	
	/**
	 * Trigger expired cookie cleaning
	 */
	public static void expire() {
		db().d("delete from cookies where expires<?",getUnixTime());
	}
	
	// ---------- INSTANCE ----------
	
	@Nonnull
	public String toString() {
		return "Avatar:"+getAvatar()+", Instance: "+getInstance()+", Character:"+getCharacter();
	}
	
	/**
	 * Return the avatar associated with this cookie
	 *
	 * @return The avatar, or null.
	 */
	@Nullable
	public User getAvatar() {
		final Integer avatarid=r().getIntNullable("avatarid");
		if (avatarid==null) {
			return null;
		}
		return User.get(avatarid);
	}
	
	/**
	 * Set the avatar for this cookie
	 *
	 * @param avatar Avatar to set to
	 */
	public void setAvatar(@Nonnull final User avatar) {
		db().d("update cookies set avatarid=? where cookie=?",avatar.getId(),cookie);
		load();
	}
	
	/**
	 * Get the instance for this cookie
	 *
	 * @return The instance, or null if not set
	 */
	@Nullable
	public Instance getInstance() {
		final Integer instanceid=r().getIntNullable("instanceid");
		if (instanceid==null) {
			return null;
		}
		return Instance.get(instanceid);
	}
	
	/**
	 * Get the character for this cookie
	 *
	 * @return Character
	 */
	@Nullable
	public Char getCharacter() {
		final Integer entityid=r().getIntNullable("characterid");
		if (entityid==null) {
			return null;
		}
		return Char.get(entityid);
	}
	
	/**
	 * Set the character for this cookie
	 *
	 * @param character Character to set to
	 */
	public void setCharacter(@Nullable final Char character) {
		Integer id=null;
		if (character!=null) {
			id=character.getId();
			final Instance i=getInstance();
			if (i==null) {
				// if instance /is/ null, then set it :P
				setInstance(null);
			} else { // if has instance, character should be from it...
				if (character.getInstance()!=i) {
					throw new SystemConsistencyException("Character is not from the selected instance");
				}
			}
		}
		db().d("update cookies set characterid=? where cookie=?",id,cookie);
		load();
	}
	
	// ----- Internal Instance -----
	@Nonnull
	private ResultsRow r() {
		if (r==null) {
			throw new SystemConsistencyException("No cookie loaded?");
		}
		return r;
	}
	
	/**
	 * Set an instance for this cookie
	 *
	 * @param entity Instance
	 */
	public void setInstance(@Nullable final Instance entity) {
		Integer id=null;
		if (entity!=null) {
			id=entity.getId();
		}
		db().d("update cookies set instanceid=? where cookie=?",id,cookie);
		load();
	}
	
	/**
	 * Load GPHUD state from the cookie.
	 * Load the instance user and character from the cookie if they exist.
	 * Inherits the avatar from the character if not set.
	 *
	 * @param st State to update
	 */
	public void setStateFromCookies(@Nonnull final State st) {
		final Instance instance=getInstance();
		if (instance!=null) {
			st.setInstance(instance);
		}
		final User av=getAvatar();
		final Char ch=getCharacter();
		if (av!=null) {
			st.setAvatar(av);
		}
		if (ch!=null) {
			st.setCharacter(ch);
		}
		if (av==null&&ch!=null) {
			st.setAvatar(ch.getOwner());
		}
		if (av!=null) {
			st.cookieString=cookie;
			st.cookie(this);
		}
	}
	
	/**
	 * Check cookie is valid.
	 * Checks cookie hasn't expired, exists, and if necessary refreshes it.
	 *
	 * @throws UserException if the cookie fails validation in any way.
	 */
	private void validateCookie() {
		try {
			load();
		} catch (@Nonnull final NoDataException e) {
			throw new UserInputStateException("Cookie Expired!",e);
		}
		final int expires=r().getInt("expires");
		if (expires<getUnixTime()) {
			db().d("delete from cookies where cookie=?",cookie);
			throw new UserInputStateException("Cookie Expired!");
		}
		// if expires within 20 minutes, set expires to 30 minutes :P
		if (r().getInt("renewable")==0) {
			return;
		}
		final int now=getUnixTime();
		if (expires<(now+COOKIE_REFRESH)) {
			db().d("update cookies set expires=? where cookie=?",now+COOKIE_LIFESPAN,cookie);
		}
		
	}
	
	/**
	 * Load the cookie row.
	 */
	private void load() {
		r=db().dqOne("select * from cookies where cookie=?",cookie);
	}
}
