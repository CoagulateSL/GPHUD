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
key radarto=NULL_KEY;
integer donotopenbefore=0;
integer disallowclose=TRUE;
integer detach=FALSE;
string hudtext="Loading...";
vector hudcolor=<1,1,1>;
string titlertext="Loading...";
list ok=[];
vector titlercolor=<1,1,1>;
integer firstshown=FALSE;
string charname="";
integer rpchannel=0;
integer rpchannelhandle=0;
string cookie="";
setupRpChannel() {
	if (rpchannelhandle!=0){llListenRemove(rpchannelhandle);}
	if (rpchannel!=0) { rpchannelhandle=llListen(rpchannel,"",llGetOwner(),""); }
}
setlamps() {
	if (LINK_RX>0 && LINK_TX>0) {
		llSetLinkPrimitiveParamsFast(LINK_RX,[PRIM_COLOR,ALL_SIDES,LAMP_RX_COL,1,PRIM_LINK_TARGET,LINK_TX,PRIM_COLOR,ALL_SIDES,LAMP_TX_COL,1]);
	}
}
string appendoutbound(string j){
	return llJsonSetValue(j,["cookie"],cookie); 
}
subprocess(string incommand,key id) {
	if (incommand=="radar") { DONOTRESPOND=TRUE; llSensor("",NULL_KEY,AGENT,20,PI); radarto=id; }
	if (incommand=="registered") { cookie=jsonget("cookie"); }
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
halt() {
	LAMP_RX=-1; LAMP_TX=-1; updatelamps();
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
		llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,"e99682b7-d008-f0e0-9f08-e0a07d74232c",<1,1,1>,<0,0,0>,0]);
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
		if (llGetInventoryType("Attacher")==INVENTORY_SCRIPT) {
			llResetOtherScript("Attacher");
			report("Waiting for GO",<0.75,0.75,1.0>);
		} else { detach=TRUE; state preactive; }
	}
	on_rez(integer n) { if (n==0) { detach=FALSE; } else { detach=TRUE; } }
    link_message(integer from,integer num,string message,key id) {
        if (num==LINK_GO) {
            state preactive;
        }
	}
	
}
state preactive {
	state_entry() {
		initComms(TRUE);
		report("Creating inbound channel",<0.5,0.75,0.5>);
		LAMP_RX=1;
		updatelamps();
		stage=0;
		llSetTimerEvent(30.0);
	}
	http_request(key id, string method, string body) {
		//llOwnerSay("Request:"+body);
		if (method == URL_REQUEST_DENIED)
		{ reboot_reason="Error getting callback URL:" + body; state reboot; }
		if (method==URL_REQUEST_GRANTED) {
			LAMP_RX=1;
			comms_url=body;
			updatelamps();
			report("Logging in to server",<0.5,0.87,0.5>);
			state active;
		}		
		json=body;
		llHTTPResponse(id,200,"{}");
		if (SHUTDOWN) { halt(); }
		if (RESET) { state reboot; }			
	}
	http_response(key id,integer status,list data,string body) {
		//llOwnerSay("Response:"+body);
		if (httpkey!=id) { return; }
		if (status!=200) { LAMP_RX=-1; LAMP_TX=-1; updatelamps(); report("HTTP error "+(string)status,<1,.75,.75>); reboot_reason="Registration failed HTTP#"+(string)status; llSleep(3); state reboot; }
		json=body;
		if(jsonget("incommand")=="registered") {
			LAMP_TX=0;
			updatelamps();
			report("Login OK!",<0.75,1,0.75>);
			comms_http_response(id,status);
			state active;
		}
		if (SHUTDOWN) { halt(); }
		if (RESET) { state reboot; }			
	}
	timer() {
		if (SHUTDOWN) { halt(); }
		if (RESET) { state reboot; }	
		if (stage==0) { reboot_reason="URL Request Timeout"; state reboot; }
		if (stage==1) { reboot_reason="Server registration timeout"; state reboot; }
	}
}
state active {
	on_rez(integer n) { llResetScript(); }
	state_entry() {
		firstrx=TRUE;
		string status=llJsonSetValue("",["version"],VERSION);
		status=llJsonSetValue(status,["versiondate"],COMPILEDATE);
		status=llJsonSetValue(status,["versiontime"],COMPILETIME);	
		httpcommand("characters.login",status);
		llListen(broadcastchannel,"",NULL_KEY,"");		
		llResetOtherScript("Legacy");
		llListen(1,"",llGetOwner(),"");
		llRegionSayTo(llGetOwner(),broadcastchannel,"{\"hudreplace\":\"hudreplace\"}");
		llRequestExperiencePermissions(llGetOwner(),"");
	}
	experience_permissions_denied(key id,integer reason) { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	touch_start(integer n)
	{
		if (llDetectedLinkNumber(0)==1) {
			llMessageLinked(LINK_THIS,LINK_LEGACY_FIRE,"","");
		} else {
			string name=llGetLinkName(llDetectedLinkNumber(0));
			if (name=="legacymenu") {
				llMessageLinked(LINK_THIS,LINK_LEGACY_FIRE,"","");
			} else {
				httpcommand("gphudclient."+name,"");
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
		if (firstrx) { report("CONNECTED",<0.5,1,0.5>); } 
		//llOwnerSay("[resp] "+body);
		json=body;
		comms_http_response(id,status);
		if (SHUTDOWN) { halt(); }
		if (RESET) { state reboot; }
		if (firstrx) { firstrx=FALSE; if (!SHUTDOWN) { banner(); } }
	}
    http_request(key id, string method, string body)
    {
		//llOwnerSay("[in] "+body);
		json=body;
		comms_http_request(id,method);
		if (SHUTDOWN) { halt(); }
		if (RESET) { state reboot; }		
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
	}
	timer () {
		llSetTimerEvent(5.0);
		//LAMP_TX=0;
		LAMP_RX=0;
		updatelamps(); if (RESET) { state reboot; } if (SHUTDOWN) { halt(); }
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
	experience_permissions(key id) {}
}

state reboot {
	state_entry() {
		LAMP_RX=-1; LAMP_TX=-1; updatelamps();
		countdown=60;
		llSetTimerEvent(1.0);
	}
	timer() {
		llSetText(reboot_reason+", will reboot in "+(string)countdown+" seconds, please stand by... \n \n \n \n",<1,.5,.5>,1);
		countdown--;
		if (countdown<=0) { llResetScript(); }
	}
}

