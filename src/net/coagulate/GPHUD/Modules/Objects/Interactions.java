package net.coagulate.GPHUD.Modules.Objects;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Obj;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class Interactions {
	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(description="Clicked Objects generate this command",
	                  context=Command.Context.AVATAR,
	                  permitJSON=false,
	                  permitUserWeb=false,
	                  permitConsole=false,
	                  permitScripting=false)
	public static Response clicked(@Nonnull final State st,
	                               @Argument.Arguments(description="Character clicking the object",
	                                                   type=Argument.ArgumentType.CHARACTER) final Char clicker) {

		final Obj object=Obj.findOrNull(st,st.objectkey);
		if (object==null) { return new ErrorResponse("This object is not properly registered with GPHUD(?)"); }
		final ObjType objecttype=object.getObjectType();
		if (objecttype==null) {
			return new ErrorResponse("This object is not configured with an object type in GPHUD");
		}
		final ObjectType ot=ObjectType.materialise(st,objecttype);
		return ot.click(st,clicker);
	}

	@Nonnull
	@Command.Commands(description="Collided/VolumeDetect Objects generate this command",
	                  context=Command.Context.AVATAR,
	                  permitJSON=false,
	                  permitUserWeb=false,
	                  permitConsole=false,
	                  permitScripting=false)
	public static Response collided(@Nonnull final State st,
	                                @Argument.Arguments(description="Character colliding with the object",
	                                                    type=Argument.ArgumentType.CHARACTER) final Char collider) {

		final Obj object=Obj.findOrNull(st,st.objectkey);
		if (object==null) { return new ErrorResponse("This object is not properly registered with GPHUD(?)"); }
		final ObjType objecttype=object.getObjectType();
		if (objecttype==null) {
			return new ErrorResponse("This object is not configured with an object type in GPHUD");
		}
		final ObjectType ot=ObjectType.materialise(st,objecttype);
		return ot.collide(st,collider);
	}
}
