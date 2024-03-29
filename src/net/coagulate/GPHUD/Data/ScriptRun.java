package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.User.UserInputNotFoundException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScriptRun extends TableRow {
	/**
	 * Create a script run.
	 * Invalidates any previous scripts waiting for the same user.
	 *
	 * @param code        Bytecode to preserve
	 * @param initialiser Initialiser to preserve
	 * @param respondant  Character that this execution is suspended for
	 * @return ScriptRuns object
	 */
	@Nonnull
	public static ScriptRun create(final byte[] code,final byte[] initialiser,@Nonnull final Char respondant) {
		// user can only have one respondant open, this helps us get the ID but also is down to the stupidity of the HUD,
		// and/or how painful/impractical it is to write complex IO in SL
		db().d("delete from scriptruns where respondant=?",respondant.getId());
		final int expires=UnixTime.getUnixTime()+900;
		db().d("replace into scriptruns(bytecode,initialiser,respondant,expires) values(?,?,?,?)",
		       code,
		       initialiser,
		       respondant.getId(),
		       expires);
		return get(db().dqiNotNull("select id from scriptruns where initialiser=? and respondant=? and expires=?",
		                           initialiser,
		                           respondant.getId(),
		                           expires));
	}
	
	// ---------- STATICS ----------
	
	@Nonnull
	public static ScriptRun get(final int id) {
		try {
			return (ScriptRun)factoryPut("ScriptRuns",id,ScriptRun::new);
		} catch (final NoDataException e) {
			throw new UserInputNotFoundException("Script run no longer exists",e,true);
		}
	}
	
	public ScriptRun(final int id) {
		super(id);
		if (id!=-1) {
			validate();
		}
	}
	
	public static void maintenance() {
		db().d("delete from scriptruns where expires<UNIX_TIMESTAMP()");
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getIdColumn() {
		return "id";
	}
	
	@Override
	public void validate(@Nonnull final State st) {
		if (validated) {
			return;
		}
		validate();
	}
	
	
	@Nullable
	@Override
	public String getNameField() {
		return null;
	}
	
	@Nullable
	@Override
	public String getLinkTarget() {
		return null;
	}
	
	@Nullable
	@Override
	public String getKVTable() {
		return null;
	}
	
	@Nullable
	@Override
	public String getKVIdField() {
		return null;
	}
	// ----- Internal Instance -----
	
	@Nonnull
	@Override
	public String getTableName() {
		return "scriptruns";
	}
	
	/**
	 * Get the respondant for this script run.
	 *
	 * @return The character this script was waiting for
	 */
	@Nonnull
	public Char getRespondant() {
		return Char.get(getInt("respondant"));
	}
	
	/**
	 * Gets the initialiser for this script run.
	 *
	 * @return The initialiser bytecode
	 */
	@Nonnull
	public byte[] getInitialiser() {
		return getBytes("initialiser");
	}
	
	/**
	 * Gets the bytecode for this script run
	 *
	 * @return The script's bytecode
	 */
	@Nonnull
	public byte[] getByteCode() {
		return getBytes("bytecode");
	}
}
