package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

public class Landmarks extends TableRow {
	public Landmarks(int id) {
		super(id);
	}

	public static Landmarks get(int id) {
		return (Landmarks) factoryPut("Landmarks", id, new Landmarks(id));
	}

	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
	}

	public static void obliterate(Instance instance,String name) {
		GPHUD.getDB().d("delete landmarks from landmarks inner join regions on landmarks.regionid=regions.regionid where regions.instanceid=? and landmarks.name like ?",instance.getId(),name);
	}

	public static Landmarks find(Instance instance,String name) {
		Integer id=GPHUD.getDB().dqi(false,"select landmarks.id from landmarks,regions where landmarks.regionid=regions.regionid and regions.instanceid=? and landmarks.name like ?",instance.getId(),name);
		if (id==null) { return null; }
		return new Landmarks(id);
	}

	public static void create(Region region,String name,float x,float y,float z,float lax,float lay,float laz) {
		obliterate(region.getInstance(),name);
		GPHUD.getDB().d("insert into landmarks(regionid,name,x,y,z,lookatx,lookaty,lookatz) values(?,?,?,?,?,?,?,?)",region.getId(),name,x,y,z,lax,lay,laz);
	}

	public String getHUDRepresentation(boolean allowretired) {
		String tp="";
		tp+=getRegion(allowretired).getGlobalCoordinates();
		tp+="|";
		tp+=getCoordinates();
		tp+="|";
		tp+=getLookAt();
		return tp;
	}

	public String getCoordinates() {
		return "<"+getFloat("x")+","+getFloat("y")+","+getFloat("z")+">";
	}
	public String getLookAt() {
		return "<"+getFloat("lookatx")+","+getFloat("lookaty")+","+getFloat("lookatz")+">";
	}

	public Region getRegion(boolean allowretired) {
		return Region.get(getInt("regionid"),allowretired);
	}

	@Override
	public String getNameField() { return "name"; }

	@Override
	public String getLinkTarget() { return null; }

	@Override
	protected int getNameCacheTime() { return 0; }

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
		return "landmarks";
	}

}
