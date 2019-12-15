package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.SystemException;
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

	protected Message(int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	@Nonnull
	public static Message get(int id) {
		return (Message) factoryPut("Message", id, new Message(id));
	}

	/**
	 * Add a new message to the queue.
	 *
	 * @param targetchar Character to enqueue the message for
	 * @param expires    UnixTime after which the message will not be activatable (and will be discarded)
	 * @param message    The JSONObject message to enqueue
	 */
	static void add(@Nonnull Char targetchar, int expires, @Nonnull JSONObject message) {
		GPHUD.getDB().d("insert into messages(characterid,expires,json) values(?,?,?)", targetchar.getId(), expires, message.toString());
	}

	/**
	 * Convenience method to queue a message for hours
	 *
	 * @param c             Character to queue for
	 * @param lifespanhours Hours to keep message
	 * @param json          Message
	 */
	public static void queueHours(@Nonnull Char c, int lifespanhours, @Nonnull JSONObject json) { queue(c, lifespanhours * 60 * 60, json); }

	/**
	 * Convenience method to queue a message for days
	 *
	 * @param c            Character to queue for
	 * @param lifespandays Days to keep message
	 * @param json         Message
	 */
	public static void queueDays(@Nonnull Char c, int lifespandays, @Nonnull JSONObject json) { queueHours(c, lifespandays * 24, json); }

	/**
	 * Convenience method to queue a message for a given ammount of time
	 *
	 * @param c               Character to queue for
	 * @param lifespanseconds How many seconds until message expires
	 * @param json            Message
	 */
	public static void queue(@Nonnull Char c, int lifespanseconds, @Nonnull JSONObject json) {
		add(c, UnixTime.getUnixTime() + lifespanseconds, json);
	}

	/**
	 * Get the next message, active or not, soonest to expire
	 *
	 * @param c Character to get a message for
	 * @return Next message for the character, or null
	 */
	@Nullable
	public static Message getNextMessage(@Nonnull Char c) {
		try {
			Integer id = GPHUD.getDB().dqi("select messageid from messages where characterid=? order by expires  limit 0,1", c.getId());
			return Message.get(id);
		} catch (NoDataException e) { return null; }
	}

	/**
	 * Count the number of messages queued for a character.
	 *
	 * @param c Character
	 * @return Number of messages
	 */
	public static int count(@Nonnull Char c) {
		return GPHUD.getDB().dqi( "select count(*) from messages where characterid=?", c.getId());
	}

	/**
	 * Get the currently active message
	 *
	 * @param c Character to get active message for
	 * @return The message, or null if there isn't one.
	 */
	@Nullable
	public static Message getActiveMessage(@Nonnull Char c) {
		try {
			Integer id = GPHUD.getDB().dqi("select messageid from messages where characterid=? and expires=0", c.getId());
			return Message.get(id);
		} catch (NoDataException e) { return null; }
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "messages";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "messageid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		throw new SystemException("Messages don't have names");
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/messages/view/" + getId();
	}

	/**
	 * Get the JSON payload for this message
	 *
	 * @return JSONObject payload
	 */
	@Nonnull
	public String getJSON() {
		return dqsnn( "select json from messages where messageid=?", getId());
	}

	/**
	 * Set this message as active (in progress).
	 */
	public void setActive() {
		d("update messages set expires=0 where messageid=?", getId());
	}

	/**
	 * Delete the message.
	 */
	public void delete() {
		d("delete from messages where messageid=?", getId());
	}

	@Nullable
	public String getKVTable() { return null; }

	@Nullable
	public String getKVIdField() { return null; }

	public void flushKVCache(State st) {}

	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
	}

	protected int getNameCacheTime() { return 0; } // name doesn't exist yet alone get cached
}
