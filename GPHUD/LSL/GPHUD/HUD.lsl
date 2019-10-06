//#define DEBUG
#include "SL/LSL/Constants.lsl"
#include "SL/LSL/Library/SetDev.lsl"
#include "SL/LSL/GPHUD/GPHUDHeader.lsl"
#include "SL/LSL/Library/JsonTools.lsl"

#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
#define COMMS_INCLUDEDIGEST
#define COMMS_DEVKEY "***REMOVED***"
#include "SL/LSL/Library/CommsV3.lsl"

integer LINK_QB1=-1;
integer LINK_QB2=-1;
integer LINK_QB3=-1;
integer LINK_QB4=-1;
integer LINK_QB5=-1;
integer LINK_QB6=-1;
integer LINK_MESSAGES=-1;
integer sizeratio=2;
key radarto=NULL_KEY;
integer detach=FALSE;
string hudtext="Loading...";
vector hudcolor=<1,1,1>;
string titlertext="Loading...";
list ok=[];
vector titlercolor=<1,1,1>;
string charname="";
integer rpchannel=0;
integer rpchannelhandle=0;
integer retrylogin=FALSE;

dodetach() {
	//llSetLinkPrimitiveParamsFast(LINK_SET,[PRIM_TEXT,"",<0,0,0>,0,PRIM_COLOR,ALL_SIDES,<0,0,0>,0,PRIM_POS_LOCAL,<-10,-10,-10>]);
	llDetachFromAvatar();
}
reflowHUD() {
	float sr=((float)sizeratio);
	llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<0.01,0.12*sr,0.12>,
		PRIM_LINK_TARGET,LINK_QB2,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02,0.04>,
		PRIM_LINK_TARGET,LINK_QB1,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02+0.04,0.04>,
		PRIM_LINK_TARGET,LINK_QB4,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02,0.0>,
		PRIM_LINK_TARGET,LINK_QB3,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02+0.04,0.0>,
		PRIM_LINK_TARGET,LINK_QB6,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02,-0.04>,
		PRIM_LINK_TARGET,LINK_QB5,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02+0.04,-0.04>,
		PRIM_LINK_TARGET,LINK_MESSAGES,PRIM_POSITION,<0.01,-0.12*sr/2.0-0.02,0.04>
		]);
}
startLogin() {
	LOGIN_ATTEMPTS--;
	if (LOGIN_ATTEMPTS<=0) { llOwnerSay("FAILED REGISTRATION 10 TIMES.  Shutting down in case we are stuck in a loop!  Please contact support."); gphud_hang(); }
	json=llJsonSetValue("",["version"],VERSION);
	json=llJsonSetValue(json,["versiondate"],COMPILEDATE);
	json=llJsonSetValue(json,["versiontime"],COMPILETIME);	
	httpcommand("characters.login","GPHUD/system");
}
integer LOGIN_ATTEMPTS=10;

