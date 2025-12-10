#define COMMS_PROTOCOL "5"
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
#define COMMS_SUPPRESS_LINK_MESSAGES
#include "SLCore/LSL/CommsV3.lsl"

#define CHECKIN_MINUTES 15
string MODE="NONE";

integer ODVERSION=11;

float maxTouchDistance=0.0;

startLogin() {
	if (llGetInventoryType("GPHUD Object Driver Inhibitor")!=INVENTORY_NONE) { return; }
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
	string incommand=jsonget("incommand");
	integer DONOTRESPOND=FALSE;
	string retjson="";
	//llOwnerSay("WE ARE HERE WITH "+json);
	if (jsonget("error")!="" && BOOTSTAGE==BOOT_APP) { // failed to login / create character?  so blind retry? :P
		llOwnerSay("Error during login/registration, please reset scripts to retry...");
		gphud_hang("Failed to register with server:\n"+jsonget("error"));
		return TRUE;	
	}
	if (incommand=="registered") { /*cookie=jsonget("cookie");*/ BOOTSTAGE=BOOT_COMPLETE; }
	if (incommand=="ping") { /*retjson=llJsonSetValue(retjson,["cookie"],cookie);*/ }
	if (jsonget("maxdistance")!="") { maxTouchDistance=(float)jsonget("maxdistance"); }
	if (jsonget("mode")!="") { MODE=jsonget("mode"); }
	if (jsonget("titlercolor")!="") { titlercolor=(vector)jsonget("titlercolor"); }	
	if (jsonget("titlertext")!="") { titlertext=jsonget("titlertext"); }
	if (jsonget("titlertext")!="" || jsonget("titlercolor")!="") { 
		string totitler=llJsonSetValue("",["titler"],(string)titlercolor+"|"+titlertext);
		llSetText(titlertext,titlercolor,1);
	}
	if (jsonget("linkmessagenumber")!="") {
		llMessageLinked(LINK_SET,((integer)jsonget("linkmessagenumber")),jsonget("linkmessage"),((key)jsonget("linkid")));
	}
	integer i=1;
	while (jsonget("output"+((string)i))!="") {
		string line=jsonget("output"+((string)i));
		i++;
		string type=llGetSubString(line,0,0);
		line=llGetSubString(line,1,-1);
		//if (type=="o") { llOwnerSay(line); } // not in objects :)
		if (type=="s") { llSay(0,line); }
		if (type=="a") {
			integer index=llSubStringIndex(line,"|");
			string oldname=llGetObjectName();
			llSetObjectName(llGetSubString(line,0,index-1));
			llSay(0,llGetSubString(line,index+1,-1));
			llSetObjectName(oldname);
		}
	}	
	if (jsonget("sayashud")!="") { llSay(0,jsonget("sayashud")); }
	i=1;
	while (jsonget("sayashud"+((string)i))!="") {
		llSay(0,jsonget("sayashud"+((string)i)));
		i++;
	}	
	if (jsonget("terminate")!="") {
		gphud_hang("=== TERMINATED ===: "+jsonget("terminate"));
	}			
	if (incommand=="shutdown" || jsonget("shutdown")!="") {
		gphud_hang("Shutdown requested: "+jsonget("shutdown"));
	}	
	if (incommand=="reboot" || jsonget("reboot")!="") {
		llOwnerSay("Rebooting at request from server: "+jsonget("reboot"));
		//shutdown();
		llResetScript();
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
		if (jsonget("objectdriverversioncheck")!="" && llGetOwnerKey(id)==llGetOwner()) { // If there's an OD version check from a remove object and it's owned by the same as us
			integer otherversion=(integer)(jsonget("objectdriverversioncheck")); // get the other version
			if (otherversion>ODVERSION && updatelock<llGetUnixTime()) {  // if their version is bigger than ours
				if (llGetInventoryType("GPHUD Object Driver Inhibitor")==INVENTORY_NONE)  { // and we're not inhibited
					integer pin=0;   // set a pin and request an update
					updatelock=llGetUnixTime()+300;
					while (pin>-1000 && pin<1000) {
						pin=((integer)(llFrand(1999999999)-1000000000));
					}
					llSetRemoteScriptAccessPin(pin);
					string jsonrequestupdate=llJsonSetValue("",["objectdriverupdatepin"],((string)pin));
					llRegionSayTo(id,broadcastchannel,jsonrequestupdate);
				}
			}
		}
		if(UPDATER && jsonget("objectdriverupdatepin")!="" && llGetOwnerKey(id)==llGetOwner()) {
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
		llSetTimerEvent(((integer)(llFrand(60.0)))+60.0);
	}
	listen(integer channel,string name,key id,string text) { all_listen(channel,name,id,text); }	
	changed(integer change) { if ((change & CHANGED_REGION) || (change & CHANGED_REGION_START) || (change & CHANGED_OWNER)) { llResetScript(); }}
	on_rez(integer parameter) { llResetScript(); }
	timer() {
		json=llJsonSetValue("",["objectdriverversioncheck"],((string)ODVERSION));
		llRegionSay(broadcastchannel,json);
		UPDATER=TRUE;
		llSetTimerEvent(((integer)(llFrand(60.0)))+900.0);
	}
}

state none {
	state_entry() { llSetTimerEvent(((integer)(llFrand(60.0)))+CHECKIN_MINUTES*60.0); 
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
	state_entry() { llSetTimerEvent(((integer)(llFrand(60.0)))+CHECKIN_MINUTES*60.0);
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
			vector toucherPos=llList2Vector(llGetObjectDetails(llDetectedKey(n),[OBJECT_POS]),0);
			float toucherDist=llVecDist(toucherPos,llGetPos());
			if (maxTouchDistance==0.0 || maxTouchDistance>toucherDist) {
				json=llJsonSetValue("",["clicker"],">"+llDetectedName(n));
				json=llJsonSetValue(json,["distance"],(string)toucherDist);
				httpcommand("objects.clicked","GPHUD/system");
			}
		}
	}
}
state phantom {
	state_entry() { llSetTimerEvent(((integer)(llFrand(60.0)))+CHECKIN_MINUTES*60.0);
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
			if (llDetectedType(n) & AGENT) {
				json=llJsonSetValue("",["collider"],">"+llDetectedName(n));
			}
		}
		httpcommand("objects.collided","GPHUD/system");
	}
}

