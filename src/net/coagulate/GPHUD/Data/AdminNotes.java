package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an administrative note.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AdminNotes extends TableRow {

	protected AdminNotes(int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An AdminNotes representation
	 */
	public static AdminNotes get(int id) {
		return (AdminNotes) factoryPut("AdminNotes", id, new AdminNotes(id));
	}

	/**
	 * Add a new message to the queue.
	 *
	 * @param instance The instance to record the note against
	 * @param admin The administrator logging the note
	 * @param target The user to record the note against
	 * @param targetchar The target character to record the note against, optional, can be null
	 * @param note The note to record
	 * @param adminonly Is this note only visible to admins (else also to the player)
	 */
	public static void add(@NotNull Instance instance, @NotNull User admin, @NotNull User target, @Nullable Char targetchar, @NotNull String note, boolean adminonly) {
		GPHUD.getDB().d("insert into adminnotes(tds,instanceid,adminid,targetuser,targetchar,note,adminonly) values(?,?,?,?,?,?,?)",
				UnixTime.getUnixTime(),
				instance.getId(),
				admin.getId(),
				target.getId(),
				(targetchar==null?null:targetchar.getId()),
				note,
				adminonly);
	}

	public static class AdminNote {
		public int tds;
		public Instance instance;
		public User admin;
		public User targetuser;
		public Char targetchar;
		public String note;
		public boolean adminonly;
		public AdminNote(int tds,Instance instance, User admin, User targetuser, Char targetchar,String note,boolean adminonly) {
			this.tds=tds;
			this.instance=instance;
			this.admin=admin;
			this.targetuser=targetuser;
			this.targetchar=targetchar;
			this.note=note;
			this.adminonly=adminonly;
		}
	}

	public static List<AdminNote> get(Instance instance, User user, Char character, boolean showall, boolean toponly) {
		List<AdminNote> results=new ArrayList<>();
		for (ResultsRow row:GPHUD.getDB().dq("select * from adminnotes where instanceid=? and ((targetuser=? and targetchar=?) or (targetuser=? and targetchar is null))"+
						(showall?"":" and adminonly=0")+" order by tds desc"+
						(toponly?" limit 0,3":""),
				instance.getId(),
				user.getId(),
				character.getId(),
				user.getId())) {
			results.add(resultify(row));
		}
		return results;
	}
	public static List<AdminNote> get(Instance instance, User user, boolean showall, boolean toponly) {
		List<AdminNote> results=new ArrayList<>();
		for (ResultsRow row:GPHUD.getDB().dq("select * from adminnotes where instanceid=? and targetuser=?"+
						(showall?"":" and adminonly=0")+" order by tds desc"+
						(toponly?" limit 0,3":""),
				instance.getId(),
				user.getId())) {
			results.add(resultify(row));
		}
		return results;
	}
	public static List<AdminNote> get(Instance instance) {
		List<AdminNote> results=new ArrayList<>();
		for (ResultsRow row:GPHUD.getDB().dq("select * from adminnotes where instanceid=? order by tds desc",
				instance.getId())) results.add(resultify(row));
		return results;
	}
	private static AdminNote resultify(ResultsRow row) {
		return new AdminNote(
				row.getInt("tds"),
				Instance.get(row.getInt("instanceid")),
				User.get(row.getInt("adminid")),
				User.get(row.getInt("targetuser")),
				(row.getInt("targetchar")==null?null:Char.get(row.getInt("targetchar"))),
				row.getString("note"),
				row.getBool("adminonly")
		);
	}

	@Override
	public String getTableName() {
		return "adminnotes";
	}

	@Override
	public String getIdField() {
		return "id";
	}

	@Override
	public String getNameField() {
		throw new SystemException("Admin Notes don't have names");
	}

	@Override
	public String getLinkTarget() {
		return "/notes/view/" + getId();
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
