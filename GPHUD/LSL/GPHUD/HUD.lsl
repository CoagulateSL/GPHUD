//#define DEBUG
#include "SL/LSL/Constants.lsl"
#include "SL/LSL/Library/SetDev.lsl"
#include "SL/LSL/Comms/Header.lsl"
#include "SL/LSL/GPHUD/GPHUDHeader.lsl"
#include "SL/LSL/Comms/API.lsl"
#include "SL/LSL/Library/JsonTools.lsl"
#include "SL/LSL/GPHUD/GPHUDHeader.lsl"

integer LINK_QB1=-1;
integer LINK_QB2=-1;
integer LINK_QB3=-1;
integer LINK_QB4=-1;
integer LINK_QB5=-1;
integer LINK_QB6=-1;
integer LINK_MESSAGES=-1;
integer LINK_RX=-1;
integer LINK_TX=-1;
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
integer ready=FALSE;
integer loggedin=FALSE;
string cookie="";
reflowHUD() {
	float sr=((float)sizeratio);
	llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<0.01,0.12*sr,0.12>,
		PRIM_LINK_TARGET,LINK_RX,PRIM_SIZE,<0.02,0.06*sr,0.02>,PRIM_POSITION,<0.01,0.06*sr/2.0,-0.06>,
		PRIM_LINK_TARGET,LINK_TX,PRIM_SIZE,<0.02,0.06*sr,0.02>,PRIM_POSITION,<0.01,-0.06*sr/2.0,-0.06>,
		PRIM_LINK_TARGET,LINK_QB2,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02,0.04>,
		PRIM_LINK_TARGET,LINK_QB1,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02+0.04,0.04>,
		PRIM_LINK_TARGET,LINK_QB4,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02,0.0>,
		PRIM_LINK_TARGET,LINK_QB3,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02+0.04,0.0>,
		PRIM_LINK_TARGET,LINK_QB6,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02,-0.04>,
		PRIM_LINK_TARGET,LINK_QB5,PRIM_POSITION,<0.01,0.12*sr/2.0+0.02+0.04,-0.04>,
		PRIM_LINK_TARGET,LINK_MESSAGES,PRIM_POSITION,<0.01,-0.12*sr/2.0-0.02,0.04>
		]);
}
brand() {llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,"36c48d34-3d84-7b9a-9979-cda80cf1d96f",<1,1,1>,<0,0,0>,0]);}
setupRpChannel() {
	if (rpchannelhandle!=0){llListenRemove(rpchannelhandle);}
	if (rpchannel!=0) { rpchannelhandle=llListen(rpchannel,"",llGetOwner(),""); }
}
setlamps() {
	if (LINK_RX>0 && LINK_TX>0) {
		llSetLinkPrimitiveParamsFast(LINK_RX,[PRIM_COLOR,ALL_SIDES,LAMP_RX_COL,1,PRIM_LINK_TARGET,LINK_TX,PRIM_COLOR,ALL_SIDES,LAMP_TX_COL,1]);
	}
}
string appendoutbound(){
	return llJsonSetValue(json,["cookie"],cookie); 
}
integer FIRSTREADY=TRUE;
comms_ready() {
	#ifdef DEBUG
	llOwnerSay("Comms_Ready");
	#endif
	if (!FIRSTREADY) { return; }
	#ifdef DEBUG
	llOwnerSay("Comms_Ready FIRST TIME");
	#endif
	FIRSTREADY=FALSE;
	banner_hud();
	calculatebroadcastchannel();
	startLogin();
	llListen(broadcastchannel,"",NULL_KEY,"");		
	llResetOtherScript("Legacy");
	llListen(1,"",llGetOwner(),"");
	llRegionSayTo(llGetOwner(),broadcastchannel,"{\"hudreplace\":\"hudreplace\"}");
	llRequestExperiencePermissions(llGetOwner(),"");
}
startLogin() {
	LOGIN_ATTEMPTS--;
	if (LOGIN_ATTEMPTS<=0) { llOwnerSay("FAILED REGISTRATION 10 TIMES.  Shutting down in case we are stuck in a loop!  Please contact support."); gphud_hang(); }
	json=llJsonSetValue("",["version"],VERSION);
	json=llJsonSetValue(json,["versiondate"],COMPILEDATE);
	json=llJsonSetValue(json,["versiontime"],COMPILETIME);	
	httpcommand("characters.login");
}
integer LOGIN_ATTEMPTS=10;
integer started=FALSE;

