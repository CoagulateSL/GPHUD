//#define DEBUG
#include "SLCore/LSL/SetDev.lsl"
#define MESSAGE_IS_SAY TRUE
#include "GPHUD/LSL/GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"

//#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
//#define COMMS_INCLUDEDIGEST
#define COMMS_NOCHARACTER
#define COMMS_NOSETTEXT
#include "configuration.lsl"
#define COMMS_INTERFACE "object"
#define COMMS_DONT_CHECK_CALLBACK
#include "SLCore/LSL/CommsV3.lsl"

#define CHECKIN_MINUTES 15
string MODE="NONE";

integer ODVERSION=1;

startLogin() {
	json=llJsonSetValue("",["version"],VERSION);
	json=llJsonSetValue(json,["versiondate"],COMPILEDATE);
	json=llJsonSetValue(json,["versiontime"],COMPILETIME);
	json=llJsonSetValue(json,["bootparams"],llGetObjectDesc());
	if (BOOTSTAGE==BOOT_COMPLETE) { json=llJsonSetValue(json,["silent"],"silent"); }
	httpcommand("objects.connect","GPHUD/system");
}
string titlertext="";
vector titlercolor=<1,1,1>;
integer process(key id) {
	gphud_process();
	string incommand=jsonget("incommand");
	integer DONOTRESPOND=FALSE;
	string retjson="";
	//llOwnerSay("WE ARE HERE WITH "+json);
	if (jsonget("error")!="" && BOOTSTAGE==BOOT_APP) { // failed to login / create character?  so blind retry? :P
		llOwnerSay("Error during login/registration, please reset scripts to retry...");
		gphud_hang("Failed to register with server:\n"+jsonget("error"));
		return TRUE;	
	}
	if (incommand=="registered") { /*cookie=jsonget("cookie");*/ BOOTSTAGE=BOOT_COMPLETE; llMessageLinked(LINK_THIS,LINK_SET_STAGE,(string)BOOTSTAGE,NULL_KEY); }
	if (incommand=="ping") { /*retjson=llJsonSetValue(retjson,["cookie"],cookie);*/ }
	//if (jsonget("eventmessage1")!="") { llOwnerSay(jsonget("eventmessage1")); }
	//if (jsonget("eventmessage2")!="") { llOwnerSay(jsonget("eventmessage2")); }
	if (jsonget("mode")!="") { MODE=jsonget("mode"); }
	if (jsonget("titlercolor")!="") { titlercolor=(vector)jsonget("titlercolor"); }	
	if (jsonget("titlertext")!="") { titlertext=jsonget("titlertext"); }
	if (jsonget("titlertext")!="" || jsonget("titlercolor")!="") { 
		string totitler=llJsonSetValue("",["titler"],(string)titlercolor+"|"+titlertext);
		llSetText(titlertext,titlercolor,1);
	}
	json=retjson;
	if (DONOTRESPOND) { return FALSE; }
	return TRUE;
}

gphud_hang(string reason) {
	string finalreason="===SHUTDOWN===\n"+reason+"\n"+llGetTimestamp();
	if (comms_url!="") { llReleaseURL(comms_url); comms_url=""; }
	while (TRUE) {
		llSetText(finalreason,<1,.75,.75>,1);
		llSleep(300);
	}
}

setup() {
	comms_setup();
	if (BOOTSTAGE==BOOT_COMMS) { 
		setDev(TRUE);
		return;
	}
	if (BOOTSTAGE==BOOT_APP) {
		#ifdef DEBUG
		llOwnerSay("SETUP stage 1 - bidirectional comms is a GO, GPHUD logo, banner, and commence login to GPHUD service");
		#endif
		banner_object();
		startLogin();
		return;
	}
}






