package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;

public class ClickTeleporter extends Teleporter {
	ClickTeleporter(State st, ObjectTypes object) {
		super(st,object);
	}

	@Override
	public String explainHtml() {
		return "A Click Teleporter : Teleport user on activation to "+json.optString("teleporttarget","(unset)");
	}
	public String explainText() { return explainHtml(); }

	@Override
	public MODE mode() { return MODE.CLICKABLE; }

	@Override
	public Response click(State st, Char clicker) {
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
