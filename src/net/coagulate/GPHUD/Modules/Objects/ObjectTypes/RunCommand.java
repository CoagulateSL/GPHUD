package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjectTypes;
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

public class RunCommand extends ObjectType {
	protected RunCommand(State st, ObjectTypes object) {
		super(st, object);
	}

	@Override
	public String explainHtml() {
		return "Runs command "+json.optString("command","unset")+" on click";
	}

	@Override
	public void editForm(State st) {
		Table t=new Table();
		t.add("Command").add(DropDownList.getCommandsList(st,"command",true));
		t.openRow();
		t.add(new Cell(new Button("Submit"),2));
		st.form.add(t);
	}

	@Override
	public void update(State st) {
		String command=st.postmap.get("command");
		if (!command.equals(json.optString("command",""))) {
			json.put("command",st.postmap.get("command"));
			object.setBehaviour(json);
		}
	}

	@Override
	public String explainText() {
		return explainHtml();
	}

	@Override
	public MODE mode() {
		return MODE.CLICKABLE;
	}

	@Override
	public Response click(State st, Char clicker) {
		if (json.optString("command","").isEmpty()) { return new ErrorResponse("Command to invoke is not configured in this object type"); }
		JSONObject resp = Modules.getJSONTemplate(st, json.getString("command"));
		new Transmission(clicker,resp).start();
		return new JSONResponse(new JSONObject());
	}
}
