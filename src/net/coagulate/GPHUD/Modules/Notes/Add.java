package net.coagulate.GPHUD.Modules.Notes;

import net.coagulate.GPHUD.Data.AdminNotes;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

public class Add {

	@Command.Commands(description = "Adds a note to a character",context = Command.Context.AVATAR,requiresPermission = "Notes.Add")
	public static Response character(State st,
	                                 @Argument.Arguments(description="Character to log note against",type = Argument.ArgumentType.CHARACTER)
		                                    Char character,
	                                 @Argument.Arguments(description = "Share note with the user",type = Argument.ArgumentType.BOOLEAN)
	                                        Boolean shared,
	                                 @Argument.Arguments(description="Note to record",type=Argument.ArgumentType.TEXT_ONELINE,max=4096)
	                                        String note)
	{
		AdminNotes.add(st.getInstance(),st.getAvatar(),character.getOwner(),character,note,!shared);
		return new OKResponse((shared?"Shared":"Admin only")+" character note added.");
	}

	@Command.Commands(description = "Adds a note to an avatar",context = Command.Context.AVATAR,requiresPermission = "Notes.Add")
	public static Response avatar(State st,
	                                 @Argument.Arguments(description="Avatar to log note against",type = Argument.ArgumentType.AVATAR)
			                                 User target,
	                                 @Argument.Arguments(description = "Share note with the user",type = Argument.ArgumentType.BOOLEAN)
			                                 Boolean shared,
	                                 @Argument.Arguments(description="Note to record",type=Argument.ArgumentType.TEXT_ONELINE,max=4096)
			                                 String note)
	{
		AdminNotes.add(st.getInstance(),st.getAvatar(),target,null,note,!shared);
		return new OKResponse((shared?"Shared":"Admin only")+" avatar note added.");
	}

}
