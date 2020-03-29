package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.NullInteger;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.Core.Tools.UnixTime;
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
public class Cookies {

	public static final int COOKIE_LIFESPAN=60*60*6;
	public static final int COOKIE_REFRESH=((int) (2.0/3.0*COOKIE_LIFESPAN)); // if cookie expires sooner than this many minutes from now
	private final String cookie;
	// i.e. 1/3rd of time gone (10 minutes), 20 minutes left, then refresh cookie.
	@Nullable
	private ResultsRow r;

	/**
	 * Load existing cookie store.
	 *
	 * @param cookie Cookie to load
	 *
	 * @throws UserException if the cookie does not exist or has expired.
	 */
	public Cookies(final String cookie) {
		this.cookie=cookie;
		validateCookie();
	}

	/**
	 * Delete a cookie
	 *
	 * @param cookie Cookie to delete
	 */
	public static void delete(final String cookie) {
		GPHUD.getDB().d("delete from cookies where cookie=?",cookie);
	}

	/**
	 * Refreshes a cookie, if necessary.
	 *
	 * @param cookie Cookie to refresh.
	 */
	public static void refreshCookie(final String cookie) {
		final int refreshifexpiresbefore=getUnixTime()+COOKIE_REFRESH;
		final int toupdate=GPHUD.getDB().dqinn("select count(*) from cookies where cookie=? and expires<? and renewable=1",cookie,refreshifexpiresbefore);
		if (toupdate==0) { return; }
		if (toupdate>1) {
			GPHUD.getLogger().warning("Unexpected anomoly, "+toupdate+" rows to update on cookie "+cookie);
		}
		//Log.log(Log.DEBUG,"SYSTEM","Cookies","Refreshing cookie "+cookie);
		GPHUD.getDB().d("update cookies set expires=? where cookie=?",getUnixTime()+COOKIE_LIFESPAN,cookie);
	}

	/**
	 * Create a new cookie session
	 *
	 * @param avatar    Avatar object to bind to, may be null
	 * @param character Character object to bind to, may be null
	 * @param instance  Instance object to bind to, may be null
	 * @param renewable Cookie can be refreshed
	 *
	 * @return the cookie string
	 */
	@Nonnull
	public static String generate(@Nullable final User avatar,
	                              @Nullable final Char character,
	                              final Instance instance,
	                              final boolean renewable) {
		final String cookie=Tokens.generateToken();
		int expire=getUnixTime();
		expire=expire+COOKIE_LIFESPAN;
		int renewableint=0;
		if (renewable) { renewableint=1; }
		/*
		String id = "";
		if (avatar != null) { id += " Avatar:" + avatar; }
		if (character != null) { id += " Character:" + character; }
		 */
		GPHUD.getDB()
		     .d("insert into cookies(cookie,expires,renewable,avatarid,characterid,instanceid) values(?,?,?,?,?,?)",
		        cookie,
		        expire,
		        renewableint,
		        getId(avatar),
		        getId(character),
		        getId(instance)
		       );
		return cookie;
	}

	private static Object getId(@Nullable final TableRow r) {
		if (r==null) { return new NullInteger(); }
		return r.getId();
	}

	private static Object getId(@Nullable final User r) {
		if (r==null) { return new NullInteger(); }
		return r.getId();
	}

	/** Load cookie, or return null if not available (expired)
	 *
	 * @param cookie Cookie to resolve
	 * @return Cookies object, or null if doesn't exist / expired.
	 */
	@Nullable
	public static Cookies loadOrNull(@Nullable final String cookie) {
		if (cookie!=null) {
			try {
				return new Cookies(cookie);
			}
			catch (@Nonnull final UserException ignored) {} // logged out possibly, or expired and cleaned up
		}
		return null;
	}

	public static int countAll() {
		return GPHUD.getDB().dqinn("select count(*) from cookies");
	}

