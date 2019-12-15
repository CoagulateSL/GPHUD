package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an administrative note.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AdminNotes extends TableRow {

	protected AdminNotes(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An AdminNotes representation
	 */
	@Nonnull
	public static AdminNotes get(final int id) {
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
	public static void add(@NotNull final Instance instance, @NotNull final User admin, @NotNull final User target, @Nullable final Char targetchar, @NotNull final String note, final boolean adminonly) {
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
		public final int tds;
		public final Instance instance;
		public final User admin;
		public final User targetuser;
		public final Char targetchar;
		public final String note;
		public final boolean adminonly;
		public AdminNote(final int tds, final Instance instance, final User admin, final User targetuser, final Char targetchar, final String note, final boolean adminonly) {
			this.tds=tds;
			this.instance=instance;
			this.admin=admin;
			this.targetuser=targetuser;
			this.targetchar=targetchar;
			this.note=note;
			this.adminonly=adminonly;
		}
	}

	@Nonnull
	public static List<AdminNote> get(@Nonnull final Instance instance, @Nonnull final User user, @Nonnull final Char character, final boolean showall, final boolean toponly) {
		final List<AdminNote> results=new ArrayList<>();
		for (final ResultsRow row:GPHUD.getDB().dq("select * from adminnotes where instanceid=? and ((targetuser=? and targetchar=?) or (targetuser=? and targetchar is null))"+
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
	@Nonnull
	public static List<AdminNote> get(@Nonnull final Instance instance, @Nonnull final User user, final boolean showall, final boolean toponly) {
		final List<AdminNote> results=new ArrayList<>();
		for (final ResultsRow row:GPHUD.getDB().dq("select * from adminnotes where instanceid=? and targetuser=?"+
						(showall?"":" and adminonly=0")+" order by tds desc"+
						(toponly?" limit 0,3":""),
				instance.getId(),
				user.getId())) {
			results.add(resultify(row));
		}
		return results;
	}
	@Nonnull
	public static List<AdminNote> get(@Nonnull final Instance instance) {
		final List<AdminNote> results=new ArrayList<>();
		for (final ResultsRow row:GPHUD.getDB().dq("select * from adminnotes where instanceid=? order by tds desc",
				instance.getId())) results.add(resultify(row));
		return results;
	}
	@Nonnull
	private static AdminNote resultify(@Nonnull final ResultsRow row) {
		return new AdminNote(
				row.getInt("tds"),
				Instance.get(row.getInt("instanceid")),
				User.get(row.getInt("adminid")),
				User.get(row.getInt("targetuser")),
				(row.getIntNullable("targetchar")==null?null:Char.get(row.getInt("targetchar"))),
				row.getStringNullable("note"),
				row.getBoolNoNull("adminonly")
		);
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "adminnotes";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "id";
	}

	@Nonnull
	@Override
	public String getNameField() {
		throw new SystemImplementationException("Admin Notes don't have names");
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/notes/view/" + getId();
	}


	@javax.annotation.Nullable
	public String getKVTable() { return null; }

	@javax.annotation.Nullable
	public String getKVIdField() { return null; }

	public void flushKVCache(final State st) {}

	public void validate(final State st) throws SystemException {
		if (validated) { return; }
		validate();
	}

	protected int getNameCacheTime() { return 0; } // name doesn't exist yet alone get cached
}
