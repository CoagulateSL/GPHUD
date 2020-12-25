package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Inventory extends TableRow {
    @Nonnull private final Attribute inventory;
    public Inventory(@Nonnull final State st, @Nonnull final Attribute inventory) {
        if (inventory.getType()!= Attribute.ATTRIBUTETYPE.INVENTORY) {
            throw new SystemImplementationException("Attribute "+inventory+" is not an INVENTORY attribute");
        }
        this.inventory=inventory;
    }

    public CharacterSet set(@Nonnull final Char character) {
        return new CharacterSet(character,inventory);
    }

    public int maxItems() { return getInt("maxitems"); }
    public void maxItems(int newMax) { set("maxitems",newMax); }
    public int maxQuantity() { return getInt("maxquantity"); }
    public void maxQuantity(int newMax) { set("maxquantity",newMax); }
    public int maxWeight() { return getInt("maxweight"); }
    public void maxWeight(int newMax) { set("maxweight",newMax); }
    public boolean defaultAllow() { return getBool("defaultallow"); }
    public void defaultAllow(boolean newAllow) { set("defaultallow",newAllow); }

    @Nonnull
    @Override
    public String getIdColumn() {
        return "attributeid";
    }

    @Override
    public void validate(@Nonnull State st) {
    }

    @Nullable
    @Override
    public String getNameField() {
        return null;
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
        return 0;
    }

    @Nonnull
    @Override
    public String getTableName() {
        return "inventoryconfiguration";
    }
}
