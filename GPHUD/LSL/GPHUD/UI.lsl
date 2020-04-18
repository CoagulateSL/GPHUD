#include "Constants.lsl"
#include "SL/LSL/Library/SetDev.lsl"
#include "SL/LSL/Library/JsonTools.lsl"

//#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
//#define COMMS_INCLUDEDIGEST
#define COMMS_DONT_CHECK_CALLBACK
#define COMMS_PROTOCOL "2"
#define COMMS_DEVKEY "***REMOVED***"
#include "SL/LSL/Library/CommsV3.lsl"

string dialogprefix="";
string mainmenu="";
integer channel=0;
string setname="";
list dialog=[];
integer handle=0;
integer MANUAL_NONE=0; integer MANUAL_AVAILABLE=1; integer MANUAL_SELECTED=2; 
integer sensormanual=MANUAL_NONE;
list zoning=[];
string ourzone="";
integer curpage=0;
string sensordescription="";
integer SHUTDOWN=FALSE;
rollchannel() {
	if (channel!=0) { llListenRemove(handle); }
	channel=(integer)(llFrand(-999999999)-1000000000);
	handle=llListen(channel,"",llGetOwner(),"");
}

page(integer p) {
	if (p<0) { p=0; }
	curpage=p;
	integer j=0;
	integer paged=0;
	if (p>0 || jsonget(dialogprefix+"12")!="") { paged=1; }
	//llOwnerSay("Paged is "+(string)paged);
	//llOwnerSay("Element12:"+jsonget(dialogprefix+"12"));
	if (paged==0) { 
		for (j=0;j<12;j++) {
			if (jsonget(dialogprefix+(string)j)!="") { dialog+=[llGetSubString(jsonget(dialogprefix+(string)j),0,23)]; }
		}
		return;
	}
	integer s=p*9; // starting element
	//llOwnerSay("Page "+(string)p+" element "+(string)s);
	if (p>0){dialog+=["<<<"];}else{dialog+=[" "];}
	dialog+=[" "];
	if (jsonget(dialogprefix+(string)(s+9))!="") { dialog+=[">>>"]; } else { dialog+=[" "]; }
	for (j=s;j<s+9;j++) {
		if (jsonget(dialogprefix+(string)j)!="") {
			dialog+=[llGetSubString(jsonget(dialogprefix+(string)j),0,23)];
		}
	}
}

trigger() {
	dialogprefix="";
	dialog=[];
	rollchannel();
	integer args=(integer) jsonget("args");
	integer iscomplete=TRUE;
	integer i;integer j;
	//llOwnerSay(json);
	//llOwnerSay("Args:"+(string)args);
	for (i=0;i<args;i++) {
		if (iscomplete) {
			string name=jsonget("arg"+(string)i+"name");
			string type=llJsonValueType(json,[name]);
			/*
			string valuetype="UNKNOWN";
			if (type==JSON_INVALID) { valuetype="INVALID"; }
			if (type==JSON_OBJECT) { valuetype="OBJECT"; }
			if (type==JSON_ARRAY) { valuetype="ARRAY"; }
			if (type==JSON_NUMBER) { valuetype="NUMBER"; }
			if (type==JSON_STRING) { valuetype="STRING"; }
			if (type==JSON_NULL) { valuetype="NULL"; }
			if (type==JSON_TRUE) { valuetype="TRUE"; }
			if (type==JSON_FALSE) { valuetype="FALSE"; }
			if (type==JSON_DELETE) { valuetype="DELETE"; }
			llOwnerSay(name+" "+valuetype);
			*/
			if (type==JSON_INVALID) {//if (jsonget(name)=="") { 
				iscomplete=FALSE;
				setname=name;
				string val="arg"+(string)i+"type";
				string type=jsonget(val);
				//llOwnerSay(type);
				integer parsed=FALSE;
				if (type=="") { llOwnerSay("INVOKE TARGET "+jsonget("invoke")+" arg "+(string)i+" name "+name+" has no type"); }
				string description=jsonget("arg"+(string)i+"description");
				if (type=="SELECT") { parsed=TRUE; 
					dialogprefix="arg"+(string)i+"button";
					page(curpage);
					llDialog(llGetOwner(),description,dialog,channel);
				}
				if (type=="TEXTBOX" || sensormanual==MANUAL_SELECTED) { parsed=TRUE;
					llTextBox(llGetOwner(),description,channel);
				}
				if ((type=="SENSORCHAR" || type=="SENSOR") && sensormanual!=MANUAL_SELECTED) { parsed=TRUE; 
					llSensor("",NULL_KEY,AGENT,20,PI);
					sensordescription=description;
					if (jsonget("arg"+(string)i+"manual")!="") { sensormanual=MANUAL_AVAILABLE; } else { sensormanual=MANUAL_NONE; }
					//llOwnerSay((string)sensormanual);
				}
				if (!parsed) { llOwnerSay("User interface failure, unknown type "+type); }
			}
		}
	}
	
	if (iscomplete) {
		httpcommand(jsonget("invoke"),"GPHUD/system");
		sensormanual=MANUAL_NONE;
	} 
}

