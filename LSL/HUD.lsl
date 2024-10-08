// NEW HUD :P
//#define DEBUG_BOOT
//#define DEBUG_JSON
#define COMMS_PROTOCOL "5"

#include "GPHUDHeader.lsl"
#include "SLCore/LSL/SetDev.lsl"
#include "SLCore/LSL/JsonTools.lsl"

// startup related stuff
integer AWAIT_GO=TRUE;
integer PERMISSIONS_STAGE=0;
integer URL_STAGE=0;
integer LISTENER_STAGE=0;
integer LOGIN_STAGE=0;
integer SETDEVTRUEONCE=TRUE;
// comms
string comms_url="";
key comms_url_key=NULL_KEY;
#include "configuration.lsl"
//listeners
integer channelonehandle=0;
integer broadcastchannelhandle=0;
integer rpchannelhandle=0;
// responders
key radarto=NULL_KEY;
// HUD related stuff
integer logincomplete=0;
string charname="Unknown";
integer rpchannel=0;
integer SHUTDOWN=TRUE;
vector titlercolor=<0,0,0>;
string titlertext="";
integer opencmd=FALSE;
string namelessprefix="";
string titlerz="0.19";
//// LOCAL INITIALISATION CODE 
getNewCommsURL() {
	URL_STAGE=-1;
	if (comms_url!="") { shutdown(); llReleaseURL(comms_url); }
	comms_url="";
	comms_url_key=llRequestURL();
}

setupListeners() {
	if (channelonehandle!=0) { llListenRemove(channelonehandle); }
	if (opencmd) { 
		channelonehandle=llListen(1,"",NULL_KEY,"");
	} else {
		channelonehandle=llListen(1,"",llGetOwner(),"");
	}
	calculatebroadcastchannel();
	if (broadcastchannelhandle!=0) { llListenRemove(broadcastchannelhandle); }
	broadcastchannelhandle=llListen(broadcastchannel,"",NULL_KEY,"");
	setupRpChannel();
}

setupRpChannel() {
	if (rpchannelhandle!=0) { llListenRemove(rpchannelhandle); }
	if (rpchannel>0) {
		rpchannelhandle=llListen(rpchannel,"",NULL_KEY,"");
	}
}

setup() {
	#ifdef DEBUG_BOOT
	llOwnerSay("Setup called : "+((string)llGetFreeMemory())+" byte free");
	#endif
	setDev(SETDEVTRUEONCE); SETDEVTRUEONCE=FALSE;

#ifndef NOEXPERIENCES	
	if (PERMISSIONS_STAGE==0) { PERMISSIONS_STAGE=-1; llRequestExperiencePermissions(llGetOwner(),""); }
#else
	if (PERMISSIONS_STAGE==0) { PERMISSIONS_STAGE=-1; llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
#endif	
	if (URL_STAGE==0) { getNewCommsURL(); }
	if (LISTENER_STAGE==0) { setupListeners(); LISTENER_STAGE=1; }
	
	if (PERMISSIONS_STAGE==1 && URL_STAGE==1 && LISTENER_STAGE==1 && LOGIN_STAGE==0) {
		startLogin();
		llRegionSayTo(llGetOwner(),broadcastchannel,"{\"hudreplace\":\"hudreplace\"}");
	}
}

startLogin() {
	#ifdef DEBUG_BOOT
	llOwnerSay("Resources ready, logging in with charid "+(string)logincomplete);
	#endif
	json="";
	jsonput("version",VERSION);
	jsonput("versiondate",COMPILEDATE);
	jsonput("versiontime",COMPILETIME);	
	jsonput("url",comms_url);
	jsonput("characterid",(string)logincomplete);
	command("GPHUDClient.Connect");
	LOGIN_STAGE=-1;	
}

brand() {llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,GPHUD_LOGO,<1,1,1>,<0,0,0>,0]);} // GPHUD branding

//// IMPORTED + REFORGED COMMS THINGS

command(string command) {
    jsonput("command",command);
    jsonput("developerkey",COMMS_DEVKEY);
	jsonput("protocol",COMMS_PROTOCOL);
	string devinject=""; if (DEV) { devinject="dev."; }
	string SERVER_URL="http://"+devinject+SERVER_HOSTNAME+"/GPHUD/system";
	#ifdef DEBUG_JSON
	llOwnerSay(llGetScriptName()+": Sending to "+SERVER_URL+"\n"+json);
	#endif
	llHTTPRequest(SERVER_URL,[HTTP_METHOD,"POST",HTTP_BODY_MAXLENGTH,4096],json);
}

