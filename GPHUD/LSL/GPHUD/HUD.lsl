// NEW HUD :P
//#define DEBUG_BOOT
//#define DEBUG_JSON

#include "SL/LSL/Constants.lsl"
#include "SL/LSL/GPHUD/GPHUDHeader.lsl"
#include "SL/LSL/Library/SetDev.lsl"
#include "SL/LSL/Library/JsonTools.lsl"

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
integer comms_node=-99;
integer comms_node_cycled=0;
#define COMMS_DEVKEY "***REMOVED***"
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
integer BANNERED=FALSE;
integer SHUTDOWN=FALSE;

//// LOCAL INITIALISATION CODE 
getNewCommsURL() {
	URL_STAGE=-1;
	if (comms_url!="") { shutdown(); llReleaseURL(comms_url); }
	comms_url="";
	comms_url_key=llRequestURL();
}

setupListeners() {
	if (channelonehandle!=0) { llListenRemove(channelonehandle); }
	channelonehandle=llListen(1,"",llGetOwner(),"");
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
	
	if (PERMISSIONS_STAGE==0) { PERMISSIONS_STAGE=-1; llRequestExperiencePermissions(llGetOwner(),""); }
	if (URL_STAGE==0) { getNewCommsURL(); }
	if (LISTENER_STAGE==0) { setupListeners(); LISTENER_STAGE=1; }
	
	if (PERMISSIONS_STAGE==1 && URL_STAGE==1 && LISTENER_STAGE==1 && LOGIN_STAGE==0) {
		startLogin();
		if (!BANNERED) { banner_hud(); BANNERED=TRUE; }
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

brand() {llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,"36c48d34-3d84-7b9a-9979-cda80cf1d96f",<1,1,1>,<0,0,0>,0]);} // GPHUD branding

//// IMPORTED + REFORGED COMMS THINGS

command(string command) {
    jsonput("command",command);
    jsonput("developerkey",COMMS_DEVKEY);
	jsonput("protocol","2"); // used to redirect various behaviours in the Java side for login
	
	string devinject=""; if (DEV) { devinject="dev"; }
	string SERVER_URL="http://Virtual"+((string)comms_node)+devinject+".SL.Coagulate.NET/GPHUD/system";
	#ifdef DEBUG_JSON
	llOwnerSay(llGetScriptName()+": Sending to "+SERVER_URL+"\n"+json);
	#endif
	llHTTPRequest(SERVER_URL,[HTTP_METHOD,"POST",HTTP_BODY_MAXLENGTH,4096],json);
}

shutdown() {
	if (comms_url!="") {
		json="";
		jsonput("url",comms_url);
		command("GPHUDClient.Disconnect");
	}
	LOGIN_STAGE=0;
}

//// PROCESSOR

// ADD A MESSAGE called uixupdate or something that makes the other script call uix.update or similar to download the HUD presentation.  or see what can be done in 2k.

