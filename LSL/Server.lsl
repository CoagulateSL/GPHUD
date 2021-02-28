//#define COMMS_DEBUG
//#define DEBUG
#define COMMS_PROTOCOL "3"
//#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
//#define COMMS_INCLUDEDIGEST
#define COMMS_DONT_CHECK_CALLBACK
#include "configuration.lsl"
#include "GPHUD/LSL/Constants.lsl"
#include "SLCore/LSL/JsonTools.lsl"
#include "SLCore/LSL/SetDev.lsl"
#include "GPHUDHeader.lsl"
#include "SLCore/LSL/ServerMesh.lsl"
#include "SLCore/LSL/CommsV3.lsl"



gphud_hang(string reason) {
	if (comms_url!="") { llReleaseURL(comms_url); comms_url=""; }
	llResetOtherScript("Dispenser");
	llSetText("Service halted\n"+reason,<1,.5,.5>,1);
	while(TRUE) { llSleep(9000+1); }
}
string name="GPHUD Region Server";
integer autoattach=FALSE;
integer parcelonly=TRUE;
integer countdown=60;
output(string s) { llOwnerSay(s); }

integer process(key id) {

	string command=jsonget("incommand");
	string othercommand=jsonget("command");
	//llOwnerSay(command+" // "+othercommand);
	//llOwnerSay(command+" // "+othercommand+" // "+json);
	#ifdef COMMS_DEBUG
	llOwnerSay(json);
	output("Processing command "+command);
	output("Processing alt command "+othercommand);
	#endif
	if (command=="registered") { llOwnerSay("Startup Complete!"); BOOTSTAGE=BOOT_COMPLETE; llMessageLinked(LINK_THIS,LINK_SET_STAGE,(string)BOOTSTAGE,NULL_KEY); setup(); }
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
	if (command=="messageto") { llRegionSayTo((key)jsonget("target"),0,jsonget("sendthismessage")); }
	if (command=="servergive") { llGiveInventory((key)(jsonget("giveto")),jsonget("itemname")); }
	if (jsonget("instancestatus")!="") { llSetText(jsonget("instancestatus")+" V"+VERSION+"\n \n \n \n \n",(vector)jsonget("statuscolor"),1); }
	if (jsonget("instancename")!="") { name=jsonget("instancename"); llSetObjectName(name+" GPHUD Region Server"); }
	if (jsonget("setlogo")!="") { setlogo((key)jsonget("setlogo")); }
	if (jsonget("rebootserver")!="") { llResetScript(); }
	if (jsonget("instantmessage")!="") {
		llMessageLinked(LINK_THIS,LINK_INSTANT_MESSAGE_SEND,jsonget("instantmessagemessage"),jsonget("instantmessage"));
	}
	if (jsonget("terminate")!="") {
		gphud_hang("=== TERMINATED ===: "+jsonget("terminate"));
	}			
	if (command=="shutdown" || jsonget("shutdown")!="") {
		gphud_hang("Shutdown requested: "+jsonget("shutdown"));
	}		
	integer output=1;
	while (jsonget("output"+(string)output)!="") {
		llSay(0,llGetSubString(jsonget("output"+(string)output),1,-1));
		output++;
	}
	if (jsonget("error")!="") { 
		llSay(0,"ERROR: "+jsonget("error"));
	}
	json="{}"; // clear the response :P
	return TRUE;
}

list ok=[];

appendoutbound(){}
integer stage=0;
report(string msg,vector col) {
	string inject=""; if (DEV) { inject="DEV "; }
	llSetText("GPHUD "+inject+"Server Startup ... "+msg+" [v"+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"]\n \n \n \n \n",col,1);
}


