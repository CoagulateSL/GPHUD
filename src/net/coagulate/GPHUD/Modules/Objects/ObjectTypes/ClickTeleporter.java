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

/**
 * An implementation of the teleporter that activates on click
 */
public class ClickTeleporter extends Teleporter {
	ClickTeleporter(final State st,@Nonnull final ObjType object) {
		super(st,object);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explainHtml() {
		return "A Click Teleporter : Teleport user on activation to "+json.optString("teleporttarget","(unset)");
	}
	
	@Nonnull
	public String explainText() {
		return explainHtml();
	}
	
	@Override
	public void payload(final State st,
	                    @Nonnull final JSONObject response,
	                    @Nonnull final Region region,
	                    @Nullable final String url) {
		super.payload(st,response,region,url);
		if (json.has("maxdistance")) {
			response.put("maxdistance",json.get("maxdistance"));
		}
	}
	
	@Nonnull
	@Override
	public MODE mode() {
		return MODE.CLICKABLE;
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
		boolean changed=false;
		changed=updateDistance(st)||changed;
		changed=updateTeleport(st)||changed;
		if (changed) {
			object.setBehaviour(json);
		}
	}
	
	@Nonnull
	@Override
	public Response click(@Nonnull final State st,@Nonnull final Char clicker,@Nonnull final Float distance) {
		return execute(st,clicker);
	}
	
}