all_http_request(key id,string method,string body) {
	json=body; body="";
	#ifdef DEBUG
	llOwnerSay("IN:"+json);
	#endif
	llMessageLinked(LINK_THIS,LINK_RECEIVE,json,"");
	if (comms_http_request(id,method)) { llHTTPResponse(id,200,json); return; }
	
	//llOwnerSay("HTTPIN:"+json);
	
	if (process(id)) { llHTTPResponse(id,200,json); }
}	
all_http_response( key request_id, integer status, list metadata, string body ) {
	#ifdef DEBUG
	llOwnerSay("REPLY:"+body);
	#endif
	if (status!=200) {
		comms_error((string)status);
		//comms_do_callback();
	}
	else
	{
		json=body; body="";
		
		if (comms_http_response(request_id,status)) { return; }
		
		process(NULL_KEY);
	}
}	
	
	//====================================
dolisten() {
		calculatebroadcastchannel();
		llListen(broadcastchannel,"",NULL_KEY,"");		
}
integer updatelock=0;
all_listen(integer channel,string name,key id,string text) {
	if (channel==broadcastchannel) {
		json=text;
		json=llJsonSetValue(json,["incommand"],"broadcast");
		if (jsonget("objectdriverversioncheck")!="" && llGetOwnerKey(id)==llGetOwner()) {
			integer otherversion=(integer)(jsonget("objectdriverversioncheck"));
			if (otherversion>ODVERSION && updatelock<llGetUnixTime()) {
				integer pin=0;
				updatelock=llGetUnixTime()+300;
				while (pin>-1000 && pin<1000) {
					pin=((integer)(llFrand(1999999999)-1000000000));
				}
				llSetRemoteScriptAccessPin(pin);
				string jsonrequestupdate=llJsonSetValue("",["objectdriverupdatepin"],((string)pin));
				llRegionSayTo(id,broadcastchannel,jsonrequestupdate);
			}
		}
		if(UPDATER && jsonget("objectdriverupdatepin")!="") {
			integer pin=(integer)(jsonget("objectdriverupdatepin"));
			llOwnerSay("Updating object driver in '"+name+"'");
			llRemoteLoadScriptPin(id,llGetScriptName(),pin,TRUE,0);
		}
		process(NULL_KEY);
	}
}
integer UPDATER=FALSE;
default {
	state_entry() {
		if (llGetScriptName()!="GPHUD Object Driver") { llOwnerSay("This script MUST be named 'GPHUD Object Driver'"); state dead; }
		if (llGetInventoryType("GPHUD Object Driver Inhibitor")!=INVENTORY_NONE) { state wait; }
		llSetText("",<0,0,0>,0);
		setDev(FALSE);
		setup(); dolisten();
	}
	http_request(key id,string method,string body) {
		all_http_request(id,method,body);
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="CLICKABLE") { state clickable; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="NONE") { state none; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="PHANTOM") { state phantom; }
	}
	http_response( key request_id, integer status, list metadata, string body ) {
		all_http_response( request_id, status, metadata, body );
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="PHANTOM") { state phantom; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="CLICKABLE") { state clickable; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="NONE") { state none; }
	}
	listen(integer channel,string name,key id,string text) { all_listen(channel,name,id,text); }
	
	timer() { startLogin(); }
	changed(integer change) { if ((change & CHANGED_REGION) || (change & CHANGED_REGION_START) || (change & CHANGED_OWNER)) { llResetScript(); }}	
	on_rez(integer parameter) { llResetScript(); }	
}
state dead {
	state_entry() { llSetText("Dead",<1,1,1>,1); }
}
state wait {
	state_entry() {
		dolisten();
		llSetTimerEvent(60.0);
	}
	listen(integer channel,string name,key id,string text) { all_listen(channel,name,id,text); }	
	changed(integer change) { if ((change & CHANGED_REGION) || (change & CHANGED_REGION_START) || (change & CHANGED_OWNER)) { llResetScript(); }}
	on_rez(integer parameter) { llResetScript(); }
	timer() {
		json=llJsonSetValue("",["objectdriverversioncheck"],((string)ODVERSION));
		llRegionSay(broadcastchannel,json);
		UPDATER=TRUE;
		llSetTimerEvent(0.0);
	}
}

