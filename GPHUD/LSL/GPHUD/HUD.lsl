#include "CommsHeader.lsl"
integer LINK_WEBPANEL=-1;
integer LINK_WEBINTRO=-1;
integer LINK_RX=-1;
integer LINK_TX=-1;
integer LINK_QB1=-1;
integer LINK_QB2=-1;
integer LINK_QB3=-1;
integer LINK_QB4=-1;
integer LINK_QB5=-1;
integer LINK_QB6=-1;
integer LINK_MESSAGES=-1;
string cookie="";
key radarto=NULL_KEY;
integer donotopenbefore=0;
integer disallowclose=TRUE;
integer alive=0;
integer resetin=-1;
integer detach=FALSE;
string hudtext="Loading...";
vector hudcolor=<1,1,1>;
string titlertext="Loading...";
vector titlercolor=<1,1,1>;
integer firstshown=FALSE;
string charname="";
integer rpchannel=0;
integer rpchannelhandle=0;
setupRpChannel() {
	if (rpchannelhandle!=0){llListenRemove(rpchannelhandle);}
	if (rpchannel!=0) { rpchannelhandle=llListen(rpchannel,"",llGetOwner(),""); }
}
setlamps() {
	if (LINK_RX>0 && LINK_TX>0) {
		llSetLinkPrimitiveParamsFast(LINK_RX,[PRIM_COLOR,ALL_SIDES,LAMP_RX_COL,1,PRIM_LINK_TARGET,LINK_TX,PRIM_COLOR,ALL_SIDES,LAMP_TX_COL,1]);
	}
}
/*media(string urlsuffix) {
		if (prefix=="") { llOwnerSay("PANIC, NO PREFIX SET FOR SERVICES!"); return; }
		string newurl=prefix;
		if (shown==FALSE) { newurl+="show/"; } 
		newurl+=urlsuffix;
		preshowhud();	
		llSetLinkMedia(LINK_WEBPANEL,4,[PRIM_MEDIA_AUTO_PLAY,TRUE,PRIM_MEDIA_FIRST_CLICK_INTERACT,TRUE,PRIM_MEDIA_PERMS_INTERACT,PRIM_MEDIA_PERM_OWNER,PRIM_MEDIA_PERMS_CONTROL,PRIM_MEDIA_PERM_NONE,PRIM_MEDIA_CURRENT_URL,newurl,PRIM_MEDIA_HOME_URL,newurl]);

}*/
/*integer shown=FALSE;
firstshow() {
	if (donotopenbefore>llGetUnixTime()) { return; }
	showhud();
	shown=TRUE;
	firstshown=TRUE;
	disallowclose=TRUE;
	llSetLinkPrimitiveParamsFast(LINK_WEBINTRO,[PRIM_POSITION,<0.00, 0.00, 0.20>,PRIM_LINK_TARGET,LINK_WEBPANEL,PRIM_POSITION,<0,0,0.11>,PRIM_SIZE,<0.01,0.2,0.1>]);
}
preshowhud() {
	if (firstshown==FALSE) { firstshow(); return; }
	if (donotopenbefore>llGetUnixTime()) { return; }
	llSetLinkPrimitiveParamsFast(LINK_WEBPANEL,[PRIM_POSITION,<0.00, 0.00, 0.19>,PRIM_SIZE,<0.01, 0.51, 0.26>,PRIM_ROTATION,llEuler2Rot(<0,0,180>*DEG_TO_RAD)]);
	shown=FALSE;
}
showhud() {
	if (donotopenbefore>llGetUnixTime()) { return; }
	llSetLinkPrimitiveParamsFast(LINK_WEBPANEL,[PRIM_POSITION,<0.00, 0.00, 0.19>,PRIM_SIZE,<0.01, 0.51, 0.26>,PRIM_ROTATION,ZERO_ROTATION,PRIM_LINK_TARGET,LINK_WEBINTRO,PRIM_POSITION,<0.00, 0.00,-0.46>]);
	shown=TRUE;
}
hidehud() {
	donotopenbefore=llGetUnixTime()+3;
	string closedurl="data:text/plain,closed - "+((string)llFrand(999999999));
	llSetLinkPrimitiveParamsFast(LINK_WEBPANEL,[PRIM_POSITION,<0,0,-2>,PRIM_ROTATION,ZERO_ROTATION,PRIM_LINK_TARGET,LINK_WEBINTRO,PRIM_POSITION,<0.00, 0.00,-0.46>]);
	llSetLinkMedia(LINK_WEBPANEL,4,[PRIM_MEDIA_AUTO_PLAY,TRUE,PRIM_MEDIA_FIRST_CLICK_INTERACT,TRUE,PRIM_MEDIA_PERMS_INTERACT,PRIM_MEDIA_PERM_OWNER,PRIM_MEDIA_PERMS_CONTROL,PRIM_MEDIA_PERM_NONE,PRIM_MEDIA_CURRENT_URL,closedurl,PRIM_MEDIA_HOME_URL,closedurl]);
	shown=FALSE;
}
hidepreshow() { llSetLinkPrimitiveParamsFast(LINK_WEBINTRO,[PRIM_POSITION,<0.00, 0.00,-0.46>]); }
*/
string appendoutbound(string j){
	return llJsonSetValue(j,["cookie"],cookie); 
}
subprocess(string incommand,key id) {
	if (incommand=="radar") { DONOTRESPOND=TRUE; llSensor("",NULL_KEY,AGENT,20,PI); radarto=id; }
	if (incommand=="ping") { retjson=llJsonSetValue(retjson,["cookie"],cookie); }
	//if (incommand=="close" && !disallowclose) { hidehud(); }
	//if (incommand=="open") { showhud(); disallowclose=FALSE;}
	if (incommand=="runtemplate") { llMessageLinked(LINK_THIS,LINK_LEGACY_RUN,json,""); }
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
	if (jsonget("titlercolor")!="") { titlercolor=(vector)jsonget("titlercolor"); }	
	if (jsonget("titlertext")!="") { titlertext=jsonget("titlertext"); }
	if (jsonget("titlertext")!="" || jsonget("titlercolor")!="") { 
		llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titler\":\""+(string)titlercolor+"|"+titlertext+"\"}");
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
}
subregistered() {
	cookie=jsonget("cookie");
	//media("gphudclient.close?gphud="+cookie);
	//firstshow();
	string name=llGetObjectName();
	banner();
	llSetObjectName(name);
	alive=1;
}
reboot() {
	LAMP_RX=-1; LAMP_TX=-1; updatelamps();
	alive=0;
	resetin=60/5;
}
halt() {
	LAMP_RX=-1; LAMP_TX=-1; updatelamps();
	alive=-1;
	if (detach) { llDetachFromAvatar(); }
}
default {
	state_entry() {
		setupRpChannel();
		llResetOtherScript("Legacy");
		integer p=llGetNumberOfPrims();
		for (;p>0;p--)	{
			string primname=llToLower(llGetLinkName(p));
			if (primname=="firsttime") { LINK_WEBINTRO=p; }
			if (primname=="webpanel") { LINK_WEBPANEL=p; }
			if (primname=="tx") { LINK_TX=p; }
			if (primname=="rx") { LINK_RX=p; }			
			if (primname=="quickbutton1") { LINK_QB1=p; }
			if (primname=="quickbutton2") { LINK_QB2=p; }
			if (primname=="quickbutton3") { LINK_QB3=p; }
			if (primname=="quickbutton4") { LINK_QB4=p; }
			if (primname=="quickbutton5") { LINK_QB5=p; }
			if (primname=="quickbutton6") { LINK_QB6=p; }
			if (primname=="getmessage") { LINK_MESSAGES=p; }
			
		}
		if (LINK_MESSAGES==-1 || LINK_WEBINTRO==-1 || LINK_WEBPANEL==-1 || LINK_RX==-1 || LINK_TX==-1) { llOwnerSay("PANIC! LINK NOT SET)");  while (1==1) { llSleep(100.0); } }
		//hidehud();
		//hidepreshow();		
		if (llGetInventoryType("Attacher")==INVENTORY_SCRIPT) {
			llResetOtherScript("Attacher");
			llSetText("GPHUD Startup ... waiting for GO event ... [v"+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"]",<0.75,0.75,1.0>,1);
			llSetObjectName("GPHUD");
		} else { state active; }
	}
	on_rez(integer n) { if (n==0) { detach=FALSE; } else { detach=TRUE; } }
    link_message(integer from,integer num,string message,key id) {
        if (num==LINK_GO) {
            state active;
        }
	}
	
}