setup() {
	setDev(FALSE);
	comms_setup();
	if (BOOTSTAGE==BOOT_COMMS) { 
		key k=SLCORE_COAGULATE_LOGO;
		setDev(TRUE);
		if (DEV) { k=SLCORE_COAGULATE_DEV_LOGO; }
		setlogo(k); setshard(k);
		calculatebroadcastchannel();
		llSetObjectName(name);
		llResetOtherScript("Dispenser");
		llResetOtherScript("SimScanner");
		return;
	}
	if (BOOTSTAGE==BOOT_APP) {
		banner_server();
		llListen(broadcastchannel,"",NULL_KEY,"");
		llListen(0,"",NULL_KEY,"");
		json=llJsonSetValue("",["version"],VERSION);
		json=llJsonSetValue(json,["versiondate"],COMPILEDATE);
		json=llJsonSetValue(json,["versiontime"],COMPILETIME);
		httpcommand("gphudserver.register","GPHUD/system");
		return;
	}
}



default {
	state_entry() {
		setup();
	}
	changed(integer change) {
		if (change & (CHANGED_OWNER | CHANGED_REGION | CHANGED_REGION_START))
		{
			llResetScript();
		}
	}
	listen(integer channel,string name,key id,string text) {
		if (channel==broadcastchannel) {
			json=text;
			if (jsonget("dispense")!="") {
				llMessageLinked(LINK_THIS,LINK_DISPENSE,"",(key)jsonget("dispense"));
			}
			if (jsonget("dispensetitler")!="") {
				llMessageLinked(LINK_THIS,LINK_DISPENSE_TITLER,jsonget("attachmentpoint"),(key)jsonget("dispensetitler"));
			}			
		}
		if (channel==0) {
			string commandprefix="**";
			if (DEV) { commandprefix="--"; }
			if (llSubStringIndex(text,commandprefix)==0) {
				if (llList2Float(llGetObjectDetails(id,[OBJECT_BODY_SHAPE_TYPE]),0)>=0.0) {
					if ((id==SYSTEM_OWNER_UUID || id==llGetOwner()) && text==commandprefix+"reboot") { llResetScript(); }
					if ((id==SYSTEM_OWNER_UUID) && text==commandprefix+"repackage") { state distribution; }
					if ((id==SYSTEM_OWNER_UUID || id==llGetOwner()) && text==commandprefix+"status") {
						llSay(0,"Server module "+VERSION+" "+COMPILEDATE+" "+COMPILETIME);
						llSay(0,"Server free memory: "+(string)llGetFreeMemory());
						llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"","");
						return;
					}
					//if (!alive) { return; }
					llSay(0,"Sending command: "+llGetSubString(text,2,-1));
					json=llJsonSetValue("",["runasavatar"],name);
					json=llJsonSetValue(json,["runaskey"],id);
					json=llJsonSetValue(json,["runasnocharacter"],"set");
					json=llJsonSetValue(json,["console"],llGetSubString(text,2,-1));
					httpcommand("console","GPHUD/system");
				}
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
		if (num==LINK_GET_DISPENSER_CONFIG) { llMessageLinked(LINK_THIS,LINK_DISPENSER_CONFIG,(string)autoattach,(string)parcelonly); }
	}
	http_request(key id,string method,string body) {
		json=body; body="";
		#ifdef DEBUG
		llOwnerSay("IN:"+body);
		#endif
		
		if (comms_http_request(id,method)) { llHTTPResponse(id,200,json); return; }
		
		//llOwnerSay("HTTPIN:"+json);
		
		if (process(id))
		{ llHTTPResponse(id,200,json); }
	}	
	http_response( key request_id, integer status, list metadata, string body ) {
		#ifdef DEBUG
		llOwnerSay("REPLY:"+body);
		#endif
		if (status!=200) {
			comms_error((string)status);
			//comms_do_callback();
		}
		else
		{
			json=body;
			
			if (comms_http_response(request_id,status)) { return; }
			
			process(NULL_KEY);
		}
	}	
}
		


state distribution {
	state_entry() { 
		llResetOtherScript("Dispenser");
		llSetObjectName("GPHUD Region Server "+VERSION+" "+COMPILEDATE);
		llSetTimerEvent(2.0);
		setlogo(SLCORE_COAGULATE_LOGO);
	}
	on_rez(integer n) { llResetScript(); }
	timer() { llSetText("Packaged mode, sleeping until next rez\n \n"+"GPHUD Region Server "+VERSION+" "+COMPILEDATE+"\n \n \n \n",<0.5,0.5,1.0>,1.0); }
}


