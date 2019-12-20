package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidFunctionCall;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class KV {

	@Nonnull
	@GSFunctions.GSFunction(description="Returns the value of a KV", returns="String - The value requested", parameters="Character character - The character to look the "+
			"value up for (use any character if the value doesn't depend on the character)<br>String kvname - The name of the KV to lookup", notes="<B>BEWARE:</b> This "+
			"function <B>ALWAYS</B> returns a String value, even if you're getting a number, so code like <br><pre>Integer value=gsGetKV(char,\"Characters.stat\")"+"+3</pre"+"><br>will return, for example, 13 if the character's stat is 1, as this is string addition rules<br>To cast the value to an integer, first do "+"<br><pre"+">Integer value=gsGetKV(char,\"characters.stat\"); Integer maths=value+3;</pre><bre>the initial value will cast the String to an Integer, which will make"+" the "+"maths in the 2nd line work properly (i.e. produce 6)")
	public static BCString gsGetKV(@Nonnull final State st,
	                               final GSVM vm,
	                               @Nonnull final BCCharacter character,
	                               @Nonnull final BCString kvname) {
		if (character.getContent().getInstance()!=st.getInstance()) {
			throw new GSInvalidFunctionCall("Character "+character+" belongs to a different instance!");
		}
		final State altstate=new State(character.getContent());
		return new BCString(null,altstate.getKV(kvname.getContent()).toString());
	}
}
