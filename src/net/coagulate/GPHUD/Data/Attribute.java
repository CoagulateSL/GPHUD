package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Experience.QuotaedXP;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.State;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.*;

/**
 * Contains the data related to an attribute defined for an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Attribute extends TableRow {

	protected Attribute(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	@Nonnull
	public static Attribute get(final int id) {
		return (Attribute) factoryPut("Attribute", id, new Attribute(id));
	}

	/**
	 * Find an attribute in an instance.
	 *
	 * @param instance Instance to look attribute up in.
	 * @param name     Name of attribute to locate
	 * @return Region object for that region, or null if none is found.
	 */
	@Nonnull
	public static Attribute find(@Nonnull final Instance instance, final String name) {
		try {
			final int id = GPHUD.getDB().dqinn("select attributeid from attributes where name like ? and instanceid=?", name, instance.getId());
			return get(id);
		} catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find attribute '" + name + "' in instance '" + instance + "'",e);
		}
	}

	/** Find an attribute that is a group by 'type'.
	 *
	 */
	@Nonnull
	public static Attribute findGroup(final @NotNull Instance instance, final String grouptype) {
		try {
			final int id = GPHUD.getDB().dqinn("select attributeid from attributes where instanceid=? and attributetype='GROUP' and grouptype=?", instance.getId(), grouptype);
			return get(id);
		} catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find an attribute representing a group of type " + grouptype, e);
		}
	}

	/**
	 * Get the attributes for the instance in this state.
	 *
	 * @param st Infers state
	 * @return Set of attribute for this instance
	 */
	@Nonnull
	public static Set<Attribute> getAttributes(@Nonnull final State st) { return getAttributes(st.getInstance()); }

	/**
	 * Get the attributes for the instance.
	 *
	 * @param instance The instance to query
	 * @return Set of attribute for this instance
	 */
	@Nonnull
	public static Set<Attribute> getAttributes(@Nonnull final Instance instance) {
		final Set<Attribute> set = new TreeSet<>();
		for (final ResultsRow r : GPHUD.getDB().dq("select attributeid from attributes where instanceid=?", instance.getId())) {
			set.add(Attribute.get(r.getInt()));
		}
		return set;
	}

	static void create(@Nonnull final Instance instance, final String name, final Boolean selfmodify, final String attributetype, final String grouptype, final Boolean usesabilitypoints, final Boolean required, String defaultvalue) {
		// =)
		if ("".equals(defaultvalue)) { defaultvalue = null; }
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
	@Nullable
	public static Attribute resolve(@Nonnull final State st, final String name) {
		final int id = new Attribute(-1).resolveToID(st, name, true);
		if (id == 0) { return null; }
		return get(id);
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "attributes"; }

	/**
	 * Gets the instance associated with this attribute.
	 *
	 * @return The Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "attributes";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "attributeid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nullable
	@Override
	public String getKVTable() { return null; }

	@Nullable
	@Override
	public String getKVIdField() { return null; }

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemConsistencyException("Attribute / State Instance mismatch"); }
	}

	@Override
	protected int getNameCacheTime() { return 60 * 60; } // 1 hour, attributes can NOT be renamed because they create a KV based on the name :P

	@Nonnull
	public ATTRIBUTETYPE getType() {
		String type;
		try { type = (String) cacheGet("type"); } catch (@Nonnull final CacheMiss ex) {
			type = getStringNullable("attributetype");
			cachePut("type", type, getNameCacheTime());
		}
		if (type == null) { throw new SystemBadValueException("Null type for attribute " + getId() + " " + getNameSafe()); }
		if ("text".equalsIgnoreCase(type)) { return TEXT; }
		if ("integer".equalsIgnoreCase(type)) { return INTEGER; }
		if ("group".equalsIgnoreCase(type)) { return GROUP; }
		if ("pool".equalsIgnoreCase(type)) { return POOL; }
		if ("float".equalsIgnoreCase(type)) { return FLOAT; }
		if ("color".equalsIgnoreCase(type)) { return COLOR; }
		if ("experience".equalsIgnoreCase(type)) { return EXPERIENCE; }
		throw new SystemImplementationException("Unhandled type " + type + " for attribute " + getId() + " " + getNameSafe());
	}

	@Nullable
	public String getSubType() { return getStringNullable("grouptype"); }

	public boolean usesAbilityPoints() { return getBool("usesabilitypoints"); }

	public boolean getRequired() { return getBool("required"); }

	/**
	 * Sets the required flag.
	 *
	 * @param required New required flag state.
	 */
	public void setRequired(final Boolean required) {
		set("required", required);
	}

	@Nullable
	public String getDefaultValue() { return getStringNullable("defaultvalue"); }

	/**
	 * Set the default value for this attribute.
	 *
	 * @param defaultvalue New default value
	 */
	public void setDefaultValue(final String defaultvalue) {
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
	public void setSelfModify(final Boolean selfmodify) {
		set("selfmodify", selfmodify);
	}

	@Nullable
	public String getCharacterValue(@Nonnull final State st) {
		if (isKV()) { return st.getKV("Characters." + getName()).value(); }
		if (getType() == GROUP) {
			final CharacterGroup cg = st.getCharacter().getGroup(getSubType());
			if (cg == null) { return null; }
			return cg.getName();
		}
		if (getType() == POOL || getType() == EXPERIENCE) {
			if (this instanceof QuotaedXP) {
				final QuotaedXP xp = (QuotaedXP) this;
				return st.getCharacter().sumPool(xp.getPool(st)) + "";
			} else { return "POOL"; }
		}
		throw new SystemImplementationException("Unhandled non KV type " + getType());
	}

	@Nonnull
	public String getCharacterValueDescription(@Nonnull final State st) {
		if (isKV()) { return st.getKV("Characters." + getName()).path(); }
		if (getType() == POOL || getType() == EXPERIENCE) {
			if (this instanceof QuotaedXP) {
				final QuotaedXP xp = (QuotaedXP) this;
				return ("<i>(In last " + xp.periodRoughly(st) + " : " + xp.periodAwarded(st) + ")</i>") +
						(", <i>Next available:" + xp.nextFree(st) + "</i>");
			} else { return "POOL"; }
		}
		if (getType() == GROUP) { return ""; }
		throw new SystemImplementationException("Unhandled type " + getType());
	}

	/**
	 * Wether this attribute is represented as a KV.
	 * Group memberships (faction, race) are NOT a KV.
	 *
	 * @return True if this attribute generates a KV.
	 */
	public boolean isKV() {
		final ATTRIBUTETYPE def = getType();
		if (def == INTEGER || def == FLOAT || def == TEXT || def == COLOR) { return true; }
		if (def == POOL || def == GROUP || def == EXPERIENCE) { return false; }
		throw new SystemImplementationException("Unknown attribute type " + def + " in attribute " + this);
	}

	/**
	 * Gets the KV type that models this attribute.
	 *
	 * @return the KVTYPE
	 * @throws SystemException If the attribute is not of a KV represented attribute.
	 */
	@Nonnull
	public KV.KVTYPE getKVType() {
		final ATTRIBUTETYPE def = getType();
		if (def == INTEGER) { return KV.KVTYPE.INTEGER; }
		if (def == FLOAT) { return KV.KVTYPE.FLOAT; }
		if (def == TEXT) { return KV.KVTYPE.TEXT; }
		if (def == COLOR) { return KV.KVTYPE.COLOR; }
		throw new SystemImplementationException("Non KV attribute type " + def + " in attribute " + this);
	}

	/**
	 * Gets the default KV value for this attribute.
	 *
	 * @return The default value
	 * @throws SystemException if this attribute type is not KV backed
	 */
	@Nonnull
	public String getKVDefaultValue() {
		final ATTRIBUTETYPE def = getType();
		if (def == INTEGER) { return "0"; }
		if (def == FLOAT) { return "0"; }
		if (def == TEXT) { return ""; }
		if (def == COLOR) { return "<1,1,1>"; }
		throw new SystemImplementationException("Unhandled KV attribute type " + def + " in attribute " + this);
	}

	/**
	 * Gets the KV hierarchy type for this attribute.
	 *
	 * @return the appropriate KVHIERARCHY
	 * @throws SystemException If this attribute is not backed by a KV type.
	 */
	@Nonnull
	public KV.KVHIERARCHY getKVHierarchy() {
		final ATTRIBUTETYPE def = getType();
		if (def == INTEGER) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def == FLOAT) { return KV.KVHIERARCHY.CUMULATIVE; }
		if (def == TEXT) { return KV.KVHIERARCHY.DELEGATING; }
		if (def == COLOR) { return KV.KVHIERARCHY.DELEGATING; }
		throw new SystemImplementationException("Unhandled attribute type " + def + " in attribute " + this);
	}

	/**
	 * Set the uses abilitypoints flag.
	 *
	 * @param usesabilitypoints Flags new value
	 */
	public void setUsesAbilityPoints(final Boolean usesabilitypoints) {
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

	public enum ATTRIBUTETYPE {TEXT, FLOAT, INTEGER, GROUP, POOL, COLOR, EXPERIENCE}


}
