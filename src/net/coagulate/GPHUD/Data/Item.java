package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

public class Item extends TableRow {

    @Nonnull public static Set<Item> getAll(@Nonnull final State state) { return getAll(state.getInstance()); }
    @Nonnull public static Set<Item> getAll(@Nonnull final Instance instance) {
        Set<Item> items=new TreeSet<>();
        for (ResultsRow row:db().dq("select id from items where instanceid=?",instance.getId())) {
            items.add(Item.get(row.getInt("id")));
        }
        return items;
    }
    @Nonnull public static Set<String> getNames(@Nonnull final State state) { return getNames(state.getInstance()); }
    @Nonnull public static Set<String> getNames(@Nonnull final Instance instance) {
        Set<String> items=new TreeSet<>();
        for (ResultsRow row:db().dq("select name from items where instanceid=?",instance.getId())) {
            items.add(row.getString("name"));
        }
        return items;
    }

    public static Item findNullable(@Nonnull final Instance instance, @Nonnull final String name) {
        Integer id= db().dqi("select id from items where name like ? and instanceid=?",name,instance.getId());
        if (id==null) { return null; }
        return Item.get(id);
    }

    @Nonnull
    @Override
    public String getIdColumn() {
        return "id";
    }

    @Override
    public void validate(@Nonnull State st) {
        if (st.getInstance()!=getInstance()) { throw new SystemImplementationException("State Instance/Item Instance mismatch"); }
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

    @Override
    protected int getNameCacheTime() {
        return 900;
    }

    @Nonnull
    @Override
    public String getTableName() {
        return "items";
    }

    /**
     * Factory style constructor
     *
     * @param id the ID number we want to get
     *
     * @return An Instance representation
     */
    @Nonnull
    public static Item get(@Nonnull final Integer id) {
        return (Item) factoryPut("Item",id,new Item(id));
    }
    protected Item(final int id) { super(id); }

    public Instance getInstance() { return Instance.get(getInt("instanceid")); }

    public String description() { return getString("description"); }
    public void description(String newDescription) { set("description",newDescription); }

    public int weight() { return getInt("weight"); }
    public void weight(int newWeight) { set("weight",newWeight); }


}
