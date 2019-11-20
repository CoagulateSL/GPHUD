package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;

public class PhantomTeleporter extends Teleporter {
	PhantomTeleporter(State st, ObjectTypes object) {
		super(st,object);
	}

	@Override
	public String explainHtml() {
		return "A Phantom Teleporter : Teleport user on collision (intersection) to "+json.optString("teleporttarget","(unset)");
	}
	public String explainText() { return explainHtml(); }

	@Override
	public MODE mode() { return MODE.PHANTOM; }

	@Override
	public Response collide(State st, Char clicker) {
		return execute(st,clicker);
	}

	@Override
	public void editForm(State st) {
		super.editForm(st);
	}

	@Override
	public void update(State st) {
		super.update(st);
	}

}
