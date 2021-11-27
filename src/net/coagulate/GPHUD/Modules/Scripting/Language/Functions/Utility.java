package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Utility {
    @GSFunctions.GSFunction(description = "Check if a given variable name is defined",
                            category = GSFunctions.SCRIPT_CATEGORY.UTILITY,
                            privileged = false,
                            notes = "Checks if a variable is defined (as, if not, code will runtime error when loading a variable)",
                            parameters = "String - name of variable",
                            returns = "Integer - 1 if the variable exists, otherwise 0")
    public static BCInteger gsDefined(final State st,
                                   final GSVM vm,
                                   @Nonnull final BCString variableName) {
        if (vm.variables.containsKey(variableName.getContent())) {
            return new BCInteger(null,1);
        }
        return new BCInteger(null,0);
    }
}
