#include "SL/LSL/Constants.lsl"
#include "SL/LSL/GPHUD/GPHUDHeader.lsl"
#include "SL/LSL/Library/SetDev.lsl"
#include "SL/LSL/Library/JsonTools.lsl"

#define HUD_MAIN 1
#define HUD_EFFECTS_5 2
#define HUD_EFFECTS_6 3
#define HUD_EFFECTS_TIMER_6 4
#define HUD_EFFECTS_1 5
#define HUD_EFFECTS_TIMER_1 6
#define HUD_EFFECTS_TIMER_2 7
#define HUD_EFFECTS_2 8
#define HUD_EFFECTS_3 9
#define HUD_EFFECTS_TIMER_3 10
#define HUD_EFFECTS_TIMER_4 11
#define HUD_EFFECTS_4 12
#define HUD_EFFECTS_TIMER_5 13
#define HUD_INVENTORY_1 14
#define HUD_INVENTORY_2 15
#define HUD_INVENTORY_3 16
#define HUD_INVENTORY_4 17
#define HUD_INVENTORY_5 18
#define HUD_INVENTORY_6 19
#define HUD_INVENTORY_7 20
#define HUD_INVENTORY_8 21
#define HUD_INVENTORY_9 22
#define HUD_INVENTORY_10 23
#define HUD_INVENTORY_11 24
#define HUD_INVENTORY_12 25
#define HUD_INVENTORY_13 26
#define HUD_SPARE_1 27
#define HUD_GET_MESSAGE 28
#define HUD_SPARE_2 29
#define HUD_QUICKBUTTON_6 30
#define HUD_QUICKBUTTON_2 31
#define HUD_SPARE_3 32
#define HUD_QUICKBUTTON_4 33
#define HUD_QUICKBUTTON_5 34
#define HUD_QUICKBUTTON_1 35
#define HUD_SPARE_4 36
#define HUD_QUICKBUTTON_3 37

integer SHUTDOWN=FALSE;

uixMain() {
	llSetLinkPrimitiveParamsFast(LINK_THIS,
		[
			PRIM_LINK_TARGET, HUD_MAIN,
				PRIM_TEXTURE, ALL_SIDES, llJsonGetValue(json,["uix-hud"]), <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-hudpos"]),
				PRIM_SIZE, (vector)llJsonGetValue(json,["uix-hudsize"]),
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_1,
				PRIM_TEXTURE, ALL_SIDES, llJsonGetValue(json,["uix-qb1"]), <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-qb1pos"]), 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_2,
				PRIM_TEXTURE, ALL_SIDES, llJsonGetValue(json,["uix-qb2"]), <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-qb2pos"]), 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_3,
				PRIM_TEXTURE, ALL_SIDES, llJsonGetValue(json,["uix-qb3"]), <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-qb3pos"]), 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_4,
				PRIM_TEXTURE, ALL_SIDES, llJsonGetValue(json,["uix-qb4"]), <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-qb4pos"]), 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_5,
				PRIM_TEXTURE, ALL_SIDES, llJsonGetValue(json,["uix-qb5"]), <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-qb5pos"]), 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_6,
				PRIM_TEXTURE, ALL_SIDES, llJsonGetValue(json,["uix-qb6"]), <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-qb6pos"]),
			PRIM_LINK_TARGET, HUD_GET_MESSAGE,
				PRIM_POS_LOCAL, (vector)llJsonGetValue(json,["uix-msgpos"])
				
		]
	);
}
/*
#define HUD_EFFECTS_5 2
#define HUD_EFFECTS_6 3
#define HUD_EFFECTS_TIMER_6 4
#define HUD_EFFECTS_1 5
#define HUD_EFFECTS_TIMER_1 6
#define HUD_EFFECTS_TIMER_2 7
#define HUD_EFFECTS_2 8
#define HUD_EFFECTS_3 9
#define HUD_EFFECTS_TIMER_3 10
#define HUD_EFFECTS_TIMER_4 11
#define HUD_EFFECTS_4 12
#define HUD_EFFECTS_TIMER_5 13
#define HUD_INVENTORY_1 14
#define HUD_INVENTORY_2 15
#define HUD_INVENTORY_3 16
#define HUD_INVENTORY_4 17
#define HUD_INVENTORY_5 18
#define HUD_INVENTORY_6 19
#define HUD_INVENTORY_7 20
#define HUD_INVENTORY_8 21
#define HUD_INVENTORY_9 22
#define HUD_INVENTORY_10 23
#define HUD_INVENTORY_11 24
#define HUD_INVENTORY_12 25
#define HUD_INVENTORY_13 26
#define HUD_SPARE_1 27
#define HUD_SPARE_2 29
#define HUD_SPARE_3 32
#define HUD_SPARE_4 36
*/
process() {
	string uix=llJsonGetValue(json,["uix"]);
	if (uix=="" || uix==JSON_NULL || uix==JSON_INVALID) { return; }
	//llOwnerSay(json);
	if (llJsonGetValue(json,["uix-main"])=="uix-main") {
		uixMain();
	}
}

default {
	state_entry() {
		//llSetTimerEvent(2);
		setDev(FALSE);
	}
	timer() {
		if (SHUTDOWN) { llSetTimerEvent(0); return; }
	}
    link_message(integer from,integer num,string message,key id) {
		if (SHUTDOWN) { return; }	
		if (num==LINK_SHUTDOWN) { SHUTDOWN=TRUE; }
		if (num==LINK_RECEIVE) {
			json=message; message=""; process(); json="";
		}
		if (num==LINK_DIAGNOSTICS) { llOwnerSay("UIX: "+(string)llGetFreeMemory()); }
	}
	http_response( key request_id, integer status, list metadata, string body ) {
		if (SHUTDOWN) { return; }	
		if (status==200) {
			json=body; body="";
			process();
			json="";
		}
	}	
}
