package net.coagulate.GPHUD.Modules.UIX;

import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class Test {

// ---------- STATICS ----------
	@Commands(description="Test only",
	          permitUserWeb=false,
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=false,
	          context=Context.ANY)
	@Nonnull
	public static Response test(
			@Nonnull
			final State state) {
		JSONObject json=new JSONObject();

		json.put("uix","uix");
		json.put("uix-main","uix-main");
		json.put("uix-qb1","4250c8ec-6dba-927b-f68f-000a456bd8ba");
		json.put("uix-qb2","eab5cd3c-ac2e-290b-df46-a53c9114f610");
		json.put("uix-qb3","d41ccbd1-1144-3788-14cc-5fc26f3da905");
		json.put("uix-qb4","5748decc-f629-461c-9a36-a35a221fe21f");
		json.put("uix-qb5","ffdaa452-d5cd-0203-de84-4f814732cff0");
		json.put("uix-qb6","b2aedfae-8401-441e-d9d1-b5b330bce411");
		json.put("uix-qb1pos","<.01,.18,.04>");
		json.put("uix-qb2pos","<.01,.14,.04>");
		json.put("uix-qb3pos","<.01,.18,.00>");
		json.put("uix-qb4pos","<.01,.14,.00>");
		json.put("uix-qb5pos","<.01,.18,-.04>");
		json.put("uix-qb6pos","<.01,.14,-.04>");
		return new JSONResponse(json);
	}
}
