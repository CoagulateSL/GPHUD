package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Modules.Characters.CharactersModule;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSResourceUnavailableException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

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
                                               final GSVM gsvm,
                                               final BCCharacter character) {
        final State astate = new State(character.getContent());
        return new BCInteger(null, CharactersModule.abilityPointsRemaining(astate));
    }

    @Nonnull
    @GSFunctions.GSFunction(description = "Gets the Avatar's name currently playing a character",
                            category = GSFunctions.SCRIPT_CATEGORY.CHARACTER,
                            notes = "Returns the empty string if not currently logged in",
                            parameters = "Character character - the character to query",
                            privileged = false,
                            returns = "String - Name of the avatar playing the character, or the empty string if not being played")
    public static BCString gsGetPlayersName(@Nonnull final State state,
                                            final GSVM gsvm,
                                            final BCCharacter character) {
        final User currentPlayer = character.getContent().getPlayedByNullable();
        String currentPlayerName = "";
        if (currentPlayer != null) {
            currentPlayerName = currentPlayer.getName();
        }
        return new BCString(null, currentPlayerName);
    }

    @Nonnull
    @GSFunctions.GSFunction(description = "Gets the Avatar's UUID currently playing a character",
                            category = GSFunctions.SCRIPT_CATEGORY.CHARACTER,
                            notes = "Returns the empty string if not currently logged in",
                            parameters = "Character character - the character to query",
                            privileged = false,
                            returns = "String - SL UUID of the avatar playing the character, or the empty string if not being played")
    public static BCString gsGetPlayersUUID(@Nonnull final State state,
                                            final GSVM gsvm,
                                            final BCCharacter character) {
        final User currentPlayer = character.getContent().getPlayedByNullable();
        String currentPlayerUUID = "";
        if (currentPlayer != null) {
            currentPlayerUUID = currentPlayer.getUUID();
        }
        return new BCString(null, currentPlayerUUID);
    }

    @Nonnull
    @GSFunctions.GSFunction(description = "Resolves a character by name",
                            category = GSFunctions.SCRIPT_CATEGORY.CHARACTER,
                            notes = "Returns a Character, throws an error if the character name is not valid",
                            parameters = "String name - the character name to lookup",
                            privileged = false,
                            returns = "Character - the appropriate Character object, erroring if resolution fails.")
    public static BCCharacter gsGetCharacter(@Nonnull final State state,
                                             final GSVM gsvm,
                                             final BCString name) {
        final Char character = Char.findNullable(state.getInstance(), name.getContent());
        if (character == null) {
            throw new GSResourceUnavailableException("Unable to resolve '" + name.getContent() + "' to a character.", true);
        }
        return new BCCharacter(null, character);
    }

}
