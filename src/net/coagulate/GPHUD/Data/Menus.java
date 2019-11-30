package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Menus entry - a name, description and complex JSONobject that wraps the button name, and commands.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Menus extends TableRow {

	protected Menus(int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An Avatar representation
	 */
	@Nonnull
	public static Menus get(int id) {
		return (Menus) factoryPut("Menus", id, new Menus(id));
	}

	/**
	 * Get a list of menus and their ID for an instance.
	 *
	 * @param st Infers instance
	 * @return Map of String menu name to Integer menu ID for this instance.
	 */
	@Nonnull
	public static Map<String, Integer> getMenusMap(@Nonnull State st) {
		Map<String, Integer> aliases = new TreeMap<>();
		for (ResultsRow r : GPHUD.getDB().dq("select name,menuid from menus where instanceid=?", st.getInstance().getId())) {
			aliases.put(r.getString("name"), r.getInt("menuid"));
		}
		return aliases;
	}

	/**
	 * Load instance menu by name
	 *
	 * @param st   State (infers instance)
	 * @param name Name of the menu to load
	 * @return Menus object
	 */
	@Nullable
	public static Menus getMenu(@Nonnull State st, String name) {
		try {
			Integer id = GPHUD.getDB().dqi("select menuid from menus where instanceid=? and name like ?", st.getInstance().getId(), name);
			return get(id);
		} catch (NoDataException e) { return null; }
	}

	/**
	 * Create a new menu,by name and description, with the given json data blob
	 *
	 * @param st          State (infers instance)
	 * @param name        Name of the new menu
	 * @param description Description of the new menu
	 * @param template    JSONObject template for the new menu (belongs to Menus module)
	 * @return the new Menus object
	 * @throws UserException If the name is invalid or duplicated.
	 */
	@Nullable
	public static Menus create(@Nonnull State st, @Nonnull String name, String description, @Nonnull JSONObject template) throws UserException {
		if (getMenu(st, name) != null) { throw new UserException("Menu " + name + " already exists"); }
		if (name.matches(".*[^A-Za-z0-9-=_,].*")) {
			throw new UserException("Menu name must not contain spaces, and mostly only allow A-Z a-z 0-9 - + _ ,");
		}
		GPHUD.getDB().d("insert into menus(instanceid,name,description,json) values(?,?,?,?)", st.getInstance().getId(), name, description, template.toString());
		Menus newalias = getMenu(st, name);
		if (newalias == null) {
			throw new SystemException("Failed to create alias " + name + " in instance id " + st.getInstance().getId() + ", created but not found?");
		}
		return newalias;
	}

	/**
	 * Load all the menus for an instance
	 *
	 * @param st State, infers instance
	 * @return Map of Name to JSONPayloads for all menus in this instance.
	 */
	@Nonnull
	public static Map<String, JSONObject> getTemplates(@Nonnull State st) {
		Map<String, JSONObject> aliases = new TreeMap<>();
		for (ResultsRow r : GPHUD.getDB().dq("select name,description,json from menus where instanceid=?", st.getInstance().getId())) {
			aliases.put(r.getString("name"), new JSONObject(r.getString("json")));
		}
		return aliases;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "menus";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "menuid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/configuration/menus/" + getId();
	}

	/**
	 * Load the JSON payload for this menu.
	 *
	 * @return The JSON payload
	 */
	@Nonnull
	public JSONObject getJSON() throws SystemException {
		String json = dqs( "select json from menus where menuid=?", getId());
		if (json == null) { throw new SystemException("No (null) template for menu id " + getId()); }
		return new JSONObject(json);
	}

	/**
	 * Set the JSON payload.
	 *
	 * @param template JSON payload
	 */
	public void setJSON(@Nonnull JSONObject template) {
		d("update menus set json=? where menuid=?", template.toString(), getId());
	}

	/**
	 * Obtain the instanceID this menu belongs to.
	 *
	 * @return Instance for this menu
	 */
	@Nonnull
	public Instance getInstance() {
		Integer id = dqi("select instanceid from menus where menuid=?", getId());
		return Instance.get(id);
	}

	@Nullable
	public String getKVTable() { return null; }

	@Nullable
	public String getKVIdField() { return null; }

	public void flushKVCache(State st) {}

	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Menus / State Instance mismatch"); }
	}

	protected int getNameCacheTime() { return 60 * 60; } // this name doesn't change, cache 1 hour
}
