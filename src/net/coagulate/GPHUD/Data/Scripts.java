package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.State;

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
			o.add("<a href=\"/GPHUD/scripting/edit/"+row.getInt("id")+"\">"+row.getString("name")+"</a>");
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
	public String getLinkTarget() { return "/GPHUD/scripting/edit/"+getId(); }

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
	}
}
