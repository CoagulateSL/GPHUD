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
#define HUD_INVENTORY_14 36
#define HUD_QUICKBUTTON_3 37

integer SHUTDOWN=FALSE;

uixMain() {
	string text=""; if (inventoryshown==0) { text=hudtext; }
	llSetLinkPrimitiveParamsFast(LINK_THIS,
		[
			PRIM_LINK_TARGET, HUD_MAIN,
				PRIM_TEXTURE, ALL_SIDES, logo, <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, hudpos,
				PRIM_SIZE, hudsize,
				PRIM_TEXT,text,hudtextcolor,1,
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_1,
				PRIM_TEXTURE, ALL_SIDES, qb1texture, <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, qb1pos, 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_2,
				PRIM_TEXTURE, ALL_SIDES, qb2texture, <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, qb2pos, 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_3,
				PRIM_TEXTURE, ALL_SIDES, qb3texture, <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, qb3pos, 
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_4,
				PRIM_TEXTURE, ALL_SIDES, qb4texture, <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, qb4pos,
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_5,
				PRIM_TEXTURE, ALL_SIDES, qb5texture, <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, qb5pos,
			PRIM_LINK_TARGET, HUD_QUICKBUTTON_6,
				PRIM_TEXTURE, ALL_SIDES, qb6texture, <1,1,1>, <0,0,0>, 0,
				PRIM_POS_LOCAL, qb6pos,
			PRIM_LINK_TARGET, HUD_GET_MESSAGE,
				PRIM_POS_LOCAL, msgpos
				
		]
	);
}

uixInventory() {
	string text=""; if (inventoryshown==0) { text=hudtext; }
	list l=[
		PRIM_LINK_TARGET, HUD_MAIN,
			PRIM_TEXT,text,hudtextcolor,1];
	integer i=0;
	for (i=1;i<=14;i++) {
		if (i<=(inventoryshown+2)) {
			l+=[PRIM_LINK_TARGET, llList2Integer(inventoryprims,i-1),
			//PRIM_POS_LOCAL, <0,0,inventorybasez + inventoryoffsetz*(((float)(i-1)))>,
			PRIM_POS_LOCAL, <0,inventorywidth/2.0 * ( ( ( i % 2) * 2.0 ) - 1.0)  ,inventorybasez + inventoryoffsetz*(((float)((i-1)/2)))>,
			PRIM_SIZE,<0,inventorywidth,inventoryoffsetz>,
			PRIM_TEXT,llList2String(inventorytext,i-1),<0,0,0>,1];
		} else {
			l+=[PRIM_LINK_TARGET, llList2Integer(inventoryprims,i-1),
			PRIM_POS_LOCAL, <0,0,-0.2>,
			PRIM_SIZE,<0,-0.3,-0.3>,
			PRIM_TEXT,"",<0,0,0>,1];	
		}
	}
	llSetLinkPrimitiveParamsFast(LINK_THIS,l);
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

*/
key logo="c792716b-13a3-06c9-6e7c-33c4e9d5a48f";
key qb1texture="4250c8ec-6dba-927b-f68f-000a456bd8ba";
key qb2texture="eab5cd3c-ac2e-290b-df46-a53c9114f610";
key qb3texture="d41ccbd1-1144-3788-14cc-5fc26f3da905";
key qb4texture="5748decc-f629-461c-9a36-a35a221fe21f";
key qb5texture="ffdaa452-d5cd-0203-de84-4f814732cff0";
key qb6texture="b2aedfae-8401-441e-d9d1-b5b330bce411";
vector qb1pos=<.01,0,-.2>;
vector qb2pos=<.01,0,-.2>;
vector qb5pos=<.01,0,-.2>;
vector qb3pos=<.01,0,-.2>;
vector qb4pos=<.01,0,-.2>;
vector qb6pos=<.01,0,-.2>;
vector msgpos=<.01,0,-.2>;
float sizeratio=2.0;
vector hudsize=<0.01,0.24,0.12>;
vector hudpos=<0,0, 0.08518>;
float inventorybasez=0.04;
float inventoryoffsetz=0.04;
float inventorywidth=0.42;
integer SUPPRESS_HUD_TEXT=FALSE;
string hudtext="";
vector hudtextcolor=<0.5,0.5,1.0>;
list inventoryprims=[HUD_INVENTORY_1, HUD_INVENTORY_2, HUD_INVENTORY_3, HUD_INVENTORY_4, HUD_INVENTORY_5, HUD_INVENTORY_6, HUD_INVENTORY_7, HUD_INVENTORY_8, HUD_INVENTORY_9, HUD_INVENTORY_10, HUD_INVENTORY_11, HUD_INVENTORY_12, HUD_INVENTORY_13, HUD_INVENTORY_14 ];
integer inventoryshown=0;
list inventorytext=[];
process() {
	//llOwnerSay(json);
	integer main=FALSE;
	if (jsonget("qb1texture")!="") { qb1texture=jsonget("qb1texture"); main=TRUE; }
	if (jsonget("qb2texture")!="") { qb2texture=jsonget("qb2texture"); main=TRUE; }
	if (jsonget("qb3texture")!="") { qb3texture=jsonget("qb3texture"); main=TRUE; }
	if (jsonget("qb4texture")!="") { qb4texture=jsonget("qb4texture"); main=TRUE; }
	if (jsonget("qb5texture")!="") { qb5texture=jsonget("qb5texture"); main=TRUE; }
	if (jsonget("qb6texture")!="") { qb6texture=jsonget("qb6texture"); main=TRUE; }
	if (jsonget("setlogo")!="") { logo=jsonget("setlogo"); main=TRUE; }
	if (jsonget("sizeratio")!="") { main=TRUE; sizeratio=((float)jsonget("sizeratio"));
		qb1pos=<.01,(0.06*sizeratio/*hud*/ + 0.04*1.5/*button*/),.04>;
		qb2pos=<.01,(0.06*sizeratio/*hud*/ + 0.04*0.5/*button*/),.04>;
		qb3pos=<.01,(0.06*sizeratio/*hud*/ + 0.04*1.5/*button*/),.00>;
		qb4pos=<.01,(0.06*sizeratio/*hud*/ + 0.04*0.5/*button*/),.00>;
		qb5pos=<.01,(0.06*sizeratio/*hud*/ + 0.04*1.5/*button*/),-.04>;
		qb6pos=<.01,(0.06*sizeratio/*hud*/ + 0.04*0.5/*button*/),-.04>;
		msgpos=<.01,(-0.06*sizeratio - 0.04*0.5),.04>;
		hudsize=<0.01,(0.12*sizeratio),0.12>;
	}
	if (jsonget("line1")!="") {
		integer i=1;
		inventorytext=[];
		inventoryshown=0;
		integer chars=0;
		for (i=1;i<=12;i++) {
			string line=jsonget("line"+(string)i);
			if (line!="") { 
				inventorytext+=line;
				inventoryshown++;
				integer length=llStringLength(line); if (length>chars) { chars=length; }
			}
		}
		inventorywidth=0.5/64.0*((float)chars);
		uixInventory();
	}
	if (main) { uixMain(); }
	
}

default {
	state_entry() {
		//llSetTimerEvent(2);
		hudtext="Coagulate GPHUD v"+VERSION+"\n"+COMPILEDATE+" "+COMPILETIME;
		setDev(FALSE);
		uixMain();
		uixInventory();
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
	touch_start(integer n) {
		if (llListFindList(inventoryprims,[llDetectedLinkNumber(0)])!=-1) { inventoryshown=0; uixInventory(); }
	}
}
