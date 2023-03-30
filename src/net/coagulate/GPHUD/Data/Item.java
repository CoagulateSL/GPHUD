package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Interfaces.Outputs.HeaderRow;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

public class Item extends TableRow {
	
	@Nonnull
	public static Set<Item> getAll(@Nonnull final State state) {
		return getAll(state.getInstance());
	}
	
	@Nonnull
	public static Set<Item> getAll(@Nonnull final Instance instance) {
		final Set<Item> items=new TreeSet<>();
		for (final ResultsRow row: db().dq("select id from items where instanceid=?",instance.getId())) {
			items.add(Item.get(row.getInt("id")));
		}
		return items;
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An Instance representation
	 */
	@Nonnull
	public static Item get(@Nonnull final Integer id) {
		return (Item)factoryPut("Item",id,Item::new);
	}
	
	protected Item(final int id) {
		super(id);
	}
	
	@Nonnull
	public static Set<String> getNames(@Nonnull final State state) {
		return getNames(state.getInstance());
	}
	
	@Nonnull
	public static Set<String> getNames(@Nonnull final Instance instance) {
		final Set<String> items=new TreeSet<>();
		for (final ResultsRow row: db().dq("select name from items where instanceid=?",instance.getId())) {
			items.add(row.getString("name"));
		}
		return items;
	}
	
	public static Table getSummaryPage(final Instance instance) {
		final Table table=new Table();
		table.add(new HeaderRow().add("Name").add("Weight").add("Tradable").add("Destroyable").add("Description"));
		for (final ResultsRow row: db().dq("select * from items where instanceid=? order by name",instance.getId())) {
			table.openRow();
			table.add(new Link(row.getString("name"),"/GPHUD/configuration/items/"+row.getInt("id")))
			     .add(row.getInt("weight"))
			     .add(row.getBool("tradable")?"Yes":"")
			     .add(row.getBool("destroyable")?"Yes":"")
			     .add(row.getString("description"));
		}
		return table;
	}
	
	public static Item findOrCreate(@Nonnull final State state,@Nonnull final String name) {
		Item item=findNullable(state.getInstance(),name);
		if (item!=null) {
			return item;
		}
		Audit.audit(true,state,Audit.OPERATOR.AVATAR,null,null,"Add","Item",null,name,"Created new item");
		create(state.getInstance(),name);
		state.getInstance().itemNameResolveCache.purge(name);
		item=findNullable(state.getInstance(),name);
		if (item!=null) {
			return item;
		}
		throw new SystemImplementationException(
				"Tried to create item '"+name+"' in instance "+state.getInstance()+" and found nothing afterwards");
	}
	
	public static Item findNullable(@Nonnull final Instance instance,@Nonnull final String name) {
		return instance.itemNameResolveCache.get(name,()->{
			Integer id=null;
			try {
				id=db().dqi("select id from items where name like ? and instanceid=?",name,instance.getId());
			} catch (final NoDataException ignored) {
			}
			if (id==null) {
				return null;
			}
			return Item.get(id);
		});
	}
	
	private static void create(@Nonnull final Instance instance,@Nonnull final String name) {
		db().d("insert into items(instanceid,name) values(?,?)",instance.getId(),name);
	}
	
	@Nonnull
	public static Item find(final State state,final String itemName) {
		final Item item=findNullable(state.getInstance(),itemName);
		if (item==null) {
			throw new UserInputLookupFailureException("Item '"+itemName+"' does not exist",true);
		}
		return item;
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
		return "name";
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
		return Instance.get(getInt("instanceid"));
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "items";
	}
	
	public String description() {
		return getString("description");
	}
	
	public void description(final String newDescription) {
		set("description",newDescription);
	}
	
	public int weight() {
		return getInt("weight");
	}
	
	public void weight(final int newWeight) {
		set("weight",newWeight);
	}
	
	
	public boolean destroyable() {
		return getBool("destroyable");
	}
	
	public boolean tradable() {
		return getBool("tradable");
	}
	
	public void destroyable(final boolean destroyable) {
		set("destroyable",destroyable);
	}
	
	public void tradable(final boolean tradable) {
		set("tradable",tradable);
	}
	
	public void delete() {
		Inventory.deleteAll(getInstance(),getName());
		final String name=getName();
		final Instance instance=getInstance();
		d("delete from items where id=?",getId());
		instance.itemNameResolveCache.purge(name);
	}
}
