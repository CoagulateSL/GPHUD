package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClickTeleporter extends Teleporter {
	ClickTeleporter(final State st,
	                @Nonnull final ObjType object) {
		super(st,object);
	}

	// ---------- INSTANCE ----------
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
	public Response click(@Nonnull final State st,
	                      @Nonnull final Char clicker) {
		return execute(st,clicker);
	}

	@Override
	public void editForm(@Nonnull final State st) {
		final Table t=new Table();
		teleportEditForm(st,t);
		editFormDistance(st,t);
		t.add(new Cell(new Button("Update"),2));
		st.form().add(t);
	}

	@Override
	public void update(@Nonnull final State st) {
		boolean changed=updateDistance(st);
		changed=updateTeleport(st) || changed;
		if (changed) { object.setBehaviour(json); }
	}
	@Override
	public void payload(State st, @Nonnull JSONObject response, @Nonnull Region region, @Nullable String url) {
		super.payload(st, response, region, url);
		if (json.has("maxdistance")) { response.put("maxdistance",json.get("maxdistance")); }
	}

}
