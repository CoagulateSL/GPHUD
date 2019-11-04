package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

public class Input {

	@GSFunctions.GSFunction(description = "Triggers the character's HUD to select a nearby character",parameters = "None",notes = "",returns = "Character - a character the user selected")
	public static BCCharacter gsSelectCharacter(State st, GSVM vm, BCCharacter target, BCString message) {
		vm.queueSelectCharacter(target.getContent(),message.getContent());
		vm.suspend(st,target.getContent());
		return target;
	}
}
