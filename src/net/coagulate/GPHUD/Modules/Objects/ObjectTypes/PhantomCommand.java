package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class PhantomCommand extends ObjectType {
	protected PhantomCommand(final State st,@Nonnull final ObjType object) {
		super(st,object);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explainHtml() {
		return "Runs command "+json.optString("command","unset")+" on collision; becomes phantom";
	}
	
	@Override
	public void editForm(@Nonnull final State st) {
		final Table t=new Table();
		t.add("Command").add(DropDownList.getCommandsList(st,"command",true));
		t.openRow();
		t.add(new Cell(new Button("Submit"),2));
		st.form().add(t);
	}
	
	@Override
	public void update(@Nonnull final State st) {
		final String command=st.postMap().get("command");
		if (!command.equals(json.optString("command",""))) {
			json.put("command",st.postMap().get("command"));
			object.setBehaviour(json);
		}
	}
	
	@Nonnull
	@Override
	public String explainText() {
		return explainHtml();
	}
	
	@Nonnull
	@Override
	public MODE mode() {
		return MODE.PHANTOM;
	}
	
	@Nonnull
	@Override
	public Response collide(@Nonnull final State st,@Nonnull final Char collider) {
		if (json.optString("command","").isEmpty()) {
			return new ErrorResponse("Command to invoke is not configured in this object type");
		}
		final JSONObject resp=Modules.getJSONTemplate(st,json.getString("command"));
		new Transmission(collider,resp).start();
		return new JSONResponse(new JSONObject());
	}
}