shutdown() {
	if (SHUTDOWN) { return; }
	if (comms_url!="") {
		json="";
		jsonput("url",comms_url);
		command("GPHUDClient.Disconnect");
	}
	LOGIN_STAGE=0; SHUTDOWN=TRUE; llMessageLinked(LINK_THIS,LINK_SHUTDOWN,"",""); logincomplete=0;
}

//// PROCESSOR

integer process(key requestid) {
	string incommand=jsonget("incommand");
	if (jsonget("logincomplete")!="") {
		logincomplete=((integer)jsonget("logincomplete"));
		SHUTDOWN=FALSE;
		#ifdef DEBUG_BOOT
		llOwnerSay("Login complete, "+((string)llGetFreeMemory())+" bytes inside large process()");
		#endif
		llMessageLinked(LINK_THIS,LINK_STARTUP,"","");
	}
	if (jsonget("message")!="") { llOwnerSay(jsonget("message")); }
	integer i=1;
	while (jsonget("output"+((string)i))!="") {
		string line=jsonget("output"+((string)i));
		i++;
		string type=llGetSubString(line,0,0);
		line=llGetSubString(line,1,-1);
		if (type=="o") { llOwnerSay(line); }
		if (type=="s") { llSay(0,line); }
		if (type=="a") {
			integer index=llSubStringIndex(line,"|");
			string oldname=llGetObjectName();
			llSetObjectName(llGetSubString(line,0,index-1));
			llSay(0,llGetSubString(line,index+1,-1));
			llSetObjectName(oldname);
		}
	}
	if (jsonget("error")!="") { typedSay(jsonget("error")); }
	if (jsonget("opencmd")!="") { if (jsonget("opencmd")=="true") { opencmd=TRUE; setupListeners(); } else { opencmd=FALSE; setupListeners(); }}
	if (jsonget("terminate")!="") {
		gphud_hang("=== TERMINATED ===: "+jsonget("terminate"),TRUE);
	}			
	if (incommand=="shutdown" || jsonget("shutdown")!="") {
		gphud_hang("Shutdown requested: "+jsonget("shutdown"),TRUE);
	}	
	if (incommand=="reboot" || jsonget("reboot")!="") {
		llOwnerSay("Restarting: "+jsonget("reboot"));
		shutdown();
	}
	if (jsonget("disconnected")=="disconnected") {
		llOwnerSay("Disconnected, reconnecting...");
		setup();
	}		
	if (incommand=="forcereconnect") { startLogin(); }
	integer DONOTRESPOND=FALSE;
	string retjson="";
	if (incommand=="radar") { DONOTRESPOND=TRUE; llSensor("",NULL_KEY,AGENT,20,PI); radarto=requestid; }
	if (incommand=="openurl") { llLoadURL(llGetOwner(),jsonget("description"),jsonget("openurl")); }
	if (jsonget("motd")!="") { llOwnerSay("MOTD: "+jsonget("motd")); }
	if (jsonget("titlerz")!="") { titlerz=jsonget("titlerz"); llRegionSayTo(llGetOwner(),broadcastchannel,llJsonSetValue("",["titlerz"],titlerz)); }
	if (jsonget("titlercolor")!="") { titlercolor=(vector)jsonget("titlercolor"); }	
	if (jsonget("titlertext")!="") { titlertext=jsonget("titlertext"); }
	if (jsonget("regettitletext")!="" || jsonget("titlertext")!="" || jsonget("titlercolor")!="") { 
		string totitler=llJsonSetValue("",["titler"],(string)titlercolor+"|"+titlertext);
		totitler=llJsonSetValue(totitler,["titlerz"],titlerz);
		llRegionSayTo(llGetOwner(),broadcastchannel,totitler);
		//llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titler\":\""+(string)titlercolor+"|"+titlertext+"\"}");
	}
	if (jsonget("hudreplace")!="") { gphud_hang("Duplicate GPHUD attached, detaching one",FALSE); }
	if (jsonget("eventmessage1")!="") { llOwnerSay(jsonget("eventmessage1")); }
	if (jsonget("eventmessage2")!="") { llOwnerSay(jsonget("eventmessage2")); }
	if (jsonget("leveltext")!="") { llOwnerSay(jsonget("leveltext")); }
	if (jsonget("rpchannel")!="") { rpchannel=(integer)jsonget("rpchannel"); setupRpChannel();}
	if (jsonget("name")!="") { charname=jsonget("name"); }
	if (jsonget("namelessprefix")!="") { namelessprefix=jsonget("namelessprefix"); }
	if (jsonget("teleport")!="") {
		list pieces=llParseString2List(jsonget("teleport"),["|"],[]);
		llTeleportAgentGlobalCoords(llGetOwner(),(vector)(llList2String(pieces,0)),(vector)(llList2String(pieces,1)),(vector)(llList2String(pieces,2)));
	}
	if (jsonget("logincommand")!="") {
		string logincommand=jsonget("logincommand");
		string oldjson=json;
		json="";
		jsonput("commandtoinvoke",logincommand);
		command("GPHUDClient.call");
		json=oldjson; oldjson="";
	}	
	if (jsonget("titler")!="") {
		string r=llJsonSetValue("",["attachmentpoint"],jsonget("titler"));
		r=llJsonSetValue(r,["dispensetitler"],(string)llGetOwner());
		if (((integer)jsonget("titler"))!=0) { llRegionSay(broadcastchannel,r); }
	}
	json=retjson;
	if (DONOTRESPOND) { return FALSE; }
	return TRUE;
}

