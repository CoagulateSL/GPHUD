package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class Landmarks extends TableRow {
	public Landmarks(final int id) {
		super(id);
	}

	@Nonnull
	public static Landmarks get(final int id) {
		return (Landmarks) factoryPut("Landmarks",id,new Landmarks(id));
	}

	@Nonnull
	public static Set<Landmarks> getAll(@Nonnull final Instance instance) {
		final Set<Landmarks> results=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB()
		                                .dq("select landmarks.id as id from landmarks,regions where landmarks.regionid=regions.regionid and regions.instanceid=?",
		                                    instance.getId()
		                                   )) {
			results.add(get(row.getInt("id")));
		}
		return results;
	}

	public static void obliterate(@Nonnull final Instance instance,
	                              final String name) {
		GPHUD.getDB()
		     .d("delete landmarks from landmarks inner join regions on landmarks.regionid=regions.regionid where regions.instanceid=? and landmarks.name like ?",
		        instance.getId(),
		        name
		       );
	}

	@Nullable
	public static Landmarks find(@Nonnull final Instance instance,
	                             final String name) {
		try {
			final int id=GPHUD.getDB()
			                  .dqinn("select landmarks.id from landmarks,regions where landmarks.regionid=regions.regionid and regions.instanceid=? and landmarks.name like"+
					                         " ?",
			                         instance.getId(),
			                         name
			                        );
			return new Landmarks(id);
		}
		catch (@Nonnull final NoDataException e) {return null;}
	}

	public static void create(@Nonnull final Region region,
	                          final String name,
	                          final float x,
	                          final float y,
	                          final float z,
	                          final float lax,
	                          final float lay,
	                          final float laz) {
		obliterate(region.getInstance(),name);
		GPHUD.getDB().d("insert into landmarks(regionid,name,x,y,z,lookatx,lookaty,lookatz) values(?,?,?,?,?,?,?,?)",region.getId(),name,x,y,z,lax,lay,laz);
	}

	@Nonnull
	@Override
	public String getIdColumn() { return "id"; }

	@Override
	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
	}

	@Nonnull
	public String getHUDRepresentation(final boolean allowretired) {
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
		return "<"+getFloatNullable("x")+","+getFloatNullable("y")+","+getFloatNullable("z")+">";
	}

	@Nonnull
	public String getLookAt() {
		return "<"+getFloatNullable("lookatx")+","+getFloatNullable("lookaty")+","+getFloatNullable("lookatz")+">";
	}

	@Nonnull
	public Region getRegion(final boolean allowretired) {
		return Region.get(getInt("regionid"),allowretired);
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