integer process(key id) {
	gphud_process();
	string incommand=jsonget("incommand");
	integer DONOTRESPOND=FALSE;
	string retjson="";
	//llOwnerSay("WE ARE HERE WITH "+json);
	if (jsonget("error")!="" && BOOTSTAGE==BOOT_APP) { // failed to login / create character?  so blind retry? :P
		llOwnerSay("Error during login/registration, please click the HUD to retry...");
		llSetText("Registration failed - click HUD to retry registration...",<1,.5,.5>,1);
		retrylogin=TRUE;
		return TRUE;	
	}
	if (incommand=="radar") { DONOTRESPOND=TRUE; llSensor("",NULL_KEY,AGENT,20,PI); radarto=id; }
	if (incommand=="registered") { cookie=jsonget("cookie"); BOOTSTAGE=BOOT_COMPLETE; llMessageLinked(LINK_THIS,LINK_SET_STAGE,(string)BOOTSTAGE,NULL_KEY); }
	if (incommand=="ping") { retjson=llJsonSetValue(retjson,["cookie"],cookie); }
	if (jsonget("sizeratio")!="") { sizeratio=(integer)jsonget("sizeratio"); reflowHUD(); }
	if (incommand=="openurl") { llLoadURL(llGetOwner(),jsonget("description"),jsonget("url")); }
	if (jsonget("setlogo")!="") { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,jsonget("setlogo"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb1texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB1,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb1texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb2texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB2,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb2texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb3texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB3,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb3texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb4texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB4,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb4texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb5texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB5,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb5texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("qb6texture")!="") { llSetLinkPrimitiveParamsFast(LINK_QB6,[PRIM_TEXTURE,ALL_SIDES,jsonget("qb6texture"),<1,1,1>,<0,0,0>,0]); }
	if (jsonget("motd")!="") { llOwnerSay("MOTD: "+jsonget("motd")); }
	if (jsonget("hudcolor")!="") { hudcolor=(vector)jsonget("hudcolor"); }
	if (jsonget("hudtext")!="") { hudtext=jsonget("hudtext"); }
	if (jsonget("hudtext")!="" || jsonget("hudcolor")!="") { 
		llSetText(hudtext,hudcolor,1);
	}
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
	if (jsonget("hudreplace")!="") { llOwnerSay("Duplicate GPHUD attached, detaching one"); dodetach(); }
	if (jsonget("eventmessage1")!="") { llOwnerSay(jsonget("eventmessage1")); }
	if (jsonget("eventmessage2")!="") { llOwnerSay(jsonget("eventmessage2")); }
	if (jsonget("leveltext")!="") { llOwnerSay(jsonget("leveltext")); }
	if (jsonget("rpchannel")!="") { rpchannel=(integer)jsonget("rpchannel"); setupRpChannel();}
	if (jsonget("name")!="") { charname=jsonget("name"); }
	json=retjson;
	if (DONOTRESPOND) { return FALSE; }
	return TRUE;
}
gphud_hang() {
	if (detach) { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerremove\":\"titlerremove\"}"); llSleep(2.0/45.0); dodetach(); }
	report("Has shutdown",<1,1,.5>);
	while (TRUE) { llSleep(60.0); }
}
report(string msg,vector col) {
	string inject=""; setDev(FALSE);
	llSetText(msg+"\n \n[GPHUD "+inject+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"]",col,1);
}

brand() {llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,"36c48d34-3d84-7b9a-9979-cda80cf1d96f",<1,1,1>,<0,0,0>,0]);} // GPHUD branding
setupRpChannel() {
	if (rpchannelhandle!=0){llListenRemove(rpchannelhandle);}
	if (rpchannel!=0) { rpchannelhandle=llListen(rpchannel,"",llGetOwner(),""); }
}


setup() {
	comms_setup();
	if (BOOTSTAGE==BOOT_COMMS) { 
		key k=LOGO_COAGULATE;
		setDev(TRUE);
		if (DEV) { k=LOGO_COAGULATE_DEV; DEV=TRUE; }
		setupRpChannel();
		llRequestExperiencePermissions(llGetOwner(),"");		
		calculatebroadcastchannel();
		llListen(broadcastchannel,"",NULL_KEY,"");		
		llResetOtherScript("UI");
		llListen(1,"",llGetOwner(),"");
		llRegionSayTo(llGetOwner(),broadcastchannel,"{\"hudreplace\":\"hudreplace\"}");
		return;
	}
	if (BOOTSTAGE==BOOT_APP) {
		#ifdef DEBUG
		llOwnerSay("SETUP stage 1 - bidirectional comms is a GO, GPHUD logo, banner, and commence login to GPHUD service");
		#endif
		brand();
		banner_hud();
		startLogin();
		return;
	}
}

