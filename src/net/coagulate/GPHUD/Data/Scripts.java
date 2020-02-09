package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Scripts extends TableRow {
	public Scripts(final int id) {
		super(id);
	}

	@Nonnull
	public static Table getTable(final Instance instance) {
		final Results rows=GPHUD.getDB().dq("select id,name,sourceversion,bytecodeversion from scripts where instanceid=? order by id asc",instance.getId());
		final Table o=new Table();
		o.add(new HeaderRow().add("Name").add("Version").add("Compiled Version"));
		for (final ResultsRow row: rows) {
			o.openRow();
			o.add("<a href=\"/GPHUD/configuration/scripting/edit/"+row.getIntNullable("id")+"\">"+row.getStringNullable("name")+"</a>");
			final Integer sourceversion=row.getIntNullable("sourceversion");
			final Integer bytecodeversion=row.getIntNullable("bytecodeversion");
			if (Objects.equals(sourceversion,bytecodeversion)) {
				o.add((sourceversion==null?"None":""+sourceversion));
				o.add((bytecodeversion==null?"None":""+bytecodeversion));
			}
			else {
				o.add("<font color=red>"+(sourceversion==null?"None":""+sourceversion)+"</font>");
				o.add("<font color=red>"+(bytecodeversion==null?"None":""+bytecodeversion)+"</font>");
			}
		}
		return o;
	}

	public static void create(@Nonnull final State st,
	                          final String scriptname) {
		final int existing=GPHUD.getDB().dqinn("select count(*) from scripts where name like ? and instanceid=?",scriptname,st.getInstance().getId());
		if (existing>0) { throw new UserInputDuplicateValueException("script with that name already exists"); }
		GPHUD.getDB().d("insert into scripts(instanceid,name) values(?,?)",st.getInstance().getId(),scriptname);
	}

	@Nonnull
	public static Scripts get(final int id) {
		return (Scripts) factoryPut("Scripts",id,new Scripts(id));
	}

	@Nonnull
	public static Set<Scripts> getScript(@Nonnull final Instance instance) {
		final Set<Scripts> scripts=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB().dq("select id from scripts where instanceid=?",instance.getId())) {
			scripts.add(new Scripts(row.getInt("id")));
		}
		return scripts;
	}

	@Nonnull
	public static Scripts find(@Nonnull final State st,
	                           final String commandname) {
		final int id=GPHUD.getDB().dqinn("select id from scripts where instanceid=? and name like ?",st.getInstance().getId(),commandname);
		return new Scripts(id);
	}

	@Nullable
	public static Scripts findOrNull(@Nonnull final State st,
	                                 final String commandname) {
		try { return find(st,commandname); } catch (@Nonnull final NoDataException e) {return null; }
	}

	@Nonnull
	public static DropDownList getList(@Nonnull final State st,
	                                   final String listname) {
		final DropDownList list=new DropDownList(listname);
		for (final ResultsRow row: GPHUD.getDB().dq("select id,name from scripts where instanceid=?",st.getInstance().getId())) {
			list.add(""+row.getIntNullable("id"),row.getStringNullable("name"));
		}
		return list;
	}

	@Nonnull
	public String getSource() {
		String script=getStringNullable("source");
		if (script==null) { script=""; }
		return script;
	}

	public void setSource(@Nonnull final String scriptsource) {
		validate();
		final String s=getSource();
		if (scriptsource.equals(s)) { return; }
		int version=getSourceVersion();
		version++;
		d("update scripts set source=?, sourceversion=? where id=?",scriptsource,version,getId());
	}

	public int getSourceVersion() {
		final Integer a=getIntNullable("sourceversion");
		if (a==null) { return 0; }
		return a;
	}

	public int getByteCodeVersion() {
		final Integer a=getIntNullable("bytecodeversion");
		if (a==null) { return 0; }
		return a;
	}

	@Nonnull
	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Script / State Instance mismatch");
		}
	}

	@Nullable
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Nonnull
	@Override
	public String getNameField() { return "name"; }

	@Nonnull
	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/scripting/edit/"+getId(); }

	@Override
	protected int getNameCacheTime() { return 600; }

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
		return "scripts";
	}

	public void setBytecode(@Nonnull final Byte[] toByteCode,
	                        final int version) {
		validate();
		d("update scripts set bytecode=?, bytecodeversion=? where id=?",toByteCode,version,getId());
		if (GPHUD.DEV) {
			final byte[] compareto=getByteCode();
			if (compareto.length!=toByteCode.length) {
				throw new SystemImplementationException("Length mismatch, wrote "+toByteCode.length+" and read "+compareto.length);
			}
			for (int i=0;i<compareto.length;i++) {
				if (compareto[i]!=toByteCode[i]) {
					throw new SystemImplementationException("Difference at "+i+" - we wrote "+toByteCode[i]+" and read "+compareto[i]);
				}
			}
		}
	}

	@Nonnull
	public byte[] getByteCode() {
		validate();
		return getBytes("bytecode");
	}

	/*
	public static void test() {
		final byte[] b=new byte[255];
		for (int i=0;i<256;i++) { b[i]=((byte)(0xff & i)); }
		GPHUD.getDB().d("insert into scripts(instanceid,name,bytecode) values(?,?,?)",-1," ENCODING TEST ",b);
		final byte[] out=GPHUD.getDB().dqbyte("select bytecode from scripts where instanceid=? and name=?",-1," ENCODING TEST ");
		GPHUD.getDB().d("delete from scripts where instanceid=? and name=?",-1," ENCODING TEST ");
		for (int i=0;i<256;i++) {
			if (b[i] != out[i]) {
				throw new SystemImplementationException("Comparison error on " + i + " - " + b[i] + " gave " + out[i]);
			}
		}
	}*/
}
