package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class PhantomScript extends RunScript {
	protected PhantomScript(final State st,@Nonnull final ObjType object) {
		super(st,object);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explainHtml() {
		return "Runs script "+json.optString("script","unset")+" on collision; sets phantom";
	}
	
	@Nonnull
	@Override
	public MODE mode() {
		return MODE.PHANTOM;
	}
	
	@Nonnull
	@Override
	public Response collide(final State st,final Char collider) {
		return click(st,collider,(float)0);
	}
}