state none {
	state_entry() { llSetTimerEvent(CHECKIN_MINUTES*60.0); 
		llSetStatus(STATUS_PHANTOM,FALSE); llVolumeDetect(FALSE); dolisten();
		#ifdef DEBUG
		llOwnerSay("Switched to NONE operational mode");
		#endif
		dolisten();
	}
	http_request(key id,string method,string body) { all_http_request(id,method,body);
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="CLICKABLE") { state clickable; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="PHANTOM") { state phantom; }
	}
	http_response( key request_id, integer status, list metadata, string body ) {all_http_response( request_id, status, metadata, body );
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="CLICKABLE") { state clickable; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="PHANTOM") { state phantom; }
	}
	listen(integer channel,string name,key id,string text) { all_listen(channel,name,id,text); }	
	timer() { startLogin(); }
	changed(integer change) { if ((change & CHANGED_REGION) || (change & CHANGED_REGION_START) || (change & CHANGED_OWNER)) { llResetScript(); }}	
	on_rez(integer parameter) { llResetScript(); }
}

state clickable {
	state_entry() { llSetTimerEvent(CHECKIN_MINUTES*60.0);
		llSetStatus(STATUS_PHANTOM,FALSE); llVolumeDetect(FALSE); dolisten();
		#ifdef DEBUG
		llOwnerSay("Switched to CLICKABLE operational mode");
		#endif
		dolisten();
	}
	http_request(key id,string method,string body) { all_http_request(id,method,body);
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="NONE") { state none; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="PHANTOM") { state phantom; }
	}
	http_response( key request_id, integer status, list metadata, string body ) {all_http_response( request_id, status, metadata, body );
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="NONE") { state none; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="PHANTOM") { state phantom; }
	}
	listen(integer channel,string name,key id,string text) { all_listen(channel,name,id,text); }	
	timer() { startLogin(); }
	changed(integer change) { if ((change & CHANGED_REGION) || (change & CHANGED_REGION_START) || (change & CHANGED_OWNER)) { llResetScript(); }}
	on_rez(integer parameter) { llResetScript(); }
	touch_start(integer n) { 
		for (n--;n>=0;n--) {
			json=llJsonSetValue("",["clicker"],">"+llDetectedName(n));
		}
		httpcommand("objects.clicked","GPHUD/system");
	}
}
state phantom {
	state_entry() { llSetTimerEvent(CHECKIN_MINUTES*60.0);
		llSetStatus(STATUS_PHANTOM,TRUE); llVolumeDetect(TRUE); dolisten();
		#ifdef DEBUG
		llOwnerSay("Switched to PHANTOM VOLUME operational mode");
		#endif
		dolisten();
	}
	http_request(key id,string method,string body) { all_http_request(id,method,body);
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="NONE") { state none; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="CLICKABLE") { state clickable; }
	}
	http_response( key request_id, integer status, list metadata, string body ) {all_http_response( request_id, status, metadata, body );
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="NONE") { state none; }
		if (BOOTSTAGE==BOOT_COMPLETE && MODE=="CLICKABLE") { state clickable; }
	}
	listen(integer channel,string name,key id,string text) { all_listen(channel,name,id,text); }	
	timer() { startLogin(); }
	changed(integer change) { if ((change & CHANGED_REGION) || (change & CHANGED_REGION_START) || (change & CHANGED_OWNER)) { llResetScript(); }}
	on_rez(integer parameter) { llResetScript(); }
	collision_start(integer n) { 
		for (n--;n>=0;n--) {
			json=llJsonSetValue("",["collider"],">"+llDetectedName(n));
		}
		httpcommand("objects.collided","GPHUD/system");
	}
}

