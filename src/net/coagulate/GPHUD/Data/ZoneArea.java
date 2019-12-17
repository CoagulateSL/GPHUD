package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A 'zone' - an area of a region.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ZoneArea extends TableRow {

	protected ZoneArea(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A zone representation
	 */
	@Nonnull
	public static ZoneArea get(final int id) {
		return (ZoneArea) factoryPut("ZoneArea", id, new ZoneArea(id));
	}

	/**
	 * Convert a string vector into an int array.
	 *
	 * @param s String vector, x,y,z format with optional angle brackets (SL format)
	 * @return 3 part int array [x,y,z]
	 */
	@Nonnull
	public static int[] parseVector(String s) {
		s = s.replaceAll("<", "");
		s = s.replaceAll(">", "");
		s = s.replaceAll("\\(", "");
		s = s.replaceAll("\\)", "");
		final String[] parts = s.split(",");
		if (parts.length != 3) { throw new UserInputValidationParseException("Could not decompose co-ordinates properly"); }
		final int[] pos = new int[3];
		try { pos[0] = (int) Float.parseFloat(parts[0]); } catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Error processing X number " + parts[0] + " - " + e.getMessage());
		}
		try { pos[1] = (int) Float.parseFloat(parts[1]); } catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Error processing Y number " + parts[1] + " - " + e.getMessage());
		}
		try { pos[2] = (int) Float.parseFloat(parts[2]); } catch (@Nonnull final NumberFormatException e) {
			throw new UserInputValidationParseException("Error processing Z number " + parts[2] + " - " + e.getMessage());
		}
		return pos;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "zoneareas";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "zoneareaid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		final String[] vectors = getVectors();
		if (vectors==null) { return "NoPosition"; }
		return vectors[0] + " - " + vectors[1];
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/configuration/zone/" + getId();
	}

	/**
	 * Set the position for this zone area.
	 *
	 * @param loc1 Corner 1 as vector string
	 * @param loc2 Corner 2 as vector string
	 */
	public void setPos(final String loc1, final String loc2) {
		final int[] one = parseVector(loc1);
		final int[] two = parseVector(loc2);
		d("update zoneareas set x1=?,y1=?,z1=?,x2=?,y2=?,z2=? where zoneareaid=?", one[0], one[1], one[2], two[0], two[1], two[2], getId());
	}

	/**
	 * Return the two corners.
	 * The corners are sorted so v1.x is less than or equal to v2.x, and similarly for y and z, simplifying the code logic at the HUD end.
	 *
	 * @return Array pair of vectors as strings representing the corners of the bounding box.
	 */
	@Nullable
	public String[] getVectors() {
		final ResultsRow r = dqone( "select * from zoneareas where zoneareaid=?", getId());
		final Integer x1 = r.getIntNullable("x1");
		final Integer y1 = r.getIntNullable("y1");
		final Integer z1 = r.getIntNullable("z1");
		final Integer x2 = r.getIntNullable("x2");
		final Integer y2 = r.getIntNullable("y2");
		final Integer z2 = r.getIntNullable("z2");
		if (x1 == null || x2 == null ||
				y1 == null || y2 == null ||
				z1 == null || z2 == null) { return null; }
		String vec1 = "<";
		String vec2 = "<";
		if (x1 <= x2) {
			vec1 += x1;
			vec2 += x2;
		} else {
			vec1 += x2;
			vec2 += x1;
		}
		vec1 += ",";
		vec2 += ",";
		if (y1 <= y2) {
			vec1 += y1;
			vec2 += y2;
		} else {
			vec1 += y2;
			vec2 += y1;
		}
		vec1 += ",";
		vec2 += ",";
		if (z1 <= z2) {
			vec1 += z1;
			vec2 += z2;
		} else {
			vec1 += z2;
			vec2 += z1;
		}
		vec1 += ">";
		vec2 += ">";
		final String[] vectors = new String[2];
		vectors[0] = vec1;
		vectors[1] = vec2;
		return vectors;
	}

	/**
	 * Get the region associated with this zone area.
	 *
	 * @return Region
	 */
	@Nonnull
	public Region getRegion(final boolean allowretired) {
		final Integer id = getIntNullable("regionid");
		if (id == null) { throw new SystemConsistencyException("Zone Area " + getId() + " has no associated region?"); }
		return Region.get(id,allowretired);
	}

	/**
	 * Get the zone object that owns this area.
	 *
	 * @return Zone
	 */
	@Nonnull
	public Zone getZone() {
		return Zone.get(dqinn( "select zoneid from zoneareas where zoneareaid=?", getId()));
	}

	/**
	 * Delete this zone area.
	 */
	public void delete() {
		d("delete from zoneareas where zoneareaid=?", getId());
	}

	@Nullable
	public String getKVTable() { return null; }

	@Nullable
	public String getKVIdField() { return null; }

	public void flushKVCache(final State st) {}

	@Override
	public void validate(@Nonnull final State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getZone().getInstance()) {
			throw new SystemConsistencyException("ZoneArea / State Instance mismatch");
		}
	}

	@Nonnull
	@Override
	public String getName() {
		final String[] vectors = getVectors();
		if (vectors==null) { return getRegion(true).getName()+"@NoPosition"; }
		return getRegion(true).getName() + "@" + vectors[0] + "-" + vectors[1];
	}

	@Override
	protected int getNameCacheTime() { return 0; }


}

