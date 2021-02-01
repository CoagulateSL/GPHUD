package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class Landmark extends TableRow {
	public Landmark(final int id) {
		super(id);
	}

	// ---------- STATICS ----------
	@Nonnull
	public static Landmark get(final int id) {
		return (Landmark) factoryPut("Landmarks",id,Landmark::new);
	}

	/**
	 * Get a set of all landmarks for an instance
	 *
	 * @param instance Instance to get landmarks for
	 *
	 * @return Set of Landmarks
	 */
	@Nonnull
	public static Set<Landmark> getAll(@Nonnull final Instance instance) {
		final Set<Landmark> results=new HashSet<>();
		for (final ResultsRow row: db().dq("select landmarks.id as id from landmarks,regions where landmarks.regionid=regions.regionid and regions.instanceid=?",
		                                   instance.getId()
		                                  )) {
			results.add(get(row.getInt("id")));
		}
		return results;
	}

	/**
	 * Get a set of all landmarks for an instance
	 *
	 * @param st State Instance to get landmarks for
	 *
	 * @return Set of Landmarks
	 */
	@Nonnull
	public static Set<Landmark> getAll(@Nonnull final State st) {
		return getAll(st.getInstance());
	}

	/**
	 * Delete a landmark by name
	 *
	 * @param instance Instance to delete landmark from
	 * @param name     Name of landmark to delete
	 */
	public static void obliterate(@Nonnull final Instance instance,
	                              @Nonnull final String name) {
		db().d("delete landmarks from landmarks inner join regions on landmarks.regionid=regions.regionid where regions.instanceid=? and landmarks.name like ?",
		       instance.getId(),
		       name
		      );
	}

	/**
	 * Find a landmark by name
	 *
	 * @param instance Instance
	 * @param name     Landmark name
	 *
	 * @return Landmark object, or null
	 */
	@Nullable
	public static Landmark find(@Nonnull final Instance instance,
	                            @Nonnull final String name) {
		try {
			final int id=db().dqiNotNull("select landmarks.id from landmarks,regions where landmarks.regionid=regions.regionid and regions.instanceid=? and landmarks.name like"+" ?",
			                        instance.getId(),
			                        name
			                       );
			return new Landmark(id);
		}
		catch (@Nonnull final NoDataException e) {return null;}
	}

	/**
	 * Find a landmark by name
	 *
	 * @param st   State Instance
	 * @param name Landmark name
	 *
	 * @return Landmark object, or null
	 */
	@Nullable
	public static Landmark find(@Nonnull final State st,
	                            @Nonnull final String name) {
		return find(st.getInstance(),name);
	}

	/**
	 * Create a landmark
	 *
	 * @param region Region for the landmark
	 * @param name   Name of the landmark
	 * @param x      Sim Local X
	 * @param y      Sim Local Y
	 * @param z      Sim Local Z
	 * @param lax    Look At X
	 * @param lay    Look At Y
	 * @param laz    Look At Z
	 */
	public static void create(@Nonnull final Region region,
	                          @Nonnull final String name,
	                          final float x,
	                          final float y,
	                          final float z,
	                          final float lax,
	                          final float lay,
	                          final float laz) {
		obliterate(region.getInstance(),name);
		db().d("insert into landmarks(regionid,name,x,y,z,lookatx,lookaty,lookatz) values(?,?,?,?,?,?,?,?)",region.getId(),name,x,y,z,lax,lay,laz);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getIdColumn() { return "id"; }

	@Override
	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
	}

	@Nonnull
	@Override
	public String getNameField() { return "name"; }

	@Nullable
	@Override
	public String getLinkTarget() { return null; }

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

	/**
	 * Returns this landmark in a delimited format the HUD understands
	 *
	 * @param allowretired Allow access to a retired region
	 *
	 * @return A pipe delimited string of global co-ords, region co-ords and look at data.
	 */
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

	/**
	 * Returns the co-ordinates in LSL Vector format (as a string).
	 *
	 * @return The x,y,z vector formatted string
	 */
	@Nonnull
	public String getCoordinates() {
		return "<"+getFloatNullable("x")+","+getFloatNullable("y")+","+getFloatNullable("z")+">";
	}

	/**
	 * Returns the look at co-ordinates in LSL Vector format (as a string).
	 *
	 * @return The look at x,y,z vector formatted string
	 */
	@Nonnull
	public String getLookAt() {
		return "<"+getFloatNullable("lookatx")+","+getFloatNullable("lookaty")+","+getFloatNullable("lookatz")+">";
	}

	/**
	 * Returns the region attached to this landmark
	 *
	 * @param allowretired Allow retired regions
	 *
	 * @return The region object
	 */
	@Nonnull
	public Region getRegion(final boolean allowretired) {
		return Region.get(getInt("regionid"),allowretired);
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "landmarks";
	}

}
