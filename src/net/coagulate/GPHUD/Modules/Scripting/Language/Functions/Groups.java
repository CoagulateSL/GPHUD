package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Groups {
	@Nonnull
	@GSFunctions.GSFunction(description = "Gets the group name for a given attribute",returns = "String - name of group of appropriate subtype, or the empty string if none",notes = "",parameters = "Character - character to interrogate<br>String - type of group to get")
	public static BCString gsGetGroupByType(final State st, final GSVM vm, @Nonnull final BCCharacter target, @Nonnull final BCString grouptype) {
		final CharacterGroup group=target.getContent().getGroup(grouptype.getContent());
		String name="";
		if (group!=null) { name=group.getName(); }
		return new BCString(null,name);
	}
}
