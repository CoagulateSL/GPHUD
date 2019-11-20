package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.State;

import java.util.HashSet;
import java.util.Set;

public class Scripts extends TableRow {
	public Scripts(int id) {
		super(id);
	}

	public static Table getTable(Instance instance) {
		Results rows = GPHUD.getDB().dq("select id,name,sourceversion,bytecodeversion from scripts order by id asc");
		Table o=new Table();
		o.add(new HeaderRow().add("Name").add("Version").add("Compiled Version"));
		for (ResultsRow row:rows) {
			o.openRow();
			o.add("<a href=\"/GPHUD/configuration/scripting/edit/"+row.getInt("id")+"\">"+row.getString("name")+"</a>");
			Integer sourceversion=row.getInt("sourceversion");
			Integer bytecodeversion=row.getInt("bytecodeversion");
			if (sourceversion==bytecodeversion) {
				o.add((sourceversion == null ? "None" : "" + sourceversion));
				o.add((bytecodeversion == null ? "None" : "" + bytecodeversion));
			} else {
				o.add("<font color=red>"+(sourceversion == null ? "None" : "" + sourceversion)+"</font>");
				o.add("<font color=red>"+(bytecodeversion == null ? "None" : "" + bytecodeversion)+"</font>");
			}
		}
		return o;
	}

	public static void create(State st, String scriptname) {
		Integer existing=GPHUD.getDB().dqi(true,"select count(*) from scripts where name like ? and instanceid=?",scriptname,st.getInstance().getId());
		if (existing>0) { throw new UserException("script with that name already exists"); }
		GPHUD.getDB().d("insert into scripts(instanceid,name) values(?,?)",st.getInstance().getId(),scriptname);
	}

	public static Scripts get(int id) {
		return (Scripts) factoryPut("Scripts", id, new Scripts(id));
	}

	public static Set<Scripts> getScript(Instance instance) {
		Set<Scripts> scripts=new HashSet<>();
		for (ResultsRow row:GPHUD.getDB().dq("select id from scripts where instanceid=?",instance.getId())) {
			scripts.add(new Scripts(row.getInt("id")));
		}
		return scripts;
	}

	public static Scripts find(State st, String commandname) {
		Integer id=GPHUD.getDB().dqi(true,"select id from scripts where instanceid=? and name like ?",st.getInstance().getId(),commandname);
		return new Scripts(id);
	}
	public static Scripts findOrNull(State st, String commandname) {
		Integer id=GPHUD.getDB().dqi(false,"select id from scripts where instanceid=? and name like ?",st.getInstance().getId(),commandname);
		if (id==null) { return null; }
		return new Scripts(id);
	}

	public static DropDownList getList(State st, String listname) {
		DropDownList list=new DropDownList(listname);
		for (ResultsRow row:GPHUD.getDB().dq("select id,name from scripts where instanceid=?",st.getInstance().getId())) {
			list.add(""+row.getInt("id"),row.getString("name"));
		}
		return list;
	}

	public String getSource() {
		String script=getString("source");
		if (script==null) { script=""; }
		return  script;
	}
	public int getSourceVersion() {
		Integer a = getInt("sourceversion");
		if (a == null) { return 0; }
		return a;
	}
	public int getByteCodeVersion() {
		Integer a=getInt("bytecodeversion");
		if (a==null) { return 0; }
		return a;
	}

	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Script / State Instance mismatch"); }
	}

	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Override
	public String getNameField() { return "name"; }

	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/scripting/edit/"+getId(); }

	@Override
	protected int getNameCacheTime() { return 600; }

	@Override
	public String getKVTable() {
		return null;
	}

	@Override
	public String getKVIdField() {
		return null;
	}

	@Override
	public String getTableName() {
		return "scripts";
	}

	public void setSource(String scriptsource) {
		validate();
		String s=getSource();
		if (scriptsource.equals(s)) { return; }
		int version=getSourceVersion();
		version++;
		d("update scripts set source=?, sourceversion=? where id=?",scriptsource,version,getId());
	}

	public void setBytecode(Byte[] toByteCode, int version) {
		validate();
		d("update scripts set bytecode=?, bytecodeversion=? where id=?",toByteCode,version,getId());
		if (GPHUD.DEV) {
			byte[] compareto = getByteCode();
			if (compareto.length != toByteCode.length) {
				throw new SystemException("Length mismatch, wrote " + toByteCode.length + " and read " + compareto.length);
			}
			for (int i = 0; i < compareto.length; i++) {
				if (compareto[i] != toByteCode[i]) {
					throw new SystemException("Difference at " + i + " - we wrote " + toByteCode[i] + " and read " + compareto[i]);
				}
			}
		}
	}

	public byte[] getByteCode() {
		validate();
		return getBytes("bytecode");
	}

	public static void test() {
		byte[] b=new byte[255];
		for (int i=0;i<256;i++) { b[i]=((byte)(0xff & i)); }
		GPHUD.getDB().d("insert into scripts(instanceid,name,bytecode) values(?,?,?)",-1," ENCODING TEST ",b);
		byte[] out=GPHUD.getDB().dqbyte(true,"select bytecode from scripts where instanceid=? and name=?",-1," ENCODING TEST ");
		GPHUD.getDB().d("delete from scripts where instanceid=? and name=?",-1," ENCODING TEST ");
		for (int i=0;i<256;i++) {
			if (b[i] != out[i]) {
				throw new SystemException("Comparison error on " + i + " - " + b[i] + " gave " + out[i]);
			}
		}
	}
}
