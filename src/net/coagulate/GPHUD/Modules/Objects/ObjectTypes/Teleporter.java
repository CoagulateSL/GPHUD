package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Landmark;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Teleportation.TeleportCommands;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public abstract class Teleporter extends ObjectType {
	protected Teleporter(final State st,
	                     @Nonnull final ObjType object) {
		super(st,object);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String getTeleportTarget(@Nonnull final State st) {
		final Landmark landmark=Landmark.find(st.getInstance(),json.optString("teleporttarget","unset"));
		if (landmark==null) {
			throw new UserConfigurationException("Teleport target is not set on clickTeleporter "+object.getName());
		}
		return landmark.getHUDRepresentation(false);
	}

	public void editForm(@Nonnull final State st) {
		final Table t=new Table();
		t.add("Target Landmark").add(TeleportCommands.getDropDownList(st,"target",json.optString("teleporttarget","")));
		t.openRow();
		t.add("Teleporter says").add(new TextInput("teleportersays",json.optString("teleportersays","")));
		t.openRow();
		t.add("HUD says to wearer").add(new TextInput("hudsays",json.optString("hudsays","")));
		t.openRow();
		t.add(new Cell(new Button("Update"),2));
		st.form().add(t);
	}

	public void update(@Nonnull final State st) {
		if (!st.postMap().get("target").isEmpty() || !st.postMap().get("teleportersays").isEmpty() || !st.postMap().get("hudsays").isEmpty()) {
			boolean update=false;
			final String target=st.postMap().get("target");
			if (!target.equals(json.optString("teleporttarget",""))) {
				json.put("teleporttarget",target);
				update=true;
			}
			final String teleportersays=st.postMap().get("teleportersays");
			if (!target.equals(json.optString("teleportersays",""))) {
				json.put("teleportersays",teleportersays);
				update=true;
			}
			final String hudsays=st.postMap().get("hudsays");
			if (!target.equals(json.optString("hudsays",""))) {
				json.put("hudsays",hudsays);
				update=true;
			}
			if (update) {
				object.setBehaviour(json);
			}
		}
	}

	// ----- Internal Instance -----
	@Nonnull
	Response execute(@Nonnull final State st,
	                 @Nonnull final Char clicker) {
		if (!st.hasModule("Teleportation")) {
			throw new UserConfigurationException("Teleporter can not function ; teleportation module is disabled at this instance.");
		}
		final JSONObject doteleport=new JSONObject();
		doteleport.put("teleport",getTeleportTarget(st));
		final String hudsays=json.optString("hudsays","");
		if (!hudsays.isEmpty()) {
			JSONResponse.message(doteleport,hudsays);
		}
		new Transmission(clicker,doteleport).start();
		final String teleportersays=json.optString("teleportersays","");
		final JSONObject resp=new JSONObject();
		JSONResponse.say(resp,teleportersays);
		return new JSONResponse(resp);
	}
}