integer process(key requestid) {
	string incommand=jsonget("incommand");
	if (jsonget("logincomplete")!="") {
		logincomplete=((integer)jsonget("logincomplete"));
		#ifdef DEBUG_BOOT
		llOwnerSay("Login complete, "+((string)llGetFreeMemory())+" bytes inside large process()");
		#endif
		llMessageLinked(LINK_THIS,LINK_SET_STAGE,"99","");
	}
	if (jsonget("message")!="") { llOwnerSay(jsonget("message")); }
	if (jsonget("say")!="") {
		string oldname=llGetObjectName();
		string newname=jsonget("sayas");
		if (newname!="") { llSetObjectName(newname); }
		llSay(0,jsonget("say"));
		if (newname!="") { llSetObjectName(oldname); }
	}
	if (jsonget("sayashud")!="") { llSay(0,jsonget("sayashud")); }
	if (jsonget("error")!="") { typedSay(jsonget("error")); }
	if (jsonget("terminate")!="") {
		gphud_hang("=== TERMINATED ===: "+jsonget("terminate"));
	}			
	if (incommand=="shutdown" || jsonget("shutdown")!="") {
		gphud_hang("Shutdown requested: "+jsonget("shutdown"));
	}	
	if (incommand=="reboot" || jsonget("reboot")!="") {
		llOwnerSay("Rebooting at request from server: "+jsonget("reboot"));
		shutdown();
		setup();
	}
	if (incommand=="forcereconnect") { startLogin(); }
	integer DONOTRESPOND=FALSE;
	string retjson="";
	//llOwnerSay("WE ARE HERE WITH "+json);
	/*if (jsonget("error")!="" && BOOTSTAGE==BOOT_APP) { // failed to login / create character?  so blind retry? :P
		llOwnerSay("Error during login/registration, please click the HUD to retry...");
		llSetText("Registration failed - click HUD to retry registration...",<1,.5,.5>,1);
		retrylogin=TRUE;
		return TRUE;	
	}*/
	if (incommand=="radar") { DONOTRESPOND=TRUE; llSensor("",NULL_KEY,AGENT,20,PI); radarto=requestid; }
	if (incommand=="registered") { /*cookie=jsonget("cookie");*/ /*BOOTSTAGE=BOOT_COMPLETE; llMessageLinked(LINK_THIS,LINK_SET_STAGE,(string)BOOTSTAGE,NULL_KEY); */ }
	if (incommand=="ping") { /*retjson=llJsonSetValue(retjson,["cookie"],cookie);*/ }
	//if (jsonget("sizeratio")!="") { sizeratio=(integer)jsonget("sizeratio"); reflowHUD(); }
	if (incommand=="openurl") { llLoadURL(llGetOwner(),jsonget("description"),jsonget("openurl")); }
	if (jsonget("setlogo")!="") { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,jsonget("setlogo"),<1,1,1>,<0,0,0>,0]); }
	/*
	if (jsonget("qb1texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB1,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb1texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb2texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB2,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb2texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb3texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB3,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb3texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb4texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB4,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb4texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb5texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB5,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb5texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb6texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB6,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb6texture"),<1,1,1>,<0,0,0>,0]); }
	*/
	if (jsonget("motd")!="") { llOwnerSay("MOTD: "+jsonget("motd")); }
	/*
	if (jsonget("titlerz")!="") { llRegionSayTo(llGetOwner(),broadcastchannel,llJsonSetValue("",["titlerz"],jsonget("titlerz"))); }
	if (jsonget("titlercolor")!="") { titlercolor=(vector)jsonget("titlercolor"); }	
	if (jsonget("titlertext")!="") { titlertext=jsonget("titlertext"); }
	if (jsonget("titlertext")!="" || jsonget("titlercolor")!="") { 
		string totitler=llJsonSetValue("",["titler"],(string)titlercolor+"|"+titlertext);
		llRegionSayTo(llGetOwner(),broadcastchannel,totitler);
		//llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titler\":\""+(string)titlercolor+"|"+titlertext+"\"}");
	}
	if (jsonget("messagecount")!="") {
		integer messages=(integer)jsonget("messagecount");
		if (messages==0) { llSetLinkPrimitiveParamsFast(LINK_MESSAGES,[PRIM_COLOR,ALL_SIDES,<0.627, 1.000, 1.000>,0]); } 
		else { llSetLinkPrimitiveParamsFast(LINK_MESSAGES,[PRIM_COLOR,ALL_SIDES,<0.627, 1.000, 1.000>,1]);
			string s=""; if (messages>1) { s="s"; }
			llOwnerSay("You have "+(string)messages+" new message"+s+".  Click the envelope to read.");
		} 
	}
	*/
	if (jsonget("hudreplace")!="") { gphud_hang("Duplicate GPHUD attached, detaching one"); }
	if (jsonget("eventmessage1")!="") { llOwnerSay(jsonget("eventmessage1")); }
	if (jsonget("eventmessage2")!="") { llOwnerSay(jsonget("eventmessage2")); }
	if (jsonget("leveltext")!="") { llOwnerSay(jsonget("leveltext")); }
	if (jsonget("rpchannel")!="") { rpchannel=(integer)jsonget("rpchannel"); setupRpChannel();}
	if (jsonget("name")!="") { charname=jsonget("name"); }
	if (jsonget("teleport")!="") {
		list pieces=llParseString2List(jsonget("teleport"),["|"],[]);
		llTeleportAgentGlobalCoords(llGetOwner(),(vector)(llList2String(pieces,0)),(vector)(llList2String(pieces,1)),(vector)(llList2String(pieces,2)));
	}
	json=retjson;
	if (DONOTRESPOND) { return FALSE; }
	return TRUE;
}

gphud_hang(string reason) {
	shutdown();
	if (reason!="") { llOwnerSay(reason); }
	if (llGetInventoryType("Attacher")!=INVENTORY_SCRIPT) {
		llSetLinkPrimitiveParamsFast(LINK_SET,[PRIM_TEXT,"",<0,0,0>,0,PRIM_COLOR,ALL_SIDES,<0,0,0>,0,PRIM_POS_LOCAL,<-10,-10,-10>]);
		llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerremove\":\"titlerremove\"}"); llSleep(2.0/45.0); llDetachFromAvatar();
	}
	llOwnerSay("Shutdown and not detaching");
	llSetText("Shutdown",<1,.8,.8>,1);
	SHUTDOWN=TRUE; llMessageLinked(LINK_THIS,LINK_SHUTDOWN,"","");
}

//// EVENT HANDLER

