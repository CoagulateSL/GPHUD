package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
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

	/**
	 * Creates a list of all scripts at an instance.
	 * TODO break this function down.
	 *
	 * @param instance Instance to query
	 *
	 * @return a Table
	 */
	@Nonnull
	public static Table getTable(@Nonnull final Instance instance) {
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

	/**
	 * Create a new empty script by name.
	 *
	 * @param st         State
	 * @param scriptname Name of script
	 *
	 * @throws UserInputDuplicateValueException If a script with this name already exists
	 */
	public static void create(@Nonnull final State st,
	                          @Nonnull final String scriptname) {
		final int existing=GPHUD.getDB().dqinn("select count(*) from scripts where name like ? and instanceid=?",scriptname,st.getInstance().getId());
		if (existing>0) { throw new UserInputDuplicateValueException("script with that name already exists"); }
		GPHUD.getDB().d("insert into scripts(instanceid,name) values(?,?)",st.getInstance().getId(),scriptname);
	}

	@Nonnull
	public static Scripts get(final int id) {
		return (Scripts) factoryPut("Scripts",id,new Scripts(id));
	}

	/**
	 * Get all script objects for an instance
	 *
	 * @param instance Instance to get
	 *
	 * @return Set of Scripts
	 */
	@Nonnull
	public static Set<Scripts> getScripts(@Nonnull final Instance instance) {
		final Set<Scripts> scripts=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB().dq("select id from scripts where instanceid=?",instance.getId())) {
			scripts.add(new Scripts(row.getInt("id")));
		}
		return scripts;
	}

	/**
	 * Find a script by name.
	 *
	 * @param st         State
	 * @param scriptname Name of script
	 *
	 * @return The script object
	 *
	 * @throws UserInputLookupFailureException If the script does not exist
	 */
	@Nonnull
	public static Scripts find(@Nonnull final State st,
	                           @Nonnull final String scriptname) {
		try {
			final int id=GPHUD.getDB().dqinn("select id from scripts where instanceid=? and name like ?",st.getInstance().getId(),scriptname);
			return new Scripts(id);
		}
		catch (NoDataException e) {
			throw new UserInputLookupFailureException("Script by name "+scriptname+" does not exist",e);
		}
	}

	/**
	 * Find a script by name.
	 *
	 * @param st         State
	 * @param scriptname Name of script
	 *
	 * @return The script object or null if it does not exist
	 */
	@Nullable
	public static Scripts findNullable(@Nonnull final State st,
	                                   @Nonnull final String scriptname) {
		try { return find(st,scriptname); } catch (@Nonnull final UserInputLookupFailureException e) {return null; }
	}

	/**
	 * Create a dropdown list of all the scripts in the instance
	 *
	 * @param st       State
	 * @param listname Name of the list control
	 *
	 * @return A DropDownList
	 */
	@Nonnull
	public static DropDownList getList(@Nonnull final State st,
	                                   @Nonnull final String listname) {
		final DropDownList list=new DropDownList(listname);
		for (final ResultsRow row: GPHUD.getDB().dq("select id,name from scripts where instanceid=?",st.getInstance().getId())) {
			list.add(""+row.getIntNullable("id"),row.getStringNullable("name"));
		}
		return list;
	}

	/**
	 * Get the source for this script
	 *
	 * @return The String source code
	 */
	@Nonnull
	public String getSource() {
		String script=getStringNullable("source");
		if (script==null) { script=""; }
		return script;
	}

	/**
	 * Set the source code for this script
	 *
	 * @param scriptsource String source code
	 */
	public void setSource(@Nonnull final String scriptsource) {
		validate();
		final String s=getSource();
		if (scriptsource.equals(s)) { return; }
		int version=getSourceVersion();
		version++;
		d("update scripts set source=?, sourceversion=? where id=?",scriptsource,version,getId());
	}

	/**
	 * Get the version number of the source code
	 *
	 * @return source code version number
	 */
	public int getSourceVersion() {
		final Integer a=getIntNullable("sourceversion");
		if (a==null) { return 0; }
		return a;
	}

	/**
	 * Get the version number of the byte code
	 *
	 * @return Byte code version number
	 */
	public int getByteCodeVersion() {
		final Integer a=getIntNullable("bytecodeversion");
		if (a==null) { return 0; }
		return a;
	}

	@Nonnull
	@Override
	public String getIdColumn() { return "id"; }

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

	/**
	 * Set the compiled bytecode for this script
	 *
	 * @param toByteCode Bytecode
	 * @param version    Version number
	 */
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

	/**
	 * Get the compiled bytecode for this script
	 *
	 * @return The compiled bytecode (potentially an empty array)
	 */
	@Nonnull
	public byte[] getByteCode() {
		validate();
		return getBytes("bytecode");
	}
}
