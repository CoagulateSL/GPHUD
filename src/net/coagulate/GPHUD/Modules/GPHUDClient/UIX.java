package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Data.Menu;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class UIX {

	// ---------- STATICS ----------
	@Commands(description="Test only", permitUserWeb=false, permitScripting=false, permitObject=false, permitExternal=false, context=Context.ANY) @Nonnull
	public static Response uixtest(@Nonnull final State state) {
		JSONObject json=new JSONObject();

		Menu menu=Menu.getMenu(state,"Main");
		JSONObject m=menu.getJSON();
		int elements=0;
		for (int i=1;i<=12;i++) {
			if (m.has("button"+i)) {
				elements=i;
				json.put("line"+i,m.getString("button"+i));
				json.put("command"+i,m.getString("command"+i));
			}
		}

		return new JSONResponse(json);
	}

	@Commands(description="Test only", permitUserWeb=false, permitScripting=false, permitObject=false, permitExternal=false, context=Context.ANY) @Nonnull
	public static Response uixtest2(@Nonnull final State state) {
		JSONObject json=new JSONObject();

		Menu menu=Menu.getMenu(state,"Main");
		JSONObject m=menu.getJSON();
		int elements=0;
		json.put("line1","Potion of Potato Peeling +2");
		json.put("line2","Lesser Potato Peeler");
		json.put("line3","A Potato Peeling");
		json.put("line4","A Peeled Potato of Insight");
		json.put("line5","A Long Potato Peel Strip");
		json.put("line6","A Piece Of Potato");
		json.put("line7","Mr. Potato Head");
		json.put("line8","Potato Eye");
		json.put("line9","Potato Potion of Starchiness +1");
		json.put("line10","Royal Potato");
		json.put("line11","Potato Ransom Note");
		json.put("line12","Potato of Potato Potatoness");

		return new JSONResponse(json);
	}

	@Nonnull
	@Commands(description="Test only",
	          permitUserWeb=false,
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=false,
	          context=Context.ANY)
	public static Response call(
			@Nonnull
			final State st,
			@Arguments(description="Selected option",
			           max=128,
			           type=ArgumentType.TEXT_ONELINE)
			@Nonnull
			final String commandtoinvoke
	                           ) {
		if (commandtoinvoke.contains(" ")) { return Modules.run(st,commandtoinvoke,false); }
		if (Modules.getCommand(st,commandtoinvoke).getArguments().size()==0) {
			//argh, it's argless cap'n
			return Modules.run(st,commandtoinvoke,false);
		}
		return Modules.getJSONTemplateResponse(st,commandtoinvoke);
	}
}