calculateZone() {
	//llOwnerSay("Calling calculatezone");
	integer i=0;
	string favloc="";
	integer favvol=999999999;
	for (i=0;i<llGetListLength(zoning);i+=3) {
		string name=llList2String(zoning,i);
		//llOwnerSay(llList2String(zoning,i+1);
		vector min=(vector)llList2String(zoning,i+1);
		vector max=(vector)llList2String(zoning,i+2);
		vector us=llGetPos();
		// are we in here
		//llOwnerSay("Compare:"+name+" - "+(string)us+" between "+(string)min+" - "+(string)max);
		if (us.x>=min.x && us.x<max.x && us.y>=min.y && us.y<max.y && us.z>=min.z && us.z<max.z) {
			integer vol=(integer)((max.x-min.x)*(max.y-min.y)*(max.z-min.z));
			if (vol<favvol) { favvol=vol; favloc=name; }
		}
	}
	//llOwnerSay("Location:"+favloc);
	//llOwnerSay("ourzone:"+ourzone+" newloc:"+favloc);
	if (favloc!=ourzone) {
		string temp=json;
		json="{\"zone\":\""+favloc+"\"}";
		httpcommand("zoning.zonetransition","GPHUD/system");
		ourzone=favloc;
		json=temp;
	}
}
process() {
	//llOwnerSay("Json2:"+jsontwo);
	string incommand=jsontwoget("incommand");	
	if (incommand=="registered") { /*cookie=jsontwoget("cookie");*/ }
	if (jsontwoget("zoning")!="") { zoning=llParseStringKeepNulls(jsontwoget("zoning"),["|"],[]); calculateZone();}
	if (jsontwoget("legacymenu")!="") { mainmenu=jsontwoget("legacymenu"); }
	if (jsontwoget("url")!="") { comms_url=jsontwoget("url"); }
	if (jsontwoget("virtual-node")!="") { comms_node=((integer)jsontwoget("virtual-node")); }
	if (jsontwoget("zone")!="" && jsontwoget("zonemessage")!="") {
		if (jsontwoget("zone")==ourzone) { llOwnerSay(jsontwoget("zonemessage")); }
	}
	if (incommand=="runtemplate") { json=jsontwo; sensormanual=MANUAL_NONE; trigger(); }
	if (jsontwoget("logincommand")!="") {
		string logincommand=jsontwoget("logincommand");
		json="";
		json=llJsonSetValue(json,["invoke"],logincommand);
		json=llJsonSetValue(json,["args"],"0");
		sensormanual=MANUAL_NONE;
		trigger();
	}
}
mainMenu() {
	json=mainmenu;
	sensormanual=MANUAL_NONE;
	trigger();
}
default {
	state_entry() {
		llSetTimerEvent(2);
		setDev(FALSE);
	}
	timer() {
		if (SHUTDOWN) { llSetTimerEvent(0); return; }
		if (llGetListLength(zoning)>0) { calculateZone(); }
	}
    link_message(integer from,integer num,string message,key id) {
		if (SHUTDOWN) { return; }	
		if (num==LINK_SHUTDOWN) { SHUTDOWN=TRUE; }
		if (num==LINK_SET_STAGE) { BOOTSTAGE=((integer)message); }
		if (num==LINK_RECEIVE) {
			//llOwnerSay("Json2:"+message);
			//llOwnerSay("False:"+((string)id));
			jsontwo=message; message=""; process(); jsontwo="";
		}
		if (num==LINK_DIAGNOSTICS) { llOwnerSay("UI: "+(string)llGetFreeMemory()); }
	}
	http_response( key request_id, integer status, list metadata, string body ) {
		if (SHUTDOWN) { return; }	
		if (status==200) {
			jsontwo=body; body="";
			process();
			jsontwo="";
		}
	}	
	listen(integer rxchannel,string name,key id,string text) {
		if (SHUTDOWN) { return; }	
		if (id==llGetOwner() && channel==rxchannel) {
			string type="";
			integer i=0;
			integer args=(integer) jsonget("args");
			for (i=0;i<args;i++) {
				string name=jsonget("arg"+(string)i+"name");
				if (jsonget(name)=="") { 			
					if (type=="") { 
						string val="arg"+(string)i+"type";
						type=jsonget(val);			
					}
				}
			}
			if (type=="SELECT") {
				if (text==" ") { trigger(); return; }
				if (text==">>>") { curpage++; trigger(); return; }
				if (text=="<<<") { curpage--; trigger(); return; } 
				// if you make the first 24 chars ambiguous, expect dumb behaviour
				if (llStringLength(text)>=23) {
					i=0;
					while (jsonget(dialogprefix+(string)i)!="") {
						if (llSubStringIndex(jsonget(dialogprefix+(string)i),text)==0) { text=jsonget(dialogprefix+(string)i); }
						i++;
					}
				}
			}
			if (type=="SENSOR" || type=="SENSORCHAR" || type=="SELECT") {
				if (sensormanual==MANUAL_AVAILABLE && text=="ManualEntry") { sensormanual=MANUAL_SELECTED; trigger(); return; }
				//llOwnerSay("Qualify "+text);
				integer i=0;
				integer perfect=-1;
				integer prefix=-1;
				for (i=0;i<llGetListLength(dialog);i++) {
					if (text==llList2String(dialog,i)) { //llOwnerSay("Perfect match '"+text+"' == '"+llList2String(dialog,i)+"'");
						perfect=i;
					}
					if (llSubStringIndex(llList2String(dialog,i),text)==0) { //llOwnerSay("Prefix match '"+text+"' == '"+llList2String(dialog,i)+"'");
						if (prefix!=-1) { //llOwnerSay("Prefix multimatch");
							prefix=-2;
						} else {
							prefix=i;
						}
					}
				}
				if (prefix>=0) { text=llList2String(dialog,prefix); }
				if (perfect>=0) { text=llList2String(dialog,perfect); }
				//llOwnerSay("IN:"+text+":OUT:"+text);
				if (type=="SENSORCHAR" && sensormanual!=MANUAL_SELECTED) { text=">"+text; }
			}
			// SL bug
			if (
				( llGetSubString(llStringTrim(text,STRING_TRIM),0,0)=="{" &&
				  llGetSubString(llStringTrim(text,STRING_TRIM),-1,-1)=="}"
				) ||
				( llGetSubString(llStringTrim(text,STRING_TRIM),0,0)=="[" &&
				  llGetSubString(llStringTrim(text,STRING_TRIM),-1,-1)=="]"
				)
			) {
				// llJsonSetValue does not properly encode strings wrapped in { } characters.  see SEC-6308.  until resolved, we block such inputs here
				llOwnerSay("Illegal input ; due to a bug in Second Life you can not surround a string with { and } or [ and ] characters.  Please alter your input and try again.");
			} else { 
				json=llJsonSetValue(json,[setname],text); curpage=0;
			}
			trigger();
		}
	}
	no_sensor() {
		if (SHUTDOWN) { return; }
		if (sensormanual==MANUAL_AVAILABLE) { sensormanual=MANUAL_SELECTED; trigger(); return; }
		llOwnerSay("Unable to detect any nearby players!");
	}
	sensor(integer n) {
		if (SHUTDOWN) { return; }
		list stride=[];
		integer i=0;
		for (i=0;i<n;i++) {
			//llOwnerSay(llDetectedName(i)+" dist "+(string)llVecDist(llGetPos(),llDetectedPos(i)));
			stride+=(float)llVecDist(llGetPos(),llDetectedPos(i));
			stride+=llDetectedName(i);
		}
		//for (i=0;i<15;i++) { stride+=[(float)(10+i),"Avatar"+(string)i]; } // test the fill
		stride=llListSort(stride,2,TRUE);
		//for (i=0;i<llGetListLength(stride);i+=2) { llOwnerSay(llList2String(stride,i+1)+" dist "+(string)llList2Float(stride,i)); }
		integer qty=12;
		//llOwnerSay((string)sensormanual);
		list truncated=[];
		if (sensormanual==MANUAL_AVAILABLE) { qty=11; }
		if (sensormanual==MANUAL_AVAILABLE) { dialog+=["ManualEntry"]; truncated+=["ManualEntry"]; }
		if (qty>(llGetListLength(stride)/2)) { qty=(llGetListLength(stride)/2); }
		for (i=0;i<(qty*2); i+=2) {
			//llOwnerSay("Adding "+llList2String(stride,i+1));
			dialog+=llList2String(stride,i+1);
			truncated+=llGetSubString(llList2String(stride,i+1),0,23);
		}
		string description=jsonget("arg"+(string)i+"description");
		llDialog(llGetOwner(),sensordescription,truncated,channel);
	}
	touch_start(integer n)
	{
		if (SHUTDOWN) { return; }
		if (BOOTSTAGE<BOOT_COMPLETE) { return; }
		if (llDetectedLinkNumber(0)==1) {
			mainMenu();
		} else {
			string name=llGetLinkName(llDetectedLinkNumber(0));
			if (name=="legacymenu") {
				mainMenu();
			}
		}
	}
}