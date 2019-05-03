package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Experience.QuotaedXP;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;

import java.util.Set;
import java.util.TreeSet;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.*;

/**
 * Contains the data related to an attribute defined for an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Attribute extends TableRow {

	protected Attribute(int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	public static Attribute get(int id) {
		return (Attribute) factoryPut("Attribute", id, new Attribute(id));
	}

	/**
	 * Find an attribute in an instance.
	 *
	 * @param instance Instance to look attribute up in.
	 * @param name     Name of attribute to locate
	 * @return Region object for that region, or null if none is found.
	 */
	public static Attribute find(Instance instance, String name) {
		Integer id = GPHUD.getDB().dqi(false, "select attributeid from attributes where name like ? and instanceid=?", name, instance.getId());
		if (id == null) {
			throw new UserException("Unable to find attribute '" + name + "' in instance '" + instance + "'");
		}
		return get(id);
	}

	/**
	 * Get the attributes for the instance in this state.
	 *
	 * @param st Infers state
	 * @return Set of attribute for this instance
	 */
	public static Set<Attribute> getAttributes(State st) { return getAttributes(st.getInstance()); }

	/**
	 * Get the attributes for the instance.
	 *
	 * @param instance The instance to query
	 * @return Set of attribute for this instance
	 */
	public static Set<Attribute> getAttributes(Instance instance) {
		Set<Attribute> set = new TreeSet<>();
		for (ResultsRow r : GPHUD.getDB().dq("select attributeid from attributes where instanceid=?", instance.getId())) {
			set.add(Attribute.get(r.getInt()));
		}
		return set;
	}

	static void create(Instance instance, String name, Boolean selfmodify, String attributetype, String grouptype, Boolean usesabilitypoints, Boolean required, String defaultvalue) {
		// =)
		if (defaultvalue != null && defaultvalue.equals("")) { defaultvalue = null; }
		GPHUD.getDB().d("insert into attributes(instanceid,name,selfmodify,attributetype,grouptype,usesabilitypoints,required,defaultvalue) values(?,?,?,?,?,?,?,?)",
				instance.getId(), name, selfmodify, attributetype, grouptype, usesabilitypoints, required, defaultvalue);
	}

	/**
	 * Find attribute by name
	 *
	 * @param st   State
	 * @param name Attribute name
	 * @return Attribute
	 */
	public static Attribute resolve(State st, String name) {
		int id = new Attribute(-1).resolveToID(st, name, true);
		if (id == 0) { return null; }
		return get(id);
	}

	@Override
	public String getLinkTarget() { return "attributes"; }

	/**
	 * Gets the instance associated with this attribute.
	 *
	 * @return The Instance object
	 */
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Override
	public String getTableName() {
		return "attributes";
	}

	@Override
	public String getIdField() {
		return "attributeid";
	}

	@Override
	public String getNameField() {
		return "name";
	}

	@Override
	public String getKVTable() { return null; }

	@Override
	public String getKVIdField() { return null; }

	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Attribute / State Instance mismatch"); }
	}

	;

	@Override
	protected int getNameCacheTime() { return 60 * 60; } // 1 hour, attributes can NOT be renamed because they create a KV based on the name :P

	public ATTRIBUTETYPE getType() {
		String type = null;
		try { type = (String) cacheGet("type"); } catch (CacheMiss ex) {
			type = getString("attributetype");
			cachePut("type", type, getNameCacheTime());
		}
		if (type == null) { throw new SystemException("Null type for attribute " + getId() + " " + getNameSafe()); }
		if (type.equalsIgnoreCase("text")) { return TEXT; }
		if (type.equalsIgnoreCase("integer")) { return INTEGER; }
		if (type.equalsIgnoreCase("group")) { return GROUP; }
		if (type.equalsIgnoreCase("pool")) { return POOL; }
		if (type.equalsIgnoreCase("float")) { return FLOAT; }
		if (type.equalsIgnoreCase("color")) { return COLOR; }
		if (type.equalsIgnoreCase("experience")) { return EXPERIENCE; }
		throw new SystemException("Unhandled type " + type + " for attribute " + getId() + " " + getNameSafe());
	}

	public String getSubType() { return getString("grouptype"); }

	public boolean usesAbilityPoints() { return getBool("usesabilitypoints"); }

	public boolean getRequired() { return getBool("required"); }

	/**
	 * Sets the required flag.
	 *
	 * @param required New required flag state.
	 */
	public void setRequired(Boolean required) {
		set("required", required);
	}

	public String getDefaultValue() { return getString("defaultvalue"); }

	/**
	 * Set the default value for this attribute.
	 *
	 * @param defaultvalue New default value
	 */
	public void setDefaultValue(String defaultvalue) {
		set("defaultvalue", defaultvalue);
	}

	/**
	 * Get the self modify flag.
	 *
	 * @return boolean true if can self modify
	 */
	public boolean getSelfModify() { return getBool("selfmodify"); }

	/**
	 * Set the self modify flag.
	 *
	 * @param selfmodify Character can self modify the attribute
	 */
	public void setSelfModify(Boolean selfmodify) {
		set("selfmodify", selfmodify);
	}

	public String getCharacterValue(State st) {
		if (isKV()) { return st.getKV("Characters." + getName()).value(); }
		if (getType() == GROUP) {
			CharacterGroup cg = st.getCharacter().getGroup(getSubType());
			if (cg == null) { return null; }
			return cg.getName();
		}
		if (getType() == POOL || getType() == EXPERIENCE) {
			if (this instanceof QuotaedXP) {
				QuotaedXP xp = (QuotaedXP) this;
				return st.getCharacter().sumPool(xp.getPool(st)) + "";
			} else { return "POOL"; }
		}
		throw new SystemException("Unhandled non KV type " + getType());
	}

	public String getCharacterValueDescription(State st) {
		if (isKV()) { return st.getKV("Characters." + getName()).path(); }
		if (getType() == POOL || getType() == EXPERIENCE) {
			if (this instanceof QuotaedXP) {
				QuotaedXP xp = (QuotaedXP) this;
				return ("<i>(In last " + xp.periodRoughly(st) + " : " + xp.periodAwarded(st) + ")</i>") +
						(", <i>Next available:" + xp.nextFree(st) + "</i>");
			} else { return "POOL"; }
		}
		if (getType() == GROUP) { return ""; }
		throw new SystemException("Unhandled type " + getType());
	}

	/**
	 * Wether this attribute is represented as a KV.
	 * Group memberships (faction, race) are NOT a KV.
	 *
	 * @return True if this attribute generates a KV.
	 */
	public boolean isKV() {
		ATTRIBUTETYPE def = getType();
		if (def == INTEGER || def == FLOAT || def == TEXT || def == COLOR) { return true; }
		if (def == POOL || def == GROUP || def == EXPERIENCE) { return false; }
		throw new SystemException("Unknown attribute type " + def + " in attribute " + this);
	}

	/**
	 * Gets the KV type that models this attribute.
	 *
	 * @return the KVTYPE
	 * @throws SystemException If the attribute is not of a KV represented attribute.
	 */
	public KV.KVTYPE getKVType() throws SystemException {
		ATTRIBUTETYPE def = getType();
		if (def == INTEGER) { return KV.KVTYPE.INTEGER; }
		if (def == FLOAT) { return KV.KVTYPE.FLOAT; }
		if (def == TEXT) { return KV.KVTYPE.TEXT; }
		if (def == COLOR) { return KV.KVTYPE.COLOR; }
		throw new SystemException("Non KV attribute type " + def + " in attribute " + this);
	}

	/**
	 * Gets the default KV value for this attribute.
	 *
	 * @return The default value
	 * @throws SystemException if this attribute type is not KV backed
	 */
	public String getKVDefaultValue() throws SystemException {
		ATTRIBUTETYPE def = getType();
		if (def == INTEGER) { return "0"; }
		if (def == FLOAT) { return "0"; }
		if (def == TEXT) { return ""; }
		if (def == COLOR) { return "<1,1,1>"; }
		throw new SystemException("Unhandled KV attribute type " + def + " in attribute " + this);
	}

	/**
	 * Gets the KV hierarchy type for this attribute.
	 *
	 * @return the appropriate KVHIERARCHY
	 * @throws SystemException If this attribute is not backed by a KV type.
	 */
	public KV.KVHIERARCHY getKVHierarchy() throws SystemException {
		ATTRIBUTETYPE def = getType();
		if (def == INTEGER) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def == FLOAT) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def == TEXT) { return KV.KVHIERARCHY.DELEGATING; }
		if (def == COLOR) { return KV.KVHIERARCHY.DELEGATING; }
		throw new SystemException("Unhandled attribute type " + def + " in attribute " + this);
	}

	/**
	 * Set the uses abilitypoints flag.
	 *
	 * @param usesabilitypoints Flags new value
	 */
	public void setUsesAbilityPoints(Boolean usesabilitypoints) {
		set("usesabilitypoints", usesabilitypoints);
	}

	/**
	 * Deletes this attribute, and its data.
	 */
	public void delete() {
		// delete data
		getInstance().wipeKV("Characters." + getName());
		d("delete from attributes where attributeid=?", getId());
	}

	public boolean readOnly() {
		return false;
	}

	public static enum ATTRIBUTETYPE {TEXT, FLOAT, INTEGER, GROUP, POOL, COLOR, EXPERIENCE}


}
