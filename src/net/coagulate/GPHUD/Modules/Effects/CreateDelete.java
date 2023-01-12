package net.coagulate.GPHUD.Modules.Effects;

import net.coagulate.GPHUD.Data.Audit;
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
import javax.annotation.Nullable;

public class CreateDelete {
	// ---------- STATICS ----------
	@Command.Commands(description="Creates a new effect",
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Effects.Create",
	                  permitScripting=false,
	                  permitExternal=false,
	                  permitObject=false)
	public static final Response create(final State st,
	                                    @Argument.Arguments(name="name",
	                                                        description="Name of the new effect",
	                                                        type=Argument.ArgumentType.TEXT_CLEAN,
	                                                        max=64) @Nonnull final String name,
	                                    @Argument.Arguments(name="metadata",
	                                                        description="Attached metadata",
	                                                        type=Argument.ArgumentType.TEXT_ONELINE,
	                                                        max=1024,
	                                                        mandatory=false) @Nullable String metaData) {
		if (metaData==null) {
			metaData="";
		}
		Effect.create(st,name,metaData);
		return new OKResponse("Created new effect "+name);
	}
	
	@URL.URLs(url="/configuration/Effects/Create")
	public static void createPage(final State st,final SafeMap parameters) {
		Modules.simpleHtml(st,"Effects.Create",parameters);
	}
	
	@Command.Commands(description="Deletes an effect",
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Effects.Delete",
	                  permitScripting=false,
	                  permitExternal=false,
	                  permitObject=false)
	public static final Response delete(final State st,
	                                    @Argument.Arguments(name="name",
	                                                        description="Name of the effect to delete",
	                                                        type=Argument.ArgumentType.EFFECT,
	                                                        max=64) @Nonnull final Effect name) {
		name.delete(st);
		return new OKResponse("Deleted effect "+name);
	}
	
	@URL.URLs(url="/configuration/Effects/Delete")
	public static void deletePage(final State st,final SafeMap parameters) {
		Modules.simpleHtml(st,"Effects.Delete",parameters);
	}
	
	@Command.Commands(description="Set MetaData on an effect",
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Effects.Edit",
	                  permitScripting=false,
	                  permitExternal=false,
	                  permitObject=false)
	public static final Response setMetaData(final State st,
	                                         @Argument.Arguments(name="effect",
	                                                             description="The effect to alter",
	                                                             type=Argument.ArgumentType.EFFECT,
	                                                             max=64) @Nonnull final Effect effect,
	                                         @Argument.Arguments(name="metadata",
	                                                             description="Metadata",
	                                                             type=Argument.ArgumentType.TEXT_ONELINE,
	                                                             max=1024,
	                                                             mandatory=false) @Nullable String metaData) {
		effect.validate(st);
		if (metaData==null) {
			metaData="";
		}
		final String oldMetaData=effect.getMetaData();
		effect.setMetaData(metaData);
		Audit.audit(st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            null,
		            "Edit",
		            "MetaData",
		            oldMetaData,
		            metaData,
		            "User set effect metadata");
		return new OKResponse("Metadata updated for "+effect);
	}
	
	@URL.URLs(url="/configuration/Effects/SetMetaData")
	public static void setMetaDataPage(final State st,final SafeMap parameters) {
		Modules.simpleHtml(st,"Effects.SetMetaData",parameters);
	}
}
