package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

public abstract class ObjectType {

	final State state;
	final ObjectTypes object;
	final JSONObject json;

	protected ObjectType(State st, ObjectTypes object) {
		this.state=st;
		this.object=object;
		this.json=object.getBehaviour();
	}

	public static ObjectType materialise(State st, ObjectTypes object) {
		JSONObject json=object.getBehaviour();
		String behaviour=json.optString("behaviour", "");
		if (behaviour.equals("ClickTeleport")) { return new ClickTeleporter(st,object); }
		if (behaviour.equals("PhantomTeleport")) { return new PhantomTeleporter(st,object); }
		if (behaviour.equals("RunCommand")) { return new RunCommand(st,object); }
		throw new SystemException("Behaviour "+behaviour+" is not known!");
	}

	public static Map<String,String> getObjectTypes(State st) {
		Map<String,String> options=new TreeMap<>();
		options.put("ClickTeleport","Teleport user on click.");
		options.put("PhantomTeleport","Teleport user on collision; becomes phantom.");
		options.put("RunCommand","Causes the character to run a command when they click.");
		return options;
	}

	public static DropDownList getDropDownList(State st) {
		DropDownList behaviours = new DropDownList("behaviour");
		Map<String, String> types = getObjectTypes(st);
		for (String k:types.keySet()) {
			behaviours.add(k,types.get(k));
		}
		return behaviours;
	}

	public abstract String explainHtml();

	public abstract void editForm(State st);

	public abstract void update(State st);

	public abstract String explainText();

	public void payload(State st, JSONObject response) {
		response.put("mode",mode());
	}

	public abstract MODE mode();

	public Response click(State st, Char clicker) { return new ErrorResponse("Object type "+object.getName()+" does not support click behaviour"); }
	public Response collide(State st, Char collider)  { return new ErrorResponse("Object type "+object.getName()+" does not support collision behaviour"); }

	enum MODE {NONE,CLICKABLE,PHANTOM};
}
