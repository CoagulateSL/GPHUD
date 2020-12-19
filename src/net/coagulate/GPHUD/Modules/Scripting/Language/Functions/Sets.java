package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.CharacterSet;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCList;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.Map;

public class Sets {
    @Nonnull
    @GSFunctions.GSFunction(description="Adds (or subtracts) from a set",
                            returns="Integer - Number of this element in set after modification",
                            parameters="Character character - Character to alter<br>"+
                                    "String set - Name of set to modify<br>"+
                                    "String element - Name of element to alter in set<br>"+
                                    "Integer amount - Ammount to alter quantity by (may be negative)",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCInteger gsSetAdd(@Nonnull final State st,
                                    @Nonnull final GSVM vm,
                                    @Nonnull final BCCharacter character,
                                    @Nonnull final BCString set,
                                    @Nonnull final BCString element,
                                    @Nonnull final BCInteger amount) {
        CharacterSet characterSet=getSet(st,character,set);
        int oldValue=characterSet.count(element.getContent());
        int newAmount=characterSet.add(element.getContent(), amount.getContent());
        Audit.audit(true,st, Audit.OPERATOR.CHARACTER,null,character.getContent(),"gsSetAdd", set.getContent(),""+oldValue, ""+newAmount, "Added "+amount.getContent()+" "+element.getContent()+" to set, changing total from "+oldValue+" to "+newAmount);
        return new BCInteger(null,newAmount);
    }

    @Nonnull
    @GSFunctions.GSFunction(description="Sets number of element in a set",
                            returns="Integer - Number of this element in set after modification",
                            parameters="Character character - Character to alter<br>"+
                                    "String set - Name of set to modify<br>"+
                                    "String element - Name of element to alter in set<br>"+
                                    "Integer amount - Ammount to set quantity to",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCInteger gsSetSet(@Nonnull final State st,
                                     @Nonnull final GSVM vm,
                                     @Nonnull final BCCharacter character,
                                     @Nonnull final BCString set,
                                     @Nonnull final BCString element,
                                     @Nonnull final BCInteger amount) {
        CharacterSet characterSet=getSet(st,character,set);
        int oldValue=characterSet.count(element.getContent());
        characterSet.set(element.getContent(), amount.getContent());
        Audit.audit(true,st, Audit.OPERATOR.CHARACTER,null,character.getContent(),"gsSetSet", set.getContent(),""+oldValue, ""+(amount.getContent()), "Added "+amount.getContent()+" "+element.getContent()+" to set, changing total from "+oldValue+" to "+amount.getContent());
        return amount;
    }
    @Nonnull
    @GSFunctions.GSFunction(description="Get the quantity of an element in a set, or zero if not present",
                            returns="Integer - Number of this element in set",
                            parameters="Character character - Character to alter<br>"+
                                    "String set - Name of set to modify<br>"+
                                    "String element - Name of element to alter in set",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCInteger gsSetGet(@Nonnull final State st,
                                     @Nonnull final GSVM vm,
                                     @Nonnull final BCCharacter character,
                                     @Nonnull final BCString set,
                                     @Nonnull final BCString element) {
        CharacterSet characterSet=getSet(st,character,set);
        int value=characterSet.count(element.getContent());
        return new BCInteger(null,value);
    }
    @Nonnull
    @GSFunctions.GSFunction(description="Wipes the contents of a set for a character",
                            returns="Integer - Total quantity of elements formerly in the set (sum of all quantities)",
                            parameters="Character character - Character to alter<br>"+
                                    "String set - Name of set to modify",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCInteger gsSetWipe(@Nonnull final State st,
                                     @Nonnull final GSVM vm,
                                     @Nonnull final BCCharacter character,
                                     @Nonnull final BCString set) {
        CharacterSet characterSet=getSet(st,character,set);
        int elements=characterSet.countElements();
        int totalQuantity=characterSet.countTotal();
        characterSet.wipe();
        Audit.audit(true,st, Audit.OPERATOR.CHARACTER,null,character.getContent(),"gsSetWipe", set.getContent(),null, null, "Wiped set, formerly containing "+elements+" elements totalling "+totalQuantity+" quantity");
        return new BCInteger(null,totalQuantity);
    }
    @Nonnull
    @GSFunctions.GSFunction(description="Copies (adds) one set to another",
                            returns="Integer - Total number of items (quantities) in the target set",
                            parameters="Character character - Character to alter<br>"+
                                    "String sourceset - Name of set to copy from<br>"+
                                    "String destinationset - Name of set to copy to",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCInteger gsSetCopy(@Nonnull final State st,
                                     @Nonnull final GSVM vm,
                                     @Nonnull final BCCharacter character,
                                     @Nonnull final BCString sourceSet,
                                     @Nonnull final BCString destinationSet) {
        CharacterSet source=getSet(st,character,sourceSet);
        CharacterSet destination=getSet(st,character,sourceSet);
        for(Map.Entry<String,Integer> element:source.elements().entrySet()) {
            destination.add(element.getKey(),element.getValue());
        }
        int totalItems=destination.countElements();
        int totalQuantity=destination.countTotal();
        Audit.audit(true,st, Audit.OPERATOR.CHARACTER,null,character.getContent(),"gsSetCopy", destinationSet.getContent(),null, null, "Copied set "+sourceSet.getContent()+" to "+destinationSet.getContent()+" which now contains "+totalItems+" totalling quantity "+totalQuantity);
        return new BCInteger(null,totalQuantity);
    }
    @Nonnull
    @GSFunctions.GSFunction(description="Returns a list of all elements in the set",
                            returns="List - A list of strings, consisting of the elements in the set",
                            parameters="Character character - Character to query<br>"+
                                    "String set - Name of set to list",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCList gsSetList(@Nonnull final State st,
                                   @Nonnull final GSVM vm,
                                   @Nonnull final BCCharacter character,
                                   @Nonnull final BCString set) {
        CharacterSet source=getSet(st,character,set);
        BCList list=new BCList(null);
        for(Map.Entry<String,Integer> element:source.elements().entrySet()) {
            list.add(new BCString(null,element.getKey()));
        }
        return list;
    }
    @Nonnull
    @GSFunctions.GSFunction(description="Returns a list of all elements in the set along with their quantity",
                            returns="List - A list of strings, and integers consisting of the elements and quantities in the set",
                            parameters="Character character - Character to query<br>"+
                                    "String set - Name of set to map",
                            notes="Even numbered list indexes (starting at 0) are Strings, odd numbered are Integers (the quantities)",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCList gsSetMap(@Nonnull final State st,
                                   @Nonnull final GSVM vm,
                                   @Nonnull final BCCharacter character,
                                   @Nonnull final BCString set) {
        CharacterSet source=getSet(st,character,set);
        BCList list=new BCList(null);
        for(Map.Entry<String,Integer> element:source.elements().entrySet()) {
            list.add(new BCString(null,element.getKey()));
            list.add(new BCInteger(null,element.getValue()));
        }
        return list;
    }
    @Nonnull
    @GSFunctions.GSFunction(description="Returns a count of the distinct elements (regardless of quantity) in the set",
                            returns="Integer - Number of elements known in the set",
                            parameters="Character character - Character to count<br>"+
                                    "String set - Name of set to count",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCInteger gsSetCount(@Nonnull final State st,
                                  @Nonnull final GSVM vm,
                                  @Nonnull final BCCharacter character,
                                  @Nonnull final BCString set) {
        CharacterSet source=getSet(st,character,set);
        return new BCInteger(null,source.countElements());
    }
    @Nonnull
    @GSFunctions.GSFunction(description="Returns a count of the total number of things (counting quantities) in the set",
                            returns="Integer - Total of all quantities in the set",
                            parameters="Character character - Character to count<br>"+
                                    "String set - Name of set to count",
                            notes="",
                            privileged=false,
                            category= GSFunctions.SCRIPT_CATEGORY.SETS)
    public static BCInteger gsSetQuantity(@Nonnull final State st,
                                       @Nonnull final GSVM vm,
                                       @Nonnull final BCCharacter character,
                                       @Nonnull final BCString set) {
        CharacterSet source=getSet(st,character,set);
        return new BCInteger(null,source.countTotal());
    }
    private static CharacterSet getSet(State st, BCCharacter character, BCString setName) {
        // find Attribute by name
        Attribute attribute=Attribute.find(st.getInstance(),setName.getContent());
        // Attribute must be a set
        if (!(attribute.getType()== Attribute.ATTRIBUTETYPE.SET)) { throw new UserInputStateException("Attribute "+attribute.getName()+" is of type "+attribute.getType()+" not SET"); }
        // Attribute must belong to instance (!)
        if (!(attribute.getInstance()==st.getInstance())) { throw new SystemImplementationException("Attribute "+attribute+" is not from instance "+st.getInstanceString()); }
        // check character is of right instance
        if (!(st.getInstance()==character.getContent().getInstance())) { throw new SystemImplementationException("Target character "+character.getContent()+" is not from instance "+st.getInstanceString()); }
        return new CharacterSet(character.getContent(),attribute);
    }
}
