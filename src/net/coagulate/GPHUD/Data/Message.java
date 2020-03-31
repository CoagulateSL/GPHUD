package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a queued message.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Message extends TableRow {

	protected Message(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return A Region representation
	 */
	@Nonnull
	public static Message get(final int id) {
		return (Message) factoryPut("Message",id,new Message(id));
	}

	/**
	 * Convenience method to queue a message for hours
	 *
	 * @param c             Character to queue for
	 * @param lifespanhours Hours to keep message
	 * @param json          Message
	 */
	public static void queueHours(@Nonnull final Char c,
	                              final int lifespanhours,
	                              @Nonnull final JSONObject json) { queue(c,lifespanhours*60*60,json); }

	/**
	 * Convenience method to queue a message for days
	 *
	 * @param c            Character to queue for
	 * @param lifespandays Days to keep message
	 * @param json         Message
	 */
	public static void queueDays(@Nonnull final Char c,
	                             final int lifespandays,
	                             @Nonnull final JSONObject json) { queueHours(c,lifespandays*24,json); }

	/**
	 * Convenience method to queue a message for a given ammount of time
	 *
	 * @param c               Character to queue for
	 * @param lifespanseconds How many seconds until message expires
	 * @param json            Message
	 */
	public static void queue(@Nonnull final Char c,
	                         final int lifespanseconds,
	                         @Nonnull final JSONObject json) {
		add(c,UnixTime.getUnixTime()+lifespanseconds,json);
	}

	/**
	 * Get the next message, active or not, soonest to expire
	 *
	 * @param c Character to get a message for
	 *
	 * @return Next message for the character, or null
	 */
	@Nullable
	public static Message getNextMessage(@Nonnull final Char c) {
		try {
			final int id=GPHUD.getDB().dqinn("select messageid from messages where characterid=? order by expires  limit 0,1",c.getId());
			return Message.get(id);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	/**
	 * Count the number of messages queued for a character.
	 *
	 * @param c Character
	 *
	 * @return Number of messages
	 */
	public static int count(@Nonnull final Char c) {
		return GPHUD.getDB().dqinn("select count(*) from messages where characterid=?",c.getId());
	}

	/**
	 * Get the currently active message
	 *
	 * @param c Character to get active message for
	 *
	 * @return The message, or null if there isn't one.
	 */
	@Nullable
	public static Message getActiveMessage(@Nonnull final Char c) {
		try {
			final int id=GPHUD.getDB().dqinn("select messageid from messages where characterid=? and expires=0",c.getId());
			return Message.get(id);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	// ----- Internal Statics -----

	/**
	 * Add a new message to the queue.
	 *
	 * @param targetchar Character to enqueue the message for
	 * @param expires    UnixTime after which the message will not be activatable (and will be discarded)
	 * @param message    The JSONObject message to enqueue
	 */
	static void add(@Nonnull final Char targetchar,
	                final int expires,
	                @Nonnull final JSONObject message) {
		GPHUD.getDB().d("insert into messages(characterid,expires,json) values(?,?,?)",targetchar.getId(),expires,message.toString());
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getTableName() {
		return "messages";
	}

	@Nonnull
	@Override
	public String getIdColumn() {
		return "messageid";
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
	}

	@Nonnull
	@Override
	public String getNameField() {
		throw new SystemImplementationException("Messages don't have names");
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/messages/view/"+getId();
	}

	@Nullable
	public String getKVTable() { return null; }

	@Nullable
	public String getKVIdField() { return null; }

	protected int getNameCacheTime() { return 0; } // name doesn't exist yet alone get cached

	/**
	 * Get the JSON payload for this message
	 *
	 * @return JSONObject payload
	 */
	@Nonnull
	public String getJSON() {
		return dqsnn("select json from messages where messageid=?",getId());
	}

	/**
	 * Set this message as active (in progress).
	 */
	public void setActive() {
		d("update messages set expires=0 where messageid=?",getId());
	}

	/**
	 * Delete the message.
	 */
	public void delete() {
		d("delete from messages where messageid=?",getId());
	}

	public void flushKVCache(final State st) {}
}