integer process(key id) {
	gphud_process();
	string incommand=jsonget("incommand");
	integer DONOTRESPOND=FALSE;
	string retjson="";
	//llOwnerSay("WE ARE HERE WITH "+json);
	if (jsonget("error")!="" && loggedin==FALSE) { // failed to login / create character?  so blind retry? :P
		llOwnerSay("Error during login/registration, retrying...");
		llSetText("Retry character registration...",<1,.5,.5>,1);
		startLogin();
		return TRUE;	
	}
	if (incommand=="radar") { DONOTRESPOND=TRUE; llSensor("",NULL_KEY,AGENT,20,PI); radarto=id; }
	if (incommand=="registered") { cookie=jsonget("cookie"); loggedin=TRUE; }
	if (incommand=="ping") { retjson=llJsonSetValue(retjson,["cookie"],cookie); }
	//if (incommand=="close" && !disallowclose) { hidehud(); }
	//if (incommand=="open") { showhud(); disallowclose=FALSE;}
	if (incommand=="runtemplate") { llMessageLinked(LINK_THIS,LINK_LEGACY_RUN,json,""); }
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
	if (jsonget("zoning")!="") { llMessageLinked(LINK_THIS,LINK_SET_ZONING,jsonget("zoning"),""); }
	if (jsonget("legacymenu")!="") { llMessageLinked(LINK_THIS,LINK_LEGACY_SET,jsonget("legacymenu"),""); }
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
	if (jsonget("hudreplace")!="") { llOwnerSay("Duplicate GPHUD attached, detaching one"); llDetachFromAvatar(); }
	if (jsonget("zone")!="") { llMessageLinked(LINK_THIS,LINK_RECEIVE,json,""); }
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
	//llMessageLinked(LINK_THIS,COMMS_SHUTDOWN,"",""); // not much point, just makes it spam a thing
	if (detach) { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerremove\":\"titlerremove\"}"); llSleep(2.0/45.0); llDetachFromAvatar(); }
	report("Has shutdown",<1,1,.5>);
}
report(string msg,vector col) {
	string inject=""; if (DEV) { inject="DEV "; }
	llSetText(msg+"\n \n[GPHUD "+inject+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"]",col,1);
}
integer stage=0;
string reboot_reason="Unknown";
integer countdown=0;
integer firstrx=TRUE;

