package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

public class ItemVerb extends TableRow {
	@Nullable
	public static ItemVerb findNullable(@Nonnull final Item item,@Nonnull final String verb) {
		for (final ResultsRow row: db().dq("select * from itemverbs where itemid=? and verb=?",item.getId(),verb)) {
			return get(row.getInt("id"));
		}
		return null;
	}
	
	@Nonnull
	public static ItemVerb find(@Nonnull final Item item,@Nonnull final String verb) {
		final ItemVerb ret=findNullable(item,verb);
		if (ret==null) {
			throw new UserInputLookupFailureException("Item "+item.getName()+" has no action "+verb);
		}
		return ret;
	}
	
	@Nonnull
	public static ItemVerb create(@Nonnull final Item item,@Nonnull final String verb) {
		final ItemVerb ret=findNullable(item,verb);
		if (ret!=null) {
			throw new UserInputDuplicateValueException("Item "+item.getName()+" already has an action named "+verb);
		}
		db().d("insert into itemverbs(itemid,verb) values(?,?)",item.getId(),verb);
		return find(item,verb);
	}
	
	@Nonnull
	public static Set<ItemVerb> findAll(@Nonnull final Item item) {
		final Set<ItemVerb> set=new TreeSet<>();
		for (final ResultsRow row: db().dq("select id from itemverbs where itemid=?",item.getId())) {
			set.add(get(row.getInt("id")));
		}
		return set;
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An Instance representation
	 */
	@Nonnull
	public static ItemVerb get(@Nonnull final Integer id) {
		return (ItemVerb)factoryPut("ItemVerb",id,ItemVerb::new);
	}
	
	protected ItemVerb(final int id) {
		super(id);
	}
	
	@Nonnull
	@Override
	public String getIdColumn() {
		return "id";
	}
	
	@Override
	public void validate(@Nonnull final State st) {
		if (st.getInstance()!=getInstance()) {
			throw new SystemImplementationException("State Instance/Item Instance mismatch");
		}
	}
	
	@Nullable
	@Override
	public String getNameField() {
		return "verb";
	}
	
	@Nullable
	@Override
	public String getLinkTarget() {
		return null;
	}
	
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
	
	public Instance getInstance() {
		return getItem().getInstance();
	}
	
	public Item getItem() {
		return Item.get(getInt("itemid"));
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "itemverbs";
	}
	
	public String description() {
		return getString("description");
	}
	
	public void description(final String newDescription) {
		set("description",newDescription);
	}
	
	public String verb() {
		return getString("verb");
	}
	
	public void verb(final String newDescription) {
		set("verb",newDescription);
	}
	
	public JSONObject payload() {
		return new JSONObject(getString("payload"));
	}
	
	public void payload(final JSONObject newDescription) {
		set("payload",newDescription.toString());
	}
	
	public void delete() {
		d("delete from itemverbs where id=?",getId());
	}
}