gphud_hang(string reason,integer killTitler) {
	shutdown();
	if (reason!="") { llOwnerSay(reason); }
	if (llGetInventoryType("Attacher")!=INVENTORY_SCRIPT) {
		llSetLinkPrimitiveParamsFast(LINK_SET,[PRIM_TEXT,"",<0,0,0>,0,PRIM_COLOR,ALL_SIDES,<0,0,0>,0,PRIM_POS_LOCAL,<-10,-10,-10>]);
		if (killTitler) { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerremove\":\"titlerremove\"}"); }
		llSleep(2.0/45.0);
		llDetachFromAvatar();
	} else { 
		llOwnerSay("Shutdown and not detaching");
		llSetText("Shutdown",<1,.8,.8>,1);
		SHUTDOWN=TRUE; llMessageLinked(LINK_THIS,LINK_SHUTDOWN,"","");
	}
}

//// EVENT HANDLER

default {
	state_entry() {
		llSetObjectName("GPHUD");
		key k=SLCORE_COAGULATE_LOGO;
		setDev(FALSE);
		if (DEV) { k=SLCORE_COAGULATE_DEV_LOGO; }
		llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,k,<1,1,1>,<0,0,0>,0]);
		llResetOtherScript("UI");llResetOtherScript("UIX");
		llSetText("",<0,0,0>,0);
		if (llGetInventoryType("Attacher")==INVENTORY_SCRIPT) {
			llResetOtherScript("Attacher"); AWAIT_GO=TRUE;
			llSetText("Coagulate GPHUD: Waiting for GO",<0.75,0.75,1.0>,1);
		} else {
			AWAIT_GO=FALSE; SHUTDOWN=FALSE;
			brand();
			setup();
		}
	}
	link_message(integer from,integer num,string message,key id) {
		if (num==LINK_GO && AWAIT_GO==TRUE) {
			setDev(FALSE);
			SHUTDOWN=FALSE;
			#ifdef DEBUG
			llOwnerSay("Attacher GO");
			#endif
			brand();
			setup();
			AWAIT_GO=FALSE;
		}
	}
#ifndef NOEXPERIENCES	
	experience_permissions(key id) {
		#ifdef DEBUG_BOOT
		llOwnerSay("Detach set experience, calling setup");
		#endif
		PERMISSIONS_STAGE=1;
		setup();
	}
	experience_permissions_denied(key id,integer reason) {
		llRequestPermissions(llGetOwner(),PERMISSION_ATTACH);
	}
