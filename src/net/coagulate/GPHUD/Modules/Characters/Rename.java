package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Rename {
	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(context=Command.Context.AVATAR,
	                  description="Rename a character",
	                  requiresPermission="Characters.ForceRename",
	                  permitExternal=false)
	public static Response rename(final State st,
	                              @Nonnull
	                              @Argument.Arguments(name="oldname",
	                                                  description="Character to rename",
	                                                  type=Argument.ArgumentType.CHARACTER,
	                                                  max=64) final Char oldname,
	                              @Argument.Arguments(name="newname",
	                                                  description="New name for character",
	                                                  max=40,
	                                                  type=Argument.ArgumentType.TEXT_ONELINE) final String newname) {
		oldname.rename(st,newname,true);
		return new OKResponse("Character has been renamed");
	}
}