default {
	state_entry() {
		llSetObjectName("GPHUD");
		key k=LOGO_COAGULATE;
		if (llGetObjectDesc()=="DEV") { 
			k=LOGO_COAGULATE_DEV;
		}
		llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,k,<1,1,1>,<0,0,0>,0]);
		setupRpChannel();
		llResetOtherScript("Legacy");
		llResetOtherScript("Comms");
		integer p=llGetNumberOfPrims();
		for (;p>0;p--)	{
			string primname=llToLower(llGetLinkName(p));
			if (primname=="quickbutton1") { LINK_QB1=p; }
			if (primname=="quickbutton2") { LINK_QB2=p; }
			if (primname=="quickbutton3") { LINK_QB3=p; }
			if (primname=="quickbutton4") { LINK_QB4=p; }
			if (primname=="quickbutton5") { LINK_QB5=p; }
			if (primname=="quickbutton6") { LINK_QB6=p; }
			if (primname=="rx") { LINK_RX=p; }
			if (primname=="tx") { LINK_TX=p; }
			if (primname=="getmessage") { LINK_MESSAGES=p; }
			
		}
		if (LINK_MESSAGES==-1 ||
			LINK_QB1==-1 ||
			LINK_QB2==-1 ||
			LINK_QB3==-1 ||
			LINK_QB4==-1 ||
			LINK_QB5==-1 ||
			LINK_QB6==-1 ||
			LINK_TX==-1 ||
			LINK_RX==-1) { llOwnerSay("PANIC! LINK NOT SET)");  while (1==1) { llSleep(100.0); } }
		if (llGetInventoryType("Attacher")==INVENTORY_SCRIPT) {
			llResetOtherScript("Attacher");
			report("Waiting for GO",<0.75,0.75,1.0>);
		} else { detach=TRUE; comms_start(); started=TRUE; }
	}
	link_message(integer from,integer num,string message,key id) {
        if (num==LINK_SEND) { json=message; message=""; httpcommand((string)id); }	
		if (num==LINK_GO) {
			#ifdef DEBUG
			llOwnerSay("Attacher GO");
			#endif
			started=TRUE;
			comms_start();
		}
		if (num==COMMS_GO) {
			#ifdef DEBUG
			llOwnerSay("COMMS GO");
			#endif			
			ready=TRUE;
			brand();
			comms_ready();
		}
		if (num==COMMS_STOP) { ready=FALSE;
			#ifdef DEBUG
			llOwnerSay("COMMS STOP");
			#endif		
		}
		if (num==COMMS_REQUEST || num==COMMS_RESPONSE) {
			json=message;
			if (process(id)) { llMessageLinked(LINK_THIS,COMMS_REQUEST_RESPONSE,json,id); }
		}
	}

	changed(integer change)
	{
		if ((change & (CHANGED_REGION | CHANGED_REGION_START )) && ready)
		{
			llResetScript();
		}
	}	
	listen(integer channel,string name,key id,string text) {
		if (!ready) { return; }
		if (channel==broadcastchannel) {
			if (text=="GOTHUD") {
				llRegionSayTo(id,broadcastchannel,"GOTHUD");
				return;
			}
			json=text;
			json=llJsonSetValue(json,["incommand"],"broadcast");
			process(NULL_KEY);
		}
		if (channel==1 && id==llGetOwner()) {
			if (text=="status" && id==IAIN_MALTZ) { llOwnerSay("HUD: "+(string)llGetFreeMemory()); llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"",""); return;}
			if (text=="reboot") { llResetScript(); }
			json=llJsonSetValue("",["console"],text);
			httpcommand("console");
		}
		if (channel==rpchannel) {
			string name=llGetObjectName(); llSetObjectName(charname); llSay(0,text); llSetObjectName(name);
		}
	}
	on_rez(integer parameter) { if (ready) { llResetScript(); } }
	touch_start(integer n)
	{
		if (!ready) { return; }
		if (!loggedin) { return; }
		if (llDetectedLinkNumber(0)==1) {
			llMessageLinked(LINK_THIS,LINK_LEGACY_FIRE,"","");
		} else {
			string name=llGetLinkName(llDetectedLinkNumber(0));
			if (name=="legacymenu") {
				llMessageLinked(LINK_THIS,LINK_LEGACY_FIRE,"","");
			} else {
				json="";
				httpcommand("gphudclient."+name);
			}
		}
	}
	experience_permissions(key id) {}
	experience_permissions_denied(key id,integer reason) { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	no_sensor() {
		if (radarto!=NULL_KEY) {
			llMessageLinked(LINK_THIS,COMMS_REQUEST_RESPONSE,"{\"avatars\":\"\"}",radarto);
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
			llMessageLinked(LINK_THIS,COMMS_REQUEST_RESPONSE,"{\"avatars\":\""+keys+"\"}",radarto);
			radarto=NULL_KEY;
		}
	}

	
}
