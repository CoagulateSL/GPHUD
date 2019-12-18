package net.coagulate.GPHUD.Modules.Objects.ObjectTypes;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.TreeMap;

public abstract class ObjectType {

	final State state;
	@Nonnull
	final ObjectTypes object;
	@Nonnull
	final JSONObject json;

	protected ObjectType(final State st,
	                     @Nonnull final ObjectTypes object)
	{
		state=st;
		this.object=object;
		json=object.getBehaviour();
	}

	@Nonnull
	public static ObjectType materialise(final State st,
	                                     @Nonnull final ObjectTypes object)
	{
		final JSONObject json=object.getBehaviour();
		final String behaviour=json.optString("behaviour","");
		if (behaviour.equals("ClickTeleport")) { return new ClickTeleporter(st,object); }
		if (behaviour.equals("PhantomTeleport")) { return new PhantomTeleporter(st,object); }
		if (behaviour.equals("RunCommand")) { return new RunCommand(st,object); }
		if (behaviour.equals("NPC")) { return new NPC(st,object); }
		throw new SystemImplementationException("Behaviour "+behaviour+" is not known!");
	}

	@Nonnull
	public static Map<String,String> getObjectTypes(final State st) {
		final Map<String,String> options=new TreeMap<>();
		options.put("ClickTeleport","Teleport user on click.");
		options.put("PhantomTeleport","Teleport user on collision; becomes phantom.");
		options.put("RunCommand","Causes the character to run a command when they click.");
		options.put("NPC","Assigns a character to this object and allows it to participate in scripted events");
		return options;
	}

	@Nonnull
	public static DropDownList getDropDownList(final State st) {
		final DropDownList behaviours=new DropDownList("behaviour");
		final Map<String,String> types=getObjectTypes(st);
		for (final Map.Entry<String,String> entry: types.entrySet()) {
			behaviours.add(entry.getKey(),entry.getValue());
		}
		return behaviours;
	}

	@Nonnull
	public abstract String explainHtml();

	public abstract void editForm(State st);

	public abstract void update(State st);

	@Nonnull
	public abstract String explainText();

	public void payload(final State st,
	                    @Nonnull final JSONObject response)
	{
		response.put("mode",mode());
	}

	@Nonnull
	public abstract MODE mode();

	@Nonnull
	public Response click(final State st,
	                      final Char clicker)
	{ return new ErrorResponse("Object type "+object.getName()+" does not support click behaviour"); }

	@Nonnull
	public Response collide(final State st,
	                        final Char collider)
	{ return new ErrorResponse("Object type "+object.getName()+" does not support collision behaviour"); }

	enum MODE {
		NONE,
		CLICKABLE,
		PHANTOM
	}
}
