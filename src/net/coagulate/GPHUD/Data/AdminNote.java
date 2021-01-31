package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an administrative note.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AdminNote extends TableRow {

	protected AdminNote(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An AdminNotes representation
	 */
	@Nonnull
	public static AdminNote get(final int id) {
		return (AdminNote) factoryPut("AdminNotes",id,AdminNote::new);
	}

	/**
	 * Add a new message to the queue.
	 *
	 * @param instance   The instance to record the note against
	 * @param admin      The administrator logging the note
	 * @param target     The user to record the note against
	 * @param targetchar The target character to record the note against, optional, can be null
	 * @param note       The note to record
	 * @param adminonly  Is this note only visible to admins (else also to the player)
	 */
	public static void add(@Nonnull final Instance instance,
	                       @Nonnull final User admin,
	                       @Nonnull final User target,
	                       @Nullable final Char targetchar,
	                       @Nonnull final String note,
	                       final boolean adminonly) {
		db().d("insert into adminnotes(tds,instanceid,adminid,targetuser,targetchar,note,adminonly) values(?,?,?,?,?,?,?)",
		       UnixTime.getUnixTime(),
		       instance.getId(),
		       admin.getId(),
		       target.getId(),
		       (targetchar==null?null:targetchar.getId()),
		       note,
		       adminonly
		      );
	}

	/**
	 * Get a list of admin notes for a specific user or character.
	 * This returns admin notes against the user (i.e. no character) or against the specific character.
	 *
	 * @param instance  Instance
	 * @param user      User
	 * @param character Character
	 * @param showall   False will omit admin-only notes, true returns all notes
	 * @param toponly   If true, returns only top 3 elements
	 *
	 * @return List (possibly empty) of AdminNote elements
	 */
	@Nonnull
	public static List<Note> get(@Nonnull final Instance instance,
	                             @Nonnull final User user,
	                             @Nonnull final Char character,
	                             final boolean showall,
	                             final boolean toponly) {
		final List<Note> results=new ArrayList<>();
		for (final ResultsRow row: db().dq("select * from adminnotes where instanceid=? and ((targetuser=? and targetchar=?) or (targetuser=? and targetchar is null))"+(showall?"":" and adminonly=0")+" order by tds desc"+(toponly?" limit 0,3":""),
		                                   instance.getId(),
		                                   user.getId(),
		                                   character.getId(),
		                                   user.getId()
		                                  )) {
			results.add(resultify(row));
		}
		return results;
	}

	/**
	 * Get a list of admin notes for a specific user or any of their characters.
	 *
	 * @param instance Instance
	 * @param user     User
	 * @param showall  False will omit admin-only notes, true returns all notes
	 * @param toponly  If true, returns only top 3 elements
	 *
	 * @return List (possibly empty) of AdminNote elements
	 */
	@Nonnull
	public static List<Note> get(@Nonnull final Instance instance,
	                             @Nonnull final User user,
	                             final boolean showall,
	                             final boolean toponly) {
		final List<Note> results=new ArrayList<>();
		for (final ResultsRow row: db().dq("select * from adminnotes where instanceid=? and targetuser=?"+(showall?"":" and adminonly=0")+" order by tds desc"+(toponly?" "+"limit 0,3":""),
		                                   instance.getId(),
		                                   user.getId()
		                                  )) {
			results.add(resultify(row));
		}
		return results;
	}

	/**
	 * Return all admin notes for an instance
	 *
	 * @param instance Instance ID to get notes for
	 *
	 * @return List of AdminNote elements
	 */
	@Nonnull
	public static List<Note> get(@Nonnull final Instance instance) {
		final List<Note> results=new ArrayList<>();
		for (final ResultsRow row: db().dq("select * from adminnotes where instanceid=? order by tds desc",instance.getId())) { results.add(resultify(row)); }
		return results;
	}

	// ----- Internal Statics -----

	/**
	 * Convert a ResultsRow to an AdminNote element
	 */
	@Nonnull
	private static Note resultify(@Nonnull final ResultsRow row) {
		return new Note(row.getInt("tds"),
		                Instance.get(row.getInt("instanceid")),
		                User.get(row.getInt("adminid")),
		                User.get(row.getInt("targetuser")),
		                (row.getIntNullable("targetchar")==null?null:Char.get(row.getInt("targetchar"))),
		                row.getStringNullable("note"),
		                row.getBool("adminonly")
		);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getTableName() {
		return "adminnotes";
	}

	@Nonnull
	@Override
	public String getIdColumn() {
		return "id";
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
	}

	@Nonnull
	@Override
	public String getNameField() {
		throw new SystemImplementationException("Admin Notes don't have names");
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/notes/view/"+getId();
	}

	@javax.annotation.Nullable
	public String getKVTable() { return null; }

	@javax.annotation.Nullable
	public String getKVIdField() { return null; }

	protected int getNameCacheTime() { return 0; } // name doesn't exist yet alone get cached

	public void flushKVCache(final State st) {}

	public static class Note {
		public final int tds;
		public final Instance instance;
		public final User admin;
		public final User targetuser;
		public final Char targetchar;
		public final String note;
		public final boolean adminonly;

		public Note(final int tds,
		            final Instance instance,
		            final User admin,
		            final User targetuser,
		            final Char targetchar,
		            final String note,
		            final boolean adminonly) {
			this.tds=tds;
			this.instance=instance;
			this.admin=admin;
			this.targetuser=targetuser;
			this.targetchar=targetchar;
			this.note=note;
			this.adminonly=adminonly;
		}
	}
}
