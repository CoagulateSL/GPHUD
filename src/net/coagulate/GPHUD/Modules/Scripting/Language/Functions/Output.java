package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Data.Landmarks;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCInteger;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSResourceUnavailableException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

public class Output {

	@GSFunctions.GSFunction(description = "Causes the HUD to speak in local chat, as the character",
			parameters = "Character - character whose HUD will emit the message<br>String - message to speak",
			returns = "Integer - The number 0",
			notes = "Messages are stacked up, per user, until the script completes or is suspended")
	public static BCInteger gsSayAsChar(State st, GSVM vm, BCCharacter target, BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { throw new GSResourceUnavailableException("Character "+target+" is not online"); }
		vm.queueSayAs(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}
	@GSFunctions.GSFunction(description = "Causes the HUD to send a message to its wearer",
			parameters = "Character - character whose HUD will message the wearer (the character themselves)<br>String - message to pass",
			returns = "Integer - The number 0",
			notes = "Messages are stacked up, per user, until the script completes or is suspended")
	public static BCInteger gsSayToChar(State st, GSVM vm, BCCharacter target, BCString message) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { throw new GSResourceUnavailableException("Character "+target+" is not online"); }
		vm.queueOwnerSay(target.getContent(),message.getContent());
		return new BCInteger(null,0);
	}

	@GSFunctions.GSFunction(description = "Teleports the plater",parameters = "BCCharacter - who to teleport<br>BCString - Landmark name to teleport to",returns="Integer - 0",notes="")
	public static BCInteger gsTeleport(State st,GSVM vm,BCCharacter target,BCString landmark) {
		if (vm.simulation) { return new BCInteger(null,0); }
		if (!target.isOnline()) { throw new GSResourceUnavailableException("Character "+target+" is not online"); }
		Landmarks t=st.getInstance().getLandmark(landmark.getContent());
		if (t==null) { throw new GSResourceUnavailableException("Can not find landmark "+landmark.getContent()); }
		vm.queueTeleport(target.getContent(),t.getHUDRepresentation());
		return new BCInteger(null,0);
	}
}