state active {
	on_rez(integer n) { llResetScript(); }
	state_entry() {
	llResetOtherScript("Legacy");
		initComms(TRUE);
		llListen(1,"",llGetOwner(),"");
		llRegionSayTo(llGetOwner(),broadcastchannel,"{\"hudreplace\":\"hudreplace\"}");
		llRequestExperiencePermissions(llGetOwner(),"");
	}
	experience_permissions_denied(key id,integer reason) { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	touch_start(integer n)
	{
		if (alive==0) { return; }
		if (alive==-1) { llResetScript(); }
		if (comms_callbackalive) { 
			if (llDetectedLinkNumber(0)==1) {
				llMessageLinked(LINK_THIS,LINK_LEGACY_FIRE,"","");
			} else {
				string name=llGetLinkName(llDetectedLinkNumber(0));
				/*
				if (name=="webmenu") { 
					shown=FALSE;
					media("gphudclient/menu?gphud="+cookie+"?seed="+((string)llFrand(999999999))); // yeah the double ? is kinda invalid or weird :P
					return;
				}
				*/
				if (name=="legacymenu") {
					llMessageLinked(LINK_THIS,LINK_LEGACY_FIRE,"","");
				} else {
					httpcommand("gphudclient."+name,"");
				}
			}
		}
	}
    changed(integer change)
    {
        if (change & (CHANGED_INVENTORY|CHANGED_REGION | CHANGED_REGION_START ))
        {
            llResetScript();
        }
    }	
    http_response(key id,integer status,list data,string body)
    {
		//llOwnerSay("[resp] "+body);
		json=body;
		comms_http_response(id,status);
		if (SHUTDOWN) { halt(); }
		if (RESET) { reboot(); }
	}
    http_request(key id, string method, string body)
    {
		//llOwnerSay("[in] "+body);
		json=body;
		comms_http_request(id,method);
		if (method==URL_REQUEST_GRANTED) {
			string status=llJsonSetValue("",["version"],VERSION);
			status=llJsonSetValue(status,["versiondate"],COMPILEDATE);
			status=llJsonSetValue(status,["versiontime"],COMPILETIME);
			httpcommand("characters.login",status);
		}
		if (SHUTDOWN) { halt(); }
		if (RESET) { reboot(); }		
    }
	
	listen(integer channel,string name,key id,string text) {
		if (channel==broadcastchannel) {
			if (text=="GOTHUD") {
				llRegionSayTo(id,broadcastchannel,"GOTHUD");
				return;
			}
			json=text;
			process("broadcast",NULL_KEY);
		}
		if (channel==1 && id==llGetOwner()) {
			if (text=="status" && id==IAIN_MALTZ) { llOwnerSay("HUD: "+(string)llGetFreeMemory()); llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"",""); return;}
			if (text=="reboot") { llResetScript(); }
			if (alive!=1) { return; }
			httpcommand("console",llJsonSetValue("",["console"],text));
		}
		if (channel==rpchannel) {
			string name=llGetObjectName(); llSetObjectName(charname); llSay(0,text); llSetObjectName(name);
		}
	}
    link_message(integer from,integer num,string message,key id) {
        if (num==LINK_SEND) {
            httpcommand((string)id,message); 
        }
		if (alive!=1) { return; }
	}
	timer () {
		if (resetin>0) { resetin--; if (resetin==0) { llResetScript(); } llSetText(comms_reason+" : Reset in "+((string)(resetin*5)),<1,.5,.5>,1); }
		llSetTimerEvent(5.0);
		//LAMP_TX=0;
		if (comms_callbackalive) { LAMP_RX=0; } else { LAMP_RX=-1; }
		updatelamps(); trigger(); if (RESET) { reboot(); } if (SHUTDOWN) { halt(); }
	}
	no_sensor() { if (radarto!=NULL_KEY) { llHTTPResponse(radarto,200,"{\"avatars\":\"\"}"); radarto=NULL_KEY; } }
	sensor(integer n) {
		integer i=0;
		string keys="";
		for (i=0;i<n;i++) {
			if (keys!="") { keys+=","; }
			keys+=(string)llDetectedKey(i);
		}
		if (radarto!=NULL_KEY) { llHTTPResponse(radarto,200,"{\"avatars\":\""+keys+"\"}"); radarto=NULL_KEY; }
	}
}