#endif	
	run_time_permissions(integer perms) {
		if(perms & PERMISSION_ATTACH) { 
			#ifdef DEBUG_BOOT
			llOwnerSay("Detach set manual");
			#endif
			PERMISSIONS_STAGE=1;
			setup();
		} else { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	}
	http_request(key id,string method,string body) {
		if (method==URL_REQUEST_DENIED && id==comms_url_key) {
			llOwnerSay("GPHUD URL Claim failed: "+body+", retry in 15s"); 
			URL_STAGE=0; comms_url_key=NULL_KEY;
			llSleep(15);
			setup();
			return;
		}	
		if (method==URL_REQUEST_GRANTED && id==comms_url_key) {
			#ifdef DEBUG_BOOT
			llOwnerSay("URL complete");
			#endif
			comms_url_key=NULL_KEY;
			comms_url=body;		
			URL_STAGE=1;
			setup();
			return;
		}	
		json=body; body="";
		#ifdef DEBUG_JSON
		llOwnerSay("IN:"+json);
		#endif
		llMessageLinked(LINK_THIS,LINK_RECEIVE,json,"");
		if (process(id)) { llHTTPResponse(id,200,json); }
	}	
	http_response( key request_id, integer status, list metadata, string body ) {
		#ifdef DEBUG_JSON 
		llOwnerSay("REPLY:"+body);
		#endif
		if (status!=200) {
			llOwnerSay(llGetScriptName()+" : Stack Server failed (#"+((string)status)+").  Please retry your last operation.");
		}
		else
		{
			json=body; body="";
			process(NULL_KEY);
		}
	}		
	listen(integer channel,string name,key id,string text) {
		if (channel==broadcastchannel) {
			if (text=="GOTHUD") {
				llRegionSayTo(id,broadcastchannel,"GOTHUD");
				return;
			}
			json=text;
			json=llJsonSetValue(json,["incommand"],"broadcast");
			llMessageLinked(LINK_THIS,LINK_RECEIVE,json,"");	
			process(NULL_KEY);
		}
		if (channel==1 && (id==llGetOwner() || (llGetOwnerKey(id)==llGetOwner() && opencmd==TRUE))) {
			if (text=="status" && id==SYSTEM_OWNER_UUID) { llOwnerSay("HUD: "+(string)llGetFreeMemory()); llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"",""); return;}
			if (text=="reconnect") { shutdown(); getNewCommsURL(); return; }
			if (text=="shutdown") { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerremove\":\"titlerremove\"}"); llSleep(2.0/45.0); gphud_hang("HUD shutdown requested by wearer.",TRUE); }
			if (text=="reboot") { llResetScript(); }
			if (logincomplete==0) { return; }
			if (!SHUTDOWN) {
				json=llJsonSetValue("",["console"],text);
				command("console");
			}
		}
		if (logincomplete==0) { return; }
		if (channel==rpchannel && id==llGetOwner() && !SHUTDOWN) {
			string prename=llGetObjectName();
			string nameas=charname;
			if (namelessprefix!="" && namelessprefix!=" " && llSubStringIndex(text,namelessprefix)==0) {
				text=llDeleteSubString(text,0,0);
				nameas="";
			}
			llSetObjectName(nameas);
			llSay(0,text);
			llSetObjectName(prename);
		}
	}
	touch_start(integer n)
	{
		if (AWAIT_GO==TRUE) { return; }
		if (llDetectedLinkNumber(0)==1 && logincomplete==0) {
			llOwnerSay("Retrying character registration");
			startLogin();
			return;
		}
		if (llDetectedLinkNumber(0)!=1 && !SHUTDOWN) {
			string name=llGetLinkName(llDetectedLinkNumber(0));
			if (name!="legacymenu" && name!="" && llSubStringIndex(name,"!!")!=0) {
				json="";
				if (llSubStringIndex(name,".")==-1) { 
					command("gphudclient."+name);
				} else {
					jsonput("commandtoinvoke",name);
					command("GPHUDClient.call");
				}
			}
		}
	}

	no_sensor() {
		if (radarto!=NULL_KEY) {
			llHTTPResponse(radarto,200,"{\"avatars\":\"\"}");
			radarto=NULL_KEY;
		}
	}
	sensor(integer n) {
		integer i=0;
		string keys="";
		for (i=0;i<n;i++) {
			if (keys!="") { keys+=","; }
			keys+=(string)llDetectedKey(i);
		}
		if (radarto!=NULL_KEY) {
			llHTTPResponse(radarto,200,"{\"avatars\":\""+keys+"\"}");
			radarto=NULL_KEY;
		}
	}
	changed (integer what) {
		if (what & CHANGED_REGION) { 
			// reset the URL stuff
			shutdown();
			setupListeners();
			getNewCommsURL();		
		}
	}
}
