package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
		editFormScript(st,t);
		editFormDistance(st,t);
		t.add(new Cell(new Button("Submit"),2));
		st.form().add(t);
	}

	@Override
    public void payload(final State st, @Nonnull final JSONObject response, @Nonnull final Region region, @Nullable final String url) {
        super.payload(st, response, region, url);
        if (json.has("maxdistance")) {
            response.put("maxdistance", json.get("maxdistance"));
        }
    }

	@Override
	public void update(@Nonnull final State st) {
		boolean changed=false;
		changed=updateScript(st) || changed;
		changed=updateDistance(st) || changed;
		if (changed) { object.setBehaviour(json); }
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
        final Script script = Script.findNullable(st, json.getString("script"));
		if (script==null) {
			return new ErrorResponse("Script '" + json.getString("script") + "' does not exist");
		}
		script.validate(st);
		final GSVM vm=new GSVM(script);
		vm.introduce("CALLER",new BCCharacter(null,clicker));
		vm.introduce("CALLERUUID",new BCString(null,clicker.getPlayedBy().getUUID()));
		populateVmVariables(st,vm);
		return new JSONResponse(vm.execute(st).asJSON(st));
	}
}