	public static void expire() {
		GPHUD.getDB().d("delete from cookies where expires<?",UnixTime.getUnixTime());
	}

	@Nonnull
	private ResultsRow r() {
		if (r==null) { throw new SystemConsistencyException("No cookie loaded?"); }
		return r;
	}

	/**
	 * Check cookie is valid.
	 * Checks cookie hasn't expired, exists, and if necessary refreshes it.
	 *
	 * @throws UserException if the cookie fails validation in any way.
	 */
	private void validateCookie() {
		try { load(); }
		catch (@Nonnull final NoDataException e) {
			throw new UserInputStateException("Cookie Expired!",e);
		}
		final int expires=r().getInt("expires");
		if (expires<getUnixTime()) {
			GPHUD.getDB().d("delete from cookies where cookie=?",cookie);
			throw new UserInputStateException("Cookie Expired!");
		}
		// if expires within 20 minutes, set expires to 30 minutes :P
		if (r().getInt("renewable")==0) { return; }
		final int now=getUnixTime();
		if (expires<(now+COOKIE_REFRESH)) {
			GPHUD.getDB().d("update cookies set expires=? where cookie=?",now+COOKIE_LIFESPAN,cookie);
		}

	}

	private void load() {
		r=GPHUD.getDB().dqone("select * from cookies where cookie=?",cookie);
	}

	/**
	 * Return the avatar associated with this cookie
	 *
	 * @return The avatar, or null.
	 */
	@Nullable
	public User getAvatar() {
		final Integer avatarid=r().getIntNullable("avatarid");
		if (avatarid==null) { return null; }
		return User.get(avatarid);
	}

	/**
	 * Set the avatar for this cookie
	 *
	 * @param avatar Avatar to set to
	 */
	public void setAvatar(@Nonnull final User avatar) {
        /*if (avatar==null) {
            GPHUD.getDB().d("update cookies set avatarid=null where cookie=?",cookie); load(); return;
        }*/
		GPHUD.getDB().d("update cookies set avatarid=? where cookie=?",avatar.getId(),cookie);
		load();
	}

	/**
	 * Get the character for this cookie
	 *
	 * @return Character
	 */
	@Nullable
	public Char getCharacter() {
		final Integer entityid=r().getIntNullable("characterid");
		if (entityid==null) { return null; }
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
			if (i!=null) { // if has instance, character should be from it...
				if (character.getInstance()!=i) {
					throw new SystemConsistencyException("Character is not from the selected instance");
				}
			}
			else {
				// if instance /is/ null, then set it :P
				setInstance(null);
			}
		}
		GPHUD.getDB().d("update cookies set characterid=? where cookie=?",id,cookie);
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
		if (instanceid==null) { return null; }
		return Instance.get(instanceid);
	}

	/**
	 * Set an instance for this cookie
	 *
	 * @param entity Instance
	 */
	public void setInstance(@Nullable final Instance entity) {
		Integer id=null;
		if (entity!=null) { id=entity.getId(); }
		GPHUD.getDB().d("update cookies set instanceid=? where cookie=?",id,cookie);
		load();
	}

	@Nullable
	public String toString() { return "Avatar:"+getAvatar()+", Instance: "+getInstance()+", Character:"+getCharacter(); }

	/** Load GPHUD state from the cookie.
	 * Load the instance user and character from the cookie if they exist.
	 * Inherits the avatar from the character if not set.
	 *
	 * @param st State to update
	 */
	public void setStateFromCookies(@Nonnull final State st) {
		final Instance instance=getInstance();
		if (instance!=null) { st.setInstance(instance); }
		final User av=getAvatar();
		final Char ch=getCharacter();
		if (av!=null) { st.setAvatar(av); }
		if (ch!=null) { st.setCharacter(ch); }
		if (av==null && ch!=null) { st.setAvatar(ch.getOwner()); }
		if (av!=null) {
			st.cookiestring=cookie;
			st.cookie(this);
		}
	}
}
