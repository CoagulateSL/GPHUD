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

		float sr=state.getKV("GPHUDClient.WidthMultiplier").floatValue();
		json.put("uix","uix");
		json.put("uix-main","uix-main");
		json.put("uix-hudsize","<0.01,"+(0.12*sr)+",0.12>");
		json.put("uix-hud",state.getKV("GPHUDClient.Logo"));
		json.put("uix-qb1",state.getKV("GPHUDClient.QuickButton1Texture"));
		json.put("uix-qb2",state.getKV("GPHUDClient.QuickButton2Texture"));
		json.put("uix-qb3",state.getKV("GPHUDClient.QuickButton3Texture"));
		json.put("uix-qb4",state.getKV("GPHUDClient.QuickButton4Texture"));
		json.put("uix-qb5",state.getKV("GPHUDClient.QuickButton5Texture"));
		json.put("uix-qb6",state.getKV("GPHUDClient.QuickButton6Texture"));
		json.put("uix-hudpos","<0,0, 0.08518>");
		json.put("uix-msgpos","<.01,"+(-0.06*sr - 0.04*0.5)+",.04>");
		json.put("uix-qb1pos","<.01,"+(0.06*sr/*hud*/ + 0.04*1.5/*button*/)+",.04>");
		json.put("uix-qb2pos","<.01,"+(0.06*sr/*hud*/ + 0.04*0.5/*button*/)+",.04>");
		json.put("uix-qb3pos","<.01,"+(0.06*sr/*hud*/ + 0.04*1.5/*button*/)+",.00>");
		json.put("uix-qb4pos","<.01,"+(0.06*sr/*hud*/ + 0.04*0.5/*button*/)+",.00>");
		json.put("uix-qb5pos","<.01,"+(0.06*sr/*hud*/ + 0.04*1.5/*button*/)+",-.04>");
		json.put("uix-qb6pos","<.01,"+(0.06*sr/*hud*/ + 0.04*0.5/*button*/)+",-.04>");
		return new JSONResponse(json);
	}
	@Commands(description="Stage the HUD in a packaged mode",
	          permitUserWeb=false,
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=false,
	          context=Context.ANY,requiresPermission="User.SuperAdmin")
	@Nonnull
	public static Response repackage(
			@Nonnull
			final State state) {
		JSONObject json=new JSONObject();

		json.put("uix","uix");
		json.put("uix-main","uix-main");
		json.put("uix-hudsize","<0.01,0.24,0.12>");
		json.put("uix-hud","c792716b-13a3-06c9-6e7c-33c4e9d5a48f");
		json.put("uix-qb1","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb2","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb3","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb4","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb5","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb6","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-hudpos","<0,0, 0.08518>");
		json.put("uix-msgpos","<.01,0,-.1>");
		json.put("uix-qb1pos","<.01,0,-.1>");
		json.put("uix-qb2pos","<.01,0,-.1>");
		json.put("uix-qb5pos","<.01,0,-.1>");
		json.put("uix-qb3pos","<.01,0,-.1>");
		json.put("uix-qb4pos","<.01,0,-.1>");
		json.put("uix-qb6pos","<.01,0,-.1>");
		return new JSONResponse(json);
	}
	@Commands(description="Stage the HUD in a packaged mode",
	          permitUserWeb=false,
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=false,
	          context=Context.ANY,requiresPermission="User.SuperAdmin")
	@Nonnull
	public static Response repackage(
			@Nonnull
			final State state) {
		JSONObject json=new JSONObject();

		json.put("uix","uix");
		json.put("uix-main","uix-main");
		json.put("uix-qb1","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb2","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb3","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb4","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb5","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb6","8dcd4a48-2d37-4909-9f78-f7a9eb4ef903");
		json.put("uix-qb1pos","<.01,0,-.1>");
		json.put("uix-qb2pos","<.01,0,-.1>");
		json.put("uix-qb5pos","<.01,0,-.1>");
		json.put("uix-qb3pos","<.01,0,-.1>");
		json.put("uix-qb4pos","<.01,0,-.1>");
		json.put("uix-qb6pos","<.01,0,-.1>");
		return new JSONResponse(json);
	}
}
