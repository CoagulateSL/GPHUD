package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class Landmarks extends TableRow {
	public Landmarks(int id) {
		super(id);
	}

	@Nonnull
	public static Landmarks get(int id) {
		return (Landmarks) factoryPut("Landmarks", id, new Landmarks(id));
	}

	@Nonnull
	public static Set<Landmarks> getAll(@Nonnull Instance instance) {
		Set<Landmarks> results=new HashSet<>();
		for (ResultsRow row:GPHUD.getDB().dq("select landmarks.id as id from landmarks,regions where landmarks.regionid=regions.regionid and regions.instanceid=?",instance.getId())) {
			results.add(get(row.getIntNullable("id")));
		}
		return results;
	}

	@Nonnull
	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
	}

	public static void obliterate(@Nonnull Instance instance, String name) {
		GPHUD.getDB().d("delete landmarks from landmarks inner join regions on landmarks.regionid=regions.regionid where regions.instanceid=? and landmarks.name like ?",instance.getId(),name);
	}

	@Nullable
	public static Landmarks find(@Nonnull Instance instance, String name) {
		try {
			Integer id=GPHUD.getDB().dqi("select landmarks.id from landmarks,regions where landmarks.regionid=regions.regionid and regions.instanceid=? and landmarks.name like ?",instance.getId(),name);
			return new Landmarks(id);
		} catch (NoDataException e) {return null;}
	}

	public static void create(@Nonnull Region region, String name, float x, float y, float z, float lax, float lay, float laz) {
		obliterate(region.getInstance(),name);
		GPHUD.getDB().d("insert into landmarks(regionid,name,x,y,z,lookatx,lookaty,lookatz) values(?,?,?,?,?,?,?,?)",region.getId(),name,x,y,z,lax,lay,laz);
	}

	@Nonnull
	public String getHUDRepresentation(boolean allowretired) {
		String tp="";
		tp+=getRegion(allowretired).getGlobalCoordinates();
		tp+="|";
		tp+=getCoordinates();
		tp+="|";
		tp+=getLookAt();
		return tp;
	}

	@Nonnull
	public String getCoordinates() {
		return "<"+getFloat("x")+","+getFloat("y")+","+getFloat("z")+">";
	}
	@Nonnull
	public String getLookAt() {
		return "<"+getFloat("lookatx")+","+getFloat("lookaty")+","+getFloat("lookatz")+">";
	}

	@Nonnull
	public Region getRegion(boolean allowretired) {
		return Region.get(getIntNullable("regionid"),allowretired);
	}

	@Nonnull
	@Override
	public String getNameField() { return "name"; }

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
		return "landmarks";
	}

}
