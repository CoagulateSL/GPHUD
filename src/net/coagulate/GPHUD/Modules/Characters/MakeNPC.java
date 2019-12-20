package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MakeNPC {
	@Nonnull
	@Command.Commands(description="Convert the currently active character to an NPC.  YOU WILL LOSE CONTROL OF YOUR CHARACTER FOREVER.", requiresPermission="Characters"+
			".MakeNPC", context=Command.Context.CHARACTER, permitObject=false, permitScripting=false)
	public static Response makeNPC(@Nonnull final State st,
	                               @Nullable
	                               @Argument.Arguments(description="Name of the currently active character.  Used to confirm you know what you're doing.", type=
			                               Argument.ArgumentType.TEXT_ONELINE, max=64, mandatory=false)
			                               String confirmname) {
		// check the characters name
		if (confirmname==null) { confirmname=""; }
		final String name=st.getCharacter().getName();
		if (!name.equalsIgnoreCase(confirmname)) {
			// welp
			String commandline="/1Characters.MakeNPC ";
			if (name.contains(" ")) { commandline+="\""; }
			commandline+=name;
			if (name.contains(" ")) { commandline+="\""; }
			return new ErrorResponse("You must supply the current character's name as the parameter to this command, specifically run the following:\n"+commandline);
		}
		st.getCharacter().setOwner(User.getSystem());
		final JSONObject response=new JSONObject();
		response.put("reboot","Your character has become an NPC and you have been disconnected from it.  Rebooting to restore GPHUD services with a different character.");
		return new JSONResponse(response);
	}
}
