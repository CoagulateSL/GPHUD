package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScriptRuns extends TableRow {
	public ScriptRuns(int id) {
		super(id);
	}

	@Nonnull
	public static ScriptRuns create(byte[] code, Byte[] initialiser, @Nonnull Char respondant) {
		// user can only have one respondant open, this helps us get the ID but also is down to the stupidity of the HUD,
		// and/or how painful/impractical it is to write complex IO in SL
		GPHUD.getDB().d("delete from scriptruns where respondant=?",respondant.getId());
		int expires=UnixTime.getUnixTime()+900;
		GPHUD.getDB().d("insert into scriptruns(bytecode,initialiser,respondant,expires) values(?,?,?,?)",code,initialiser,respondant.getId(), expires);
		return get(GPHUD.getDB().dqi(true,"select id from scriptruns where initialiser=? and respondant=? and expires=?",initialiser,respondant.getId(),expires));
	}

	@Nonnull
	public static ScriptRuns get(int id) {
		return (ScriptRuns) factoryPut("ScriptRuns", id, new ScriptRuns(id));
	}

	@Nonnull
	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
	}


	@Nullable
	@Override
	public String getNameField() { return null; }

	@Nullable
	@Override
	public String getLinkTarget() { return null; }

	@Override
	protected int getNameCacheTime() { return 0; }

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

	@Nonnull
	@Override
	public String getTableName() {
		return "scriptruns";
	}

	@Nonnull
	public Char getRespondant() {
		return Char.get(getInt("respondant"));
	}

	@Nullable
	public byte[] getInitialiser() {
		return getBytes("initialiser");
	}
	@Nullable
	public byte[] getByteCode() {
		return getBytes("bytecode");
	}
}
