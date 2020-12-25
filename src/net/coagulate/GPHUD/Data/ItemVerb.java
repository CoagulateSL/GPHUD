package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemVerb extends TableRow {
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

    @Override
    protected int getNameCacheTime() {
        return 900;
    }

    @Nonnull
    @Override
    public String getTableName() {
        return "itemverbs";
    }

    /**
     * Factory style constructor
     *
     * @param id the ID number we want to get
     *
     * @return An Instance representation
     */
    @Nonnull
    public static ItemVerb get(@Nonnull final Integer id) {
        return (ItemVerb) factoryPut("ItemVerb",id,new ItemVerb(id));
    }
    protected ItemVerb(final int id) { super(id); }

    public Instance getInstance() { return Instance.get(getInt("instanceid")); }

    public String description() { return getString("description"); }
    public void description(String newDescription) { set("description",newDescription); }
    public String verb() { return getString("verb"); }
    public void verb(String newDescription) { set("verb",newDescription); }
    public JSONObject payload() { return new JSONObject(getString("payload")); }
    public void payload(JSONObject newDescription) { set("payload",newDescription.toString()); }

}
