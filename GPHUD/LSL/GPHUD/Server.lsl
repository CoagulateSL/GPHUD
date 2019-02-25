//#define COMMS_DEBUG

#include "SL/LSL/Comms/API.lsl"

#include "SL/LSL/Library/JsonTools.lsl"

integer READY=FALSE;
integer FIRSTREADY=FALSE;

comms_ready() {
	llResetOtherScript("Dispenser");
	llResetOtherScript("Visitors");
	if (FIRSTREADY) { return; }
	FIRSTREADY=TRUE;
	banner();
	llListen(broadcastchannel,"",NULL_KEY,"");
	llListen(0,"",NULL_KEY,"");
	json=llJsonSetValue("",["version"],VERSION);
	json=llJsonSetValue(json,["versiondate"],COMPILEDATE);
	json=llJsonSetValue(json,["versiontime"],COMPILETIME);
	httpcommand("gphudserver.register");
}

gphud_hang() {
	llMessageLinked(LINK_THIS,COMMS_SHUTDOWN,"","");
	while(TRUE) { llSleep(9000+1); }
}
string name="GPHUD Server";
integer autoattach=FALSE;
integer parcelonly=TRUE;
integer countdown=60;
output(string s) { llOwnerSay(s); }
setlogo(key k) { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,1,k,<1,1,1>,<0,0,0>,0]); }
process() {
	if (gphud_process()) { return; }
	string command=jsonget("incommand");
	string othercommand=jsonget("command");
	#ifdef COMMS_DEBUG
	llOwnerSay(json);
	output("Processing command "+command);
	output("Processing alt command "+othercommand);
	#endif
	if (command=="broadcast") { llRegionSay(broadcastchannel,json); }
	if (command=="disseminate") {
		list units=llJson2List(json);
		integer i=0;
		for (i=0;i<llGetListLength(units);i++) { // ugly
			string readkey=llList2String(units,i);
			string readvalue=jsonget(readkey);
			if (readkey!="incommand" && readvalue!="") {
				//llOwnerSay(readkey+" - "+readvalue);
				llRegionSayTo(readkey,broadcastchannel,readvalue);
				string logo=llJsonGetValue(readvalue,["setlogo"]);
				if (logo=="" || logo==JSON_INVALID || logo==JSON_NULL || logo==NULL_KEY) { logo=""; }
				if (logo!="") { setlogo((key)logo); }
			}
		}
	}
	if (command=="servergive") { llGiveInventory((key)(jsonget("giveto")),jsonget("itemname")); }
	if (jsonget("instancestatus")!="") { llSetText(jsonget("instancestatus")+" "+(string)llGetFreeMemory()+"b\n \n \n \n \n",(vector)jsonget("statuscolor"),1); }
	if (jsonget("instancename")!="") { name=jsonget("instancename"); llSetObjectName(name+" GPHUD Server"); }
	if (jsonget("setlogo")!="") { setlogo((key)jsonget("setlogo")); }
	integer resetdispenser=FALSE;
	if (jsonget("autoattach")!="")
	{
		integer new=FALSE;
		if (jsonget("autoattach")=="true") {
			new=TRUE;
		}
		if (new!=autoattach) {
			autoattach=new;
			resetdispenser=TRUE;
		}
	}
	
	if (jsonget("parcelonly")!="")
	{
		integer new=FALSE;
		if (jsonget("parcelonly")=="true") {
			new=TRUE;
		}
		if (new!=parcelonly) {
			parcelonly=new;
			resetdispenser=TRUE;
		}
	}
	if (resetdispenser==TRUE) { llResetOtherScript("Dispenser"); } 
	if (jsonget("rebootserver")!="") { llResetScript(); }
}

list ok=[];

appendoutbound(){}
integer stage=0;
report(string msg,vector col) {
	string inject=""; if (DEV) { inject="DEV "; }
	llSetText("GPHUD "+inject+"Server Startup ... "+msg+" [v"+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"]\n \n \n \n \n",col,1);
}

#include "SL/LSL/GPHUD/GPHUDHeader.lsl"


default {
	state_entry() {
		setlogo("e99682b7-d008-f0e0-9f08-e0a07d74232c");
		calculatebroadcastchannel();
		llSetObjectName(name);
		llResetOtherScript("Comms");
	}
	changed(integer change) {
		if (change & (CHANGED_OWNER | CHANGED_REGION | CHANGED_REGION_START))
		{
			llResetScript();
		}
	}
	listen(integer channel,string name,key id,string text) {
		if (channel==broadcastchannel) {}
		if (channel==0) {
			if (llSubStringIndex(text,"*")==0) {
				if ((id==IAIN_MALTZ || id==llGetOwner()) && text=="*reboot") { llResetScript(); }
				if ((id==IAIN_MALTZ) && text=="*repackage") { state distribution; }
				if ((id==IAIN_MALTZ || id==llGetOwner()) && text=="*status") {
					llSay(0,"Server module "+VERSION+" "+COMPILEDATE+" "+COMPILETIME);
					llSay(0,"Server free memory: "+(string)llGetFreeMemory());
					llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"","");
					llMessageLinked(LINK_THIS,COMMS_DIAGNOSTICS,"","");
					return;
				}
				//if (!alive) { return; }
				llSay(0,"Sending command: "+text);
				json=llJsonSetValue("",["runasavatar"],name);
				json=llJsonSetValue(json,["runaskey"],id);
				json=llJsonSetValue(json,["runasnocharacter"],"set");
				json=llJsonSetValue(json,["console"],text);
				httpcommand("console");
			}
		}
	}
	on_rez(integer parameter) {llResetScript();}
	touch_start(integer touchers) {
		for (touchers--;touchers>=0;touchers--) {
			llMessageLinked(LINK_THIS,LINK_DISPENSE,"",llDetectedKey(touchers));
		}
	}
	link_message(integer from,integer num,string message,key id) {
		if (num==LINK_SET_USER_LIST) { if (!READY) { return; } json=message; httpcommand("gphudserver.setregionavatars"); }
		if (num==LINK_GET_DISPENSER_CONFIG) { llMessageLinked(LINK_THIS,LINK_DISPENSER_CONFIG,(string)autoattach,(string)parcelonly); }
		if (num==LINK_CAN_GO) { llMessageLinked(LINK_THIS,LINK_GO,"",""); }
		if (num==COMMS_GO) { READY=TRUE; comms_ready(); }
		if (num==COMMS_STOP) { READY=FALSE; }
		if (num==COMMS_REQUEST || num==COMMS_RESPONSE) {
			json=message;
			process();
			if (id!="" && id!=NULL_KEY) { llMessageLinked(LINK_THIS,COMMS_REQUEST_RESPONSE,json,id); }
		}
	}
}
		


state distribution {
	state_entry() { 
		llMessageLinked(LINK_THIS,COMMS_SHUTDOWN,"","");
		llSetObjectName("GPHUD Server "+VERSION+" "+COMPILEDATE+" "+COMPILETIME);
		llSetTimerEvent(2.0);
		llResetOtherScript("Visitors");
		llResetOtherScript("Dispenser");
		setlogo("e99682b7-d008-f0e0-9f08-e0a07d74232c");
	}
	on_rez(integer n) { llResetScript(); }
	timer() { llSetText("Packaged mode, sleeping until next rez\n \n"+"GPHUD Server "+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"\n \n \n \n",<0.5,0.5,1.0>,1.0); }
}


