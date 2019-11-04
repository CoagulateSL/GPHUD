package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidFunctionCall;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

public class KV {

	@GSFunctions.GSFunction(description = "Returns the value of a KV",returns = "String - The value requested",parameters = "Character character - The character to look the value up for (use any character if the value doesn't depend on the character)<br>String kvname - The name of the KV to lookup",notes = "")
	public static BCString gsGetKV(State st, GSVM vm, BCCharacter character, BCString kvname) {
		if (character.getContent().getInstance()!=st.getInstance()) { throw new GSInvalidFunctionCall("Character "+character+" belongs to a different instance!"); }
		State altstate=new State(character.getContent());
		return new BCString(null,altstate.getKV(kvname.getContent()).toString());
	}
}
