package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScriptRuns extends TableRow {
	public ScriptRuns(final int id) {
		super(id);
	}

	@Nonnull
	public static ScriptRuns create(final byte[] code,
	                                final Byte[] initialiser,
	                                @Nonnull final Char respondant) {
		// user can only have one respondant open, this helps us get the ID but also is down to the stupidity of the HUD,
		// and/or how painful/impractical it is to write complex IO in SL
		GPHUD.getDB().d("delete from scriptruns where respondant=?",respondant.getId());
		final int expires=UnixTime.getUnixTime()+900;
		GPHUD.getDB().d("insert into scriptruns(bytecode,initialiser,respondant,expires) values(?,?,?,?)",code,initialiser,respondant.getId(),expires);
		return get(GPHUD.getDB().dqinn("select id from scriptruns where initialiser=? and respondant=? and expires=?",initialiser,respondant.getId(),expires));
	}

	@Nonnull
	public static ScriptRuns get(final int id) {
		return (ScriptRuns) factoryPut("ScriptRuns",id,new ScriptRuns(id));
	}

	@Nonnull
	@Override
	public String getIdColumn() { return "id"; }

	@Override
	public void validate(final State st) {
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

	@Nonnull
	public byte[] getInitialiser() {
		return getBytes("initialiser");
	}

	@Nonnull
	public byte[] getByteCode() {
		return getBytes("bytecode");
	}
}
