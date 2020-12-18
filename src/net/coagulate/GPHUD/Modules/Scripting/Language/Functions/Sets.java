package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.CharacterSet;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

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