default {
	state_entry() {
		llSetObjectName("GPHUD");
		key k=LOGO_COAGULATE;
		setDev(FALSE);
		if (DEV) { k=LOGO_COAGULATE_DEV; }
		llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,k,<1,1,1>,<0,0,0>,0]);
		llResetOtherScript("UI");llResetOtherScript("UIX");
		llSetText("",<0,0,0>,0);
		if (llGetInventoryType("Attacher")==INVENTORY_SCRIPT) {
			llResetOtherScript("Attacher"); AWAIT_GO=TRUE;
			llSetText("Coagulate GPHUD: Waiting for GO",<0.75,0.75,1.0>,1);
		} else {
			AWAIT_GO=FALSE;
			brand();
			setup();
		}
	}
	link_message(integer from,integer num,string message,key id) {
		if (SHUTDOWN) { return; }	
		if (num==LINK_GO && AWAIT_GO==TRUE) {
			#ifdef DEBUG
			llOwnerSay("Attacher GO");
			#endif
			brand();
			setup();
			AWAIT_GO=FALSE;
		}
	}
	experience_permissions(key id) {
		if (SHUTDOWN) { return; }	
		#ifdef DEBUG_BOOT
		llOwnerSay("Detach set experience, calling setup");
		#endif
		PERMISSIONS_STAGE=1;
		setup();
	}
	experience_permissions_denied(key id,integer reason) {
		if (SHUTDOWN) { return; }	
		llRequestPermissions(llGetOwner(),PERMISSION_ATTACH);
	}
	run_time_permissions(integer perms) {
		if (SHUTDOWN) { return; }	
		if(perms & PERMISSION_ATTACH) { 
			#ifdef DEBUG_BOOT
			llOwnerSay("Detach set manual");
			#endif
			PERMISSIONS_STAGE=1;
			setup();
		} else { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	}
	http_request(key id,string method,string body) {
		if (SHUTDOWN) { return; }	
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
			if (comms_node==-99) { comms_node=((integer)llFrand(6.0)); }
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
		if (SHUTDOWN) { return; }	
		#ifdef DEBUG_JSON 
		llOwnerSay("REPLY:"+body);
		#endif
		if (status!=200) {
			comms_node_cycled++;
			if (comms_node_cycled>6) {
				string s="All servers failed, sleeping for 5 minutes then rebooting.";
				llOwnerSay(s);
				llSetText(s,<1,.5,.5>,1);
				llSleep(300);
				llResetScript();
			}
			llOwnerSay(llGetScriptName()+" : Cluster Server "+((string)comms_node)+" failed.  Please retry your last operation.");
			comms_node=(comms_node+1)%6;
		}
		else
		{
			json=body; body="";
			process(NULL_KEY);
		}
	}		
	listen(integer channel,string name,key id,string text) {
		if (SHUTDOWN) { return; }	
		if (channel==broadcastchannel) {
			if (text=="GOTHUD") {
				llRegionSayTo(id,broadcastchannel,"GOTHUD");
				return;
			}
			json=text;
			json=llJsonSetValue(json,["incommand"],"broadcast");
			//llOwnerSay("broadcast:"+json);
			llMessageLinked(LINK_THIS,LINK_RECEIVE,json,"");	
			process(NULL_KEY);
		}
		if (channel==1 && id==llGetOwner()) {
			if (text=="status" && id==IAIN_MALTZ) { llOwnerSay("HUD: "+(string)llGetFreeMemory()); llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"",""); return;}
			if (text=="reconnect") { shutdown(); getNewCommsURL(); return; }
			if (text=="shutdown") { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerremove\":\"titlerremove\"}"); llSleep(2.0/45.0); gphud_hang(""); }
			if (text=="reboot") { llResetScript(); }
			if (logincomplete==0) { return; }
			json=llJsonSetValue("",["console"],text);
			command("console");
		}
		if (logincomplete==0) { return; }
		if (channel==rpchannel) {
			string name=llGetObjectName(); llSetObjectName(charname); llSay(0,text); llSetObjectName(name);
		}
	}
	on_rez(integer parameter) { llResetScript(); }
	touch_start(integer n)
	{
		if (SHUTDOWN) { if (llDetectedKey(0)==IAIN_MALTZ) { llResetScript(); } return; }	
		if (AWAIT_GO==TRUE) { return; }
		if (logincomplete==0) {
			llSetText("Retry character registration...",<1,.5,.5>,1);
			startLogin();
			return;
		}
		if (llDetectedLinkNumber(0)!=1) {
			//llOwnerSay("Link number "+((string)llDetectedLinkNumber(0)));
			string name=llGetLinkName(llDetectedLinkNumber(0));
			//llOwnerSay("Link name "+name);
			if (name!="legacymenu" && name!="") {
				json="";
				if (llSubStringIndex(name,".")==-1) { 
					//llOwnerSay("GPHUDDirect "+name);
					command("gphudclient."+name);
				} else {
					//llOwnerSay("GPHUDClient.call "+name);
					jsonput("commandtoinvoke",name);
					command("GPHUDClient.call");
				}
			}
		}
	}

	no_sensor() {
		if (SHUTDOWN) { return; }	
		if (radarto!=NULL_KEY) {
			llHTTPResponse(radarto,200,"{\"avatars\":\"\"}");
			radarto=NULL_KEY;
		}
	}
	sensor(integer n) {
		if (SHUTDOWN) { return; }
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
		if (SHUTDOWN) { return; }
		if (what & CHANGED_REGION) { 
			// reset the URL stuff
			shutdown();
			getNewCommsURL();		
		}
	}
}
