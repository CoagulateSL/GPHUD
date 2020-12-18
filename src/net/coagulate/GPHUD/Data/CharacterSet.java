package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.GPHUD;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CharacterSet {
    private final Char character;
    private final Attribute set;

    /** Create a reference to a particular set on a character.
     *
     * @param character The character owning the set
     * @param set The attribute defining the set
     */
    public CharacterSet(@Nonnull Char character,@Nonnull Attribute set) {
        if (set.getType()!= Attribute.ATTRIBUTETYPE.SET) {
            throw new SystemImplementationException("Accessing a set of a non SET attribute type "+set.getName()+" is "+set.getType());
        }
        if (set.getInstance()!=character.getInstance()) {
            throw new SystemImplementationException("SetAttribute/Character instance mismatch");
        }
        this.character=character;
        this.set=set;
    }

    private static DBConnection db() { return GPHUD.getDB(); }
    /** Count the number of distinct elements (text strings) in the set
     *
     * @return The number of different types of thing (elements) in this set
     */
    public int countElements() {
        return db().dqiNotNull("select count(*) from charactersets where characterid=? and attributeid=?",character.getId(),set.getId());
    }

    /** Count the total number of things in the set (total of all quantities)
     *
     * @return The number of items (total quantity) in this set
     */
    public int countTotal() {
        return db().dqiNotNull("select sum(qty) from charactersets where characterid=? and attributeid=?",character.getId(),set.getId());
    }
    /** Returns the number of (quantity of) the passed element contained in this set */
    public int count(@Nonnull String element) {
        Results rows = db().dq("select qty from charactersets where characterid=? and attributeid=? and element=?", character.getId(), set.getId(), element);
        if (rows.empty()) { return 0; }
        int total=0;
        for (ResultsRow row:rows) {
            total=total+row.getInt("qty");
        }
        return total;
    }

    /** Adds (or subtracts with a negative) a quantity to an element
     *
     * @param element Element from the set
     * @param add Quantity to add (or remove if negative)
     * @return The new quantity
     */
    public int add(@Nonnull String element,int add) {
        int count=count(element);
        count=count+add;
        set(element,count);
        return count;
    }

    /** Sets a quantity of an element
     *
     * @param element Element from the set
     * @param setTo Quantity to set to
     */
    public void set(@Nonnull String element,int setTo) {
        if (setTo==0) { delete(element); return; }
        db().d("replace into charactersets(characterid,attributeid,element,qty) values(?,?,?,?)",character.getId(),set.getId(),element,setTo);
    }


    /** Remove an element from a set
     *
     * @param element Element to remove entirely from the set
     */
    public void delete(@Nonnull String element) {
        db().d("delete from charactersets where characterid=? and attributeid=? and element=?",character.getId(),set.getId(),element);
    }

    /** Retrieve a set of the distinct elements in this set (...)
     *
     * @return A set of strings for all the elements in this set
     */
    public Set<String> list() {
        Set<String> returnSet=new TreeSet<>();
        for (ResultsRow row:db().dq("select element from charactersets where characterid=? and attributeid=?",character.getId(),set.getId())) {
            returnSet.add(row.getString("element"));
        }
        return returnSet;
    }

    /** Retrieve a map of the elements and their quantity ; i.e. the set
     *
     * @return A map of strings (element names) and quantities from the set.
     */
    public Map<String,Integer> elements() {
        Map<String,Integer> returnMap=new TreeMap<>();
        for (ResultsRow row:db().dq("select element,qty from charactersets where characterid=? and attributeid=?",character.getId(),set.getId())) {
            returnMap.put(row.getString("element"),row.getInt("qty"));
        }
        return returnMap;
    }
}
