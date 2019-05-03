package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

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
	static void add(Char targetchar, int expires, JSONObject message) {
		GPHUD.getDB().d("insert into messages(characterid,expires,json) values(?,?,?)", targetchar.getId(), expires, message.toString());
	}

	/**
	 * Convenience method to queue a message for hours
	 *
	 * @param c             Character to queue for
	 * @param lifespanhours Hours to keep message
	 * @param json          Message
	 */
	public static void queueHours(Char c, int lifespanhours, JSONObject json) { queue(c, lifespanhours * 60 * 60, json); }

	/**
	 * Convenience method to queue a message for days
	 *
	 * @param c            Character to queue for
	 * @param lifespandays Days to keep message
	 * @param json         Message
	 */
	public static void queueDays(Char c, int lifespandays, JSONObject json) { queueHours(c, lifespandays * 24, json); }

	/**
	 * Convenience method to queue a message for a given ammount of time
	 *
	 * @param c               Character to queue for
	 * @param lifespanseconds How many seconds until message expires
	 * @param json            Message
	 */
	public static void queue(Char c, int lifespanseconds, JSONObject json) {
		add(c, UnixTime.getUnixTime() + lifespanseconds, json);
	}

	/**
	 * Get the next message, active or not, soonest to expire
	 *
	 * @param c Character to get a message for
	 * @return Next message for the character, or null
	 */
	public static Message getNextMessage(Char c) {
		Integer id = GPHUD.getDB().dqi(false, "select messageid from messages where characterid=? order by expires  limit 0,1", c.getId());
		if (id == null) { return null; }
		return Message.get(id);
	}

	/**
	 * Count the number of messages queued for a character.
	 *
	 * @param c Character
	 * @return Number of messages
	 */
	public static int count(Char c) {
		Integer id = GPHUD.getDB().dqi(true, "select count(*) from messages where characterid=?", c.getId());
		return id;
	}

	/**
	 * Get the currently active message
	 *
	 * @param c Character to get active message for
	 * @return The message, or null if there isn't one.
	 */
	public static Message getActiveMessage(Char c) {
		Integer id = GPHUD.getDB().dqi(false, "select messageid from messages where characterid=? and expires=0", c.getId());
		if (id == null) { return null; }
		return Message.get(id);
	}

	@Override
	public String getTableName() {
		return "messages";
	}

	@Override
	public String getIdField() {
		return "messageid";
	}

	@Override
	public String getNameField() {
		throw new SystemException("Messages don't have names");
	}

	@Override
	public String getLinkTarget() {
		return "/messages/view/" + getId();
	}

	/**
	 * Get the JSON payload for this message
	 *
	 * @return JSONObject payload
	 */
	public String getJSON() {
		return dqs(true, "select json from messages where messageid=?", getId());
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

	public String getKVTable() { return null; }

	public String getKVIdField() { return null; }

	public void flushKVCache(State st) {}

	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
	}

	protected int getNameCacheTime() { return 0; } // name doesn't exist yet alone get cached
}
