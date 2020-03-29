package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Data.Effect;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class CreateDelete {
	@Command.Commands(description="Creates a new effect", context=Command.Context.AVATAR, requiresPermission="Effects.Create")
	public static final Response create(final State st,
	                                    @Argument.Arguments(description="Name of the new effect", type=Argument.ArgumentType.TEXT_CLEAN, max=64)
	                                    @Nonnull final String name) {
		Effect.create(st,name);
		return new OKResponse("Created new effect "+name);
	}

	@URL.URLs(url="/configuration/Effects/Create")
	public static void createPage(State st,
	                              SafeMap parameters) {
		Modules.simpleHtml(st,"Effects.Create",parameters);
	}

	@Command.Commands(description="Deletes an effect", context=Command.Context.AVATAR, requiresPermission="Effects.Delete")
	public static final Response delete(final State st,
	                                    @Argument.Arguments(description="Name of the effect to delete", type=Argument.ArgumentType.EFFECT, max=64)
	                                    @Nonnull final Effect name) {
		name.delete(st);
		return new OKResponse("Deleted effect "+name);
	}

	@URL.URLs(url="/configuration/Effects/Delete")
	public static void deletePage(State st,
	                              SafeMap parameters) {
		Modules.simpleHtml(st,"Effects.Delete",parameters);
	}


}
