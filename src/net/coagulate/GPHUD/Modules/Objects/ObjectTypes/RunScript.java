package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class RunScript extends ObjectType {
	protected RunScript(final State st,
                        @Nonnull final ObjType object) {
		super(st,object);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explainHtml() {
		return "Runs script "+json.optString("script","unset")+" on click";
	}

	@Override
	public void editForm(@Nonnull final State st) {
		final Table t=new Table();
		t.add("Script").add(DropDownList.getScriptsList(st,"script"));
		t.openRow();
		t.add(new Cell(new Button("Submit"),2));
		st.form().add(t);
	}

	@Override
	public void update(@Nonnull final State st) {
		final String script=st.postMap().get("script");
		if (!script.equals(json.optString("script",""))) {
			json.put("script",st.postMap().get("script"));
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
		return MODE.CLICKABLE;
	}

	@Nonnull
	@Override
	public Response click(@Nonnull final State st,
	                      @Nonnull final Char clicker) {
		if (json.optString("script","").isEmpty()) {
			return new ErrorResponse("Script to invoke is not configured in this object type");
		}
		Script script=Script.findNullable(st,json.getString("script"));
		if (script==null) {
			return new ErrorResponse("Script '" + json.getString("script") + "' does not exist");
		}
		script.validate(st);
		final GSVM vm=new GSVM(script.getByteCode());
		vm.introduce("CALLER",new BCCharacter(null,clicker));
		vm.introduce("CALLERUUID",new BCString(null,clicker.getPlayedByNullable().getUUID()));
		populateVmVariables(st,vm);
		return new JSONResponse(vm.execute(st).asJSON(st));
	}
}
