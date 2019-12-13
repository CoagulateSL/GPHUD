package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class ClickTeleporter extends Teleporter {
	ClickTeleporter(State st, @Nonnull ObjectTypes object) {
		super(st,object);
	}

	@Nonnull
	@Override
	public String explainHtml() {
		return "A Click Teleporter : Teleport user on activation to "+json.optString("teleporttarget","(unset)");
	}
	@Nonnull
	public String explainText() { return explainHtml(); }

	@Nonnull
	@Override
	public MODE mode() { return MODE.CLICKABLE; }

	@Nonnull
	@Override
	public Response click(@Nonnull State st, @Nonnull Char clicker) {
		return execute(st,clicker);
	}

	@Override
	public void editForm(@Nonnull State st) {
		super.editForm(st);
	}

	@Override
	public void update(@Nonnull State st) {
		super.update(st);
	}

}
