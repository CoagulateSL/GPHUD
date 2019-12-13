package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Rename {
	@Nonnull
	@Command.Commands(context = Command.Context.AVATAR, description = "Rename a character", requiresPermission = "Characters.ForceRename")
	public static Response rename(State st,
	                              @Nonnull @Argument.Arguments(description = "Character to rename", type = Argument.ArgumentType.CHARACTER, max = 64)
			                              Char oldname,
	                              @Argument.Arguments(description = "New name for character", max = 40, type = Argument.ArgumentType.TEXT_ONELINE)
			                              String newname) {
		oldname.rename(newname);
		return new OKResponse("Character has been renamed");
	}
}
