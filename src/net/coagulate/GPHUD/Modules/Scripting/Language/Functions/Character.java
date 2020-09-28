package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Characters.CharactersModule;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Character {
    @Nonnull
    @GSFunctions.GSFunction(description = "Gets the number of available ability points",
                            category = GSFunctions.SCRIPT_CATEGORY.CHARACTER,
                            notes = "Returns zero if none are available.  May return a negative under some circumstances.",
                            parameters = "Character character - the character to query",
                            privileged = false,
                            returns = "Integer - number of ability points")
    public static BCInteger gsGetAbilityPoints(@Nonnull final State state,
                                               GSVM gsvm,
                                               BCCharacter character) {
        State astate=new State(character.getContent());
        return new BCInteger(null,CharactersModule.abilityPointsRemaining(astate));
    }

}