default {

	http_request(key id,string method,string body) {
		json=body; body="";
		#ifdef DEBUG
		llOwnerSay("IN:"+body);
		#endif
		
		if (comms_http_request(id,method)) { llHTTPResponse(id,200,json); return; }
		
		//llOwnerSay("HTTPIN:"+json);
		
		if (process(id)) { llHTTPResponse(id,200,json); }
	}	
	http_response( key request_id, integer status, list metadata, string body ) {
		#ifdef DEBUG
		llOwnerSay("REPLY:"+body);
		#endif
		if (status!=200) {
			comms_error((string)status);
			comms_do_callback();
		}
		else
		{
			json=body;
			
			if (comms_http_response(request_id,status)) { return; }
			
			process(NULL_KEY);
		}
	}	
	
	//====================================
	state_entry() {
		llSetObjectName("GPHUD");
		key k=LOGO_COAGULATE;
		setDev(FALSE);
		if (DEV) { k=LOGO_COAGULATE_DEV; }
		llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,k,<1,1,1>,<0,0,0>,0]);
		llResetOtherScript("UI");
		integer p=llGetNumberOfPrims();
		for (;p>0;p--)	{
			string primname=llToLower(llGetLinkName(p));
			if (primname=="quickbutton1") { LINK_QB1=p; }
			if (primname=="quickbutton2") { LINK_QB2=p; }
			if (primname=="quickbutton3") { LINK_QB3=p; }
			if (primname=="quickbutton4") { LINK_QB4=p; }
			if (primname=="quickbutton5") { LINK_QB5=p; }
			if (primname=="quickbutton6") { LINK_QB6=p; }
			if (primname=="getmessage") { LINK_MESSAGES=p; }
			
		}
		if (LINK_MESSAGES==-1 ||
			LINK_QB1==-1 ||
			LINK_QB2==-1 ||
			LINK_QB3==-1 ||
			LINK_QB4==-1 ||
			LINK_QB5==-1 ||
			LINK_QB6==-1) { llOwnerSay("PANIC! LINK NOT SET)");  while (1==1) { llSleep(100.0); } }
		if (llGetInventoryType("Attacher")==INVENTORY_SCRIPT) {
			llResetOtherScript("Attacher");
			report("Waiting for GO",<0.75,0.75,1.0>);
		} else { setup(); }
	}
	link_message(integer from,integer num,string message,key id) {
		if (num==LINK_GO && BOOTSTAGE==BOOT_COMMS) {
			#ifdef DEBUG
			llOwnerSay("Attacher GO");
			#endif
			setup();
		}
	}

	changed(integer change)
	{
		if (change & (CHANGED_REGION))
		{
			dodetach();
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
			//llOwnerSay("broadcast:"+json);
			llMessageLinked(LINK_THIS,LINK_RECEIVE,json,"");	
			process(NULL_KEY);
		}
		if (channel==1 && id==llGetOwner()) {
			if (text=="status" && id==IAIN_MALTZ) { llOwnerSay("HUD: "+(string)llGetFreeMemory()); llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"",""); return;}
			if (text=="shutdown") { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerremove\":\"titlerremove\"}"); llSleep(2.0/45.0); dodetach(); }
			if (text=="reboot") { llResetScript(); }
			if (BOOTSTAGE==BOOT_COMMS) { return; }
			json=llJsonSetValue("",["console"],text);
			httpcommand("console","GPHUD/system");
		}
		if (BOOTSTAGE<BOOT_COMPLETE) { return; }
		if (channel==rpchannel) {
			string name=llGetObjectName(); llSetObjectName(charname); llSay(0,text); llSetObjectName(name);
		}
	}
	on_rez(integer parameter) { if (BOOTSTAGE>BOOT_COMMS) { llResetScript(); } }
	touch_start(integer n)
	{
		if (retrylogin) {
			retrylogin=FALSE;
			llSetText("Retry character registration...",<1,.5,.5>,1);
			startLogin();
		}
		if (BOOTSTAGE<BOOT_COMPLETE) { return; }
		if (llDetectedLinkNumber(0)!=1) {
			string name=llGetLinkName(llDetectedLinkNumber(0));
			if (name!="legacymenu") {
				json="";
				httpcommand("gphudclient."+name,"GPHUD/system");
			}
		}
	}
	experience_permissions(key id) {
		detach=TRUE;
		#ifdef DEBUG
		llOwnerSay("Detach set experience");
		#endif
	}
	experience_permissions_denied(key id,integer reason) { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	run_time_permissions(integer perms) {
		if(perms & PERMISSION_ATTACH) { 
			detach=TRUE;
			#ifdef DEBUG
			llOwnerSay("Detach set manual");
			#endif
		} else { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
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

	
}
