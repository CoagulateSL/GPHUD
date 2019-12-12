package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.NullInteger;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Handles session cookies.
 * Session cookies are used in numerous places throughout HTTP interactions.
 * The user interface and GPHUD panel interface use sessions via cookies to store login data.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Cookies {

	public static final int COOKIE_LIFESPAN = 30 * 60;
	public static final int COOKIE_REFRESH = ((int) (2.0 / 3.0 * COOKIE_LIFESPAN)); // if cookie expires sooner than this many minutes from now
	// i.e. 1/3rd of time gone (10 minutes), 20 minutes left, then refresh cookie.
	private ResultsRow r;
	private final String cookie;

	/**
	 * Load existing cookie store.
	 *
	 * @param cookie Cookie to load
	 * @throws SystemException if the cookie does not exist or has expired.
	 */
	public Cookies(String cookie) {
		this.cookie = cookie;
		validateCookie();
	}

	/**
	 * Delete a cookie
	 *
	 * @param cookie Cookie to delete
	 */
	public static void delete(String cookie) {
		GPHUD.getDB().d("delete from cookies where cookie=?", cookie);
	}

	/**
	 * Refreshes a cookie, if necessary.
	 *
	 * @param cookie Cookie to refresh.
	 */
	public static void refreshCookie(String cookie) {
		int refreshifexpiresbefore = getUnixTime() + COOKIE_REFRESH;
		int toupdate = GPHUD.getDB().dqi(true, "select count(*) from cookies where cookie=? and expires<? and renewable=1", cookie, refreshifexpiresbefore);
		if (toupdate == 0) { return; }
		if (toupdate > 1) {
			GPHUD.getLogger().warning("Unexpected anomoly, " + toupdate + " rows to update on cookie " + cookie);
		}
		//Log.log(Log.DEBUG,"SYSTEM","Cookies","Refreshing cookie "+cookie);
		GPHUD.getDB().d("update cookies set expires=? where cookie=?", getUnixTime() + COOKIE_LIFESPAN, cookie);
	}

	/**
	 * Create a new cookie session
	 *
	 * @param avatar    Avatar object to bind to, may be null
	 * @param character Character object to bind to, may be null
	 * @param instance  Instance object to bind to, may be null
	 * @param renewable Cookie can be refreshed
	 * @return
	 */
	public static String generate(User avatar, Char character, Instance instance, boolean renewable) {
		String cookie = Tokens.generateToken();
		int expire = getUnixTime();
		expire = expire + COOKIE_LIFESPAN;
		int renewableint = 0;
		if (renewable) { renewableint = 1; }
		String id = "";
		if (avatar != null) { id += " Avatar:" + avatar.toString(); }
		if (character != null) { id += " Character:" + character.toString(); }
		GPHUD.getDB().d("insert into cookies(cookie,expires,renewable,avatarid,characterid,instanceid) values(?,?,?,?,?,?)", cookie,
				expire,
				renewableint,
				getId(avatar),
				getId(character),
				getId(instance));
		return cookie;
	}

	private static Object getId(TableRow r) {
		if (r == null) { return new NullInteger(); }
		return r.getId();
	}

	private static Object getId(User r) {
		if (r == null) { return new NullInteger(); }
		return r.getId();
	}

	public static Cookies loadOrNull(String cookie) {
		if (cookie != null) {
			try {
				return new Cookies(cookie);
			} catch (SystemException e) {} // logged out possibly, or expired and cleaned up
		}
		return null;
	}

	/**
	 * Check cookie is valid.
	 * Checks cookie hasn't expired, exists, and if necessary refreshes it.
	 *
	 * @throws SystemException if the cookie fails validation in any way.
	 */
	private void validateCookie() throws SystemException {
		try { load(); } catch (NoDataException e) { throw new SystemException("Cookie Expired!", e); }
		int expires = r.getInt("expires");
		if (expires < getUnixTime()) {
			GPHUD.getDB().d("delete from cookies where cookie=?", cookie);
			throw new SystemException("Cookie Expired!");
		}
		// if expires within 20 minutes, set expires to 30 minutes :P
		if (r.getInt("renewable") == 0) { return; }
		int now = getUnixTime();
		if (expires < (now + COOKIE_REFRESH)) {
			GPHUD.getDB().d("update cookies set expires=? where cookie=?", now + COOKIE_LIFESPAN, cookie);
		}

	}

	private void load() {
		r = GPHUD.getDB().dqone(true, "select * from cookies where cookie=?", cookie);
	}

	/**
	 * Return the avatar associated with this cookie
	 *
	 * @return The avatar, or null.
	 */
	public User getAvatar() {
		Integer avatarid = r.getInt("avatarid");
		if (avatarid == null) { return null; }
		return User.get(avatarid);
	}

	/**
	 * Set the avatar for this cookie
	 *
	 * @param avatar Avatar to set to
	 */
	public void setAvatar(User avatar) {
        /*if (avatar==null) {
            GPHUD.getDB().d("update cookies set avatarid=null where cookie=?",cookie); load(); return;
        }*/
		GPHUD.getDB().d("update cookies set avatarid=? where cookie=?", avatar.getId(), cookie);
		load();
	}

	/**
	 * Get the character for this cookie
	 *
	 * @return Character
	 */
	public Char getCharacter() {
		Integer entityid = r.getInt("characterid");
		if (entityid == null) { return null; }
		return Char.get(entityid);
	}

	/**
	 * Set the character for this cookie
	 *
	 * @param character Character to set to
	 */
	public void setCharacter(Char character) {
		Integer id = null;
		if (character != null) {
			id = character.getId();
			Instance i = getInstance();
			if (i != null) { // if has instance, character should be from it...
				if (character.getInstance() != i) {
					throw new UserException("Character is not from the selected instance");
				}
			} else {
				// if instance /is/ null, then set it :P
				setInstance(i);
			}
		}
		GPHUD.getDB().d("update cookies set characterid=? where cookie=?", id, cookie);
		load();
	}

	/**
	 * Get the instance for this cookie
	 *
	 * @return The instance
	 */
	public Instance getInstance() {
		Integer instanceid = r.getInt("instanceid");
		if (instanceid == null) { return null; }
		return Instance.get(instanceid);
	}

	/**
	 * Set an instance for this cookie
	 *
	 * @param entity Instance
	 */
	public void setInstance(Instance entity) {
		Integer id = null;
		if (entity != null) { id = entity.getId(); }
		GPHUD.getDB().d("update cookies set instanceid=? where cookie=?", id, cookie);
		load();
	}

	public String toString() { return "Avatar:" + getAvatar() + ", Instance: " + getInstance() + ", Character:" + getCharacter(); }

	public void setStateFromCookies(State st) {
		Instance instance = getInstance();
		if (instance != null) { st.setInstance(instance); }
		User av = getAvatar();
		Char ch = getCharacter();
		if (av != null) { st.setAvatar(av); }
		if (ch != null) { st.setCharacter(ch); }
		if (av == null && ch != null) { st.setAvatar(ch.getOwner()); }
		if (av != null) {
			st.cookiestring = cookie;
			st.cookie = this;
		}
	}
}
