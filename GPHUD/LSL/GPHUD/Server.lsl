string name="GPHUD Server";
integer autoattach=FALSE;
integer parcelonly=TRUE;
integer countdown=60;
setlamps() {
//	llOwnerSay("RX:"+(string)LAMP_RX+" TX:"+(string)LAMP_TX);
	llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_COLOR,5,LAMP_RX_COL,1,PRIM_COLOR,2,LAMP_TX_COL,1]);
}
mark(integer n) {
	key texture="24eb95bf-0d9c-da12-ffa7-11b0b697b4f5";
	if (n==0) { texture="c498041c-1885-37d0-03d5-296ccb96b422"; }
	if (n==1) { texture="de2e74d4-9ccd-6d83-ea03-1c95c2ef57ca"; }
	llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,4,texture,<1,1,1>,<0,0,0>,PI_BY_TWO]);
}
setlogo(key k) { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,1,k,<1,1,1>,<0,0,0>,0]); }
#include "CommsHeader.lsl"
subprocess(string command,key id) {
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
	if (jsonget("instancestatus")!="") { llSetText(jsonget("instancestatus")+" "+(string)llGetFreeMemory()+"#"+((string)(llGetListLength(txid)))+"\n \n \n \n \n",(vector)jsonget("statuscolor"),1); }
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
integer alive=FALSE;
subregistered() {
	llOwnerSay("=== Server startup complete");
	alive=TRUE; mark(1);
	llMessageLinked(LINK_THIS,LINK_GO,"","");
}


string appendoutbound(string message) {return message;}
default {
	on_rez(integer n) { llResetScript(); }
	state_entry() {
		mark(-1);
		setlogo("e99682b7-d008-f0e0-9f08-e0a07d74232c");
		llResetOtherScript("Dispenser");
		llResetOtherScript("Visitors");
		llSetText("INITIALISING\n \n"+"GPHUD Server "+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"\n \n \n \n",<1.00,1.0,0.5>,1.0);	
		llSetObjectName(name);
		banner();
		initComms(TRUE);
		message_is_say=TRUE;
		llListen(0,"",NULL_KEY,"");
	}
	timer () {
		llSetTimerEvent(5.0);
		//LAMP_TX=0;
		if (comms_callbackalive) { LAMP_RX=0; } else { LAMP_RX=-1; }
		updatelamps(); trigger(); if (RESET) { state rebootscript; }
	}
    changed(integer change)
    {
        if (change & (CHANGED_OWNER | CHANGED_REGION | CHANGED_REGION_START | CHANGED_TELEPORT))
        {
            llResetScript();
        }
    }	
		
    http_response(key id,integer status,list data,string body)
    {
		//llOwnerSay(((string)status)+" - "+body);
		json=body;
		comms_http_response(id,status);
		if (SHUTDOWN) { state stop; }
		if (RESET) { state rebootscript; }
	}
    http_request(key id, string method, string body)
    {
		json=body;
		comms_http_request(id,method);
		if (method==URL_REQUEST_GRANTED) {
			string status=llJsonSetValue("",["version"],VERSION);
			status=llJsonSetValue(status,["versiondate"],COMPILEDATE);
			status=llJsonSetValue(status,["versiontime"],COMPILETIME);
			httpcommand("gphudserver.register",status);
		}		
		if (SHUTDOWN) { state stop; }
		if (RESET) { state rebootscript; }
    }	
	listen(integer channel,string name,key id,string text) {
		if (channel==broadcastchannel) {}
		if (channel==0) {
			if (llSubStringIndex(text,"*")==0) {
				if ((id==IAIN_MALTZ || id==llGetOwner()) && text=="*reboot") { llResetScript(); }
				if ((id==IAIN_MALTZ) && text=="*repackage") { state distribution; }
				if ((id==IAIN_MALTZ || id==llGetOwner()) && text=="*status") { llSay(0,"Server module "+VERSION+" "+COMPILEDATE+" "+COMPILETIME);llSay(0,"Server free memory: "+(string)llGetFreeMemory()); llMessageLinked(LINK_THIS,LINK_DIAGNOSTICS,"",""); return; }
				//if (!alive) { return; }
				llSay(0,"Sending command: "+text);
				string message=llJsonSetValue("",["runasavatar"],name);
				message=llJsonSetValue(message,["runaskey"],id);
				message=llJsonSetValue(message,["runasnocharacter"],"set");
				message=llJsonSetValue(message,["console"],text);
				httpcommand("console",message);
			}
		}
	}
    link_message(integer from,integer num,string message,key id) {
		if (!alive) { return ; }
		if (num==LINK_SET_USER_LIST) { httpcommand("gphudserver.setregionavatars",message); }
		if (num==LINK_GET_DISPENSER_CONFIG) { llMessageLinked(LINK_THIS,LINK_DISPENSER_CONFIG,(string)autoattach,(string)parcelonly); }
		if (num==LINK_CAN_GO) { llMessageLinked(LINK_THIS,LINK_GO,"",""); }
	}
	touch_start(integer n) {
		for (n--;n>=0;n--) {
			llMessageLinked(LINK_THIS,LINK_DISPENSE,"",llDetectedKey(n));
		}
	}
}

state stop {
	state_entry() {
		LAMP_TX=-1; LAMP_RX=-1; updatelamps(); mark(0);
		llMessageLinked(LINK_THIS,LINK_LEGACY_PACKAGE,"","");
		llSetText("SHUTDOWN:"+comms_reason+"\n \n \n \n",<1,.5,.5>,1);
	}
	touch_start(integer n) { if (llDetectedKey(0)==llGetOwner()) { llSetText("REBOOT",<1,.5,.5>,1); llSleep(1.0); llSetText("",<0,0,0>,0); llResetScript(); } }
}

state distribution {
	state_entry() { llSetObjectName("GPHUD Server "+VERSION+" "+COMPILEDATE+" "+COMPILETIME); LAMP_TX=-1; LAMP_RX=-1; updatelamps(); llSetText("Packaged mode, sleeping until next rez\n \n"+"GPHUD Server "+VERSION+" "+COMPILEDATE+" "+COMPILETIME+"\n \n \n \n",<0.5,0.5,1.0>,1.0); llResetOtherScript("Visitors"); llResetOtherScript("Dispenser"); mark(-1); setlogo("e99682b7-d008-f0e0-9f08-e0a07d74232c"); }
	on_rez(integer n) { llResetScript(); }
}
state rebootscript {
	state_entry() {
		LAMP_RX=-1; LAMP_TX=-1; updatelamps(); mark(0);
		alive=FALSE; 
		countdown=60;
		llSay(0,"Connection to server lost, restarting...");
		llSetTimerEvent(1.0);
	}
	timer() {
		llSetText("No servers available, will reboot in "+(string)countdown+" seconds, please stand by... \n \n \n \n",<1,.5,.5>,1);
		countdown--;
		if (countdown<=0) { llResetScript(); }
	}
}
