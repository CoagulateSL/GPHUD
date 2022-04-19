package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.*;

import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.SET;

public class CharacterSet {
    protected final Char character;
    protected final Attribute set;

    /**
     * Create a reference to a particular set on a character.
     *
     * @param character The character owning the set
     * @param set       The attribute defining the set
     */
    public CharacterSet(@Nonnull final Char character, @Nonnull final Attribute set) {
        this.character = character;
        this.set = set;
        checks();
    }

    private void checks() {
        if (set.getType() != SET) {
            throw new SystemImplementationException("Accessing a set of a non SET attribute type " + set.getName() + " is " + set.getType());
        }
        if (set.getInstance() != character.getInstance()) {
            throw new SystemImplementationException("SetAttribute/Character instance mismatch");
        }
    }

    protected CharacterSet(@Nonnull final Char character, @Nonnull final Attribute set, final boolean noChecks) {
        this.character = character;
        this.set = set;
        if (!noChecks) {
            checks();
        }
    }

    protected static DBConnection db() { return GPHUD.getDB(); }
    /** Count the number of distinct elements (text strings) in the set
     *
     * @return The number of different types of thing (elements) in this set
     */
    public int countElements() {
        return db().dqiNotNull("select count(*) from charactersets where characterid=? and attributeid=?",character.getId(),set.getId());
    }

    /**
     * Count the total number of things in the set (total of all quantities)
     *
     * @return The number of items (total quantity) in this set
     */
    public int countTotal() {
        final Integer result = db().dqi("select sum(qty) from charactersets where characterid=? and attributeid=?", character.getId(), set.getId());
        if (result == null) {
            return 0;
        }
        return result;
    }

    /**
     * Returns the number of (quantity of) the passed element contained in this set
     */
    public int count(@Nonnull final String element) {
        final Results rows = db().dq("select qty from charactersets where characterid=? and attributeid=? and element=?", character.getId(), set.getId(), element);
        if (rows.empty()) {
            return 0;
        }
        int total = 0;
        for (final ResultsRow row : rows) {
            total = total + row.getInt("qty");
        }
        return total;
    }

    /**
     * Adds (or subtracts with a negative) a quantity to an element
     *
     * @param element Element from the set
     * @param add     Quantity to add (or remove if negative)
     * @return The new quantity
     */
    public int add(@Nonnull final String element, final int add) {
        int count = count(element);
        count = count + add;
        set(element, count);
        return count;
    }

    /**
     * Sets a quantity of an element
     *
     * @param element Element from the set
     * @param setTo   Quantity to set to
     */
    public void set(@Nonnull final String element, final int setTo) {
        if (setTo == 0) {
            delete(element);
            return;
        }
        db().d("insert into charactersets(characterid,attributeid,element,qty) values(?,?,?,?) on duplicate key update qty=?", character.getId(), set.getId(), element, setTo, setTo);
    }


    /**
     * Remove an element from a set
     *
     * @param element Element to remove entirely from the set
     */
    public void delete(@Nonnull final String element) {
        db().d("delete from charactersets where characterid=? and attributeid=? and element=?",character.getId(),set.getId(),element);
    }

    /** Retrieve a set of the distinct elements in this set (...)
     *
     * @return A set of strings for all the elements in this set
     */
    public Set<String> list() {
        final Set<String> returnSet = new TreeSet<>();
        for (final ResultsRow row : db().dq("select element from charactersets where characterid=? and attributeid=?", character.getId(), set.getId())) {
            returnSet.add(row.getString("element"));
        }
        return returnSet;
    }

    public static Set<Attribute> getAll(@Nonnull final State state) { return getAll(state.getInstance()); }
    public static Set<Attribute> getAll(@Nonnull final Instance instance) {
        final Set<Attribute> attributes = Attribute.getAttributes(instance);
        final Set<Attribute> sets = new HashSet<>();
        for (final Attribute attribute : attributes) {
            if (attribute.getType() == SET) {
                sets.add(attribute);
            }
        }
        return sets;
    }

    /** Retrieve a map of the elements and their quantity ; i.e. the set
     *
     * @return A map of strings (element names) and quantities from the set.
     */
    public Map<String, Integer> elements() {
        final Map<String, Integer> returnMap = new TreeMap<>();
        for (final ResultsRow row : db().dq("select element,qty from charactersets where characterid=? and attributeid=?", character.getId(), set.getId())) {
            returnMap.put(row.getString("element"), row.getInt("qty"));
        }
        return returnMap;
    }

    public void wipe() {
        db().d("delete from charactersets where characterid=? and attributeid=?",character.getId(),set.getId());
    }

    /** Returns the set as a formatted string, "1xThing, 2xThing2, 3xThing3" etc
     *
     * @return The formatted set contents
     */
    public String textList() {
        final StringBuilder output=new StringBuilder();
        boolean notFirst = false;
        for (final Map.Entry<String, Integer> element : elements().entrySet()) {
            if (notFirst) {
                output.append(", ");
            }
            output.append(element.getValue()).append("x").append(element.getKey());
            notFirst=true;
        }
        return output.toString();
    }
}
