#include "Constants.lsl"

// shared secret
string DEVELOPER_KEY="***REMOVED***";

// developer mode
integer DEV=0;

// reason for shutdown
key httpkey=NULL_KEY;
string comms_reason="";
string comms_url="";
integer comms_callback=FALSE;
integer SHUTDOWN=FALSE;
integer RESET=FALSE;
string SERVER_URL="";
integer serveractive=-1;
integer LAMP_RX=-1;
integer LAMP_TX=-1;
vector LAMP_RX_COL=<0,0,0>;
vector LAMP_TX_COL=<0,0,0>;
string retjson="";
integer broadcastchannel=0;
integer message_is_say=FALSE;
typedSay(string s) {
	if (message_is_say) { llSay(0,s); } else { llOwnerSay(s); }
}
updatelamps() {
	LAMP_RX_COL=<0,0,0>;
	LAMP_TX_COL=<0,0,0>;
	if (LAMP_RX==0) { LAMP_RX_COL=<0,0.5,0>; }
	if (LAMP_RX==1) { LAMP_RX_COL=<0,1,0>; }
	if (LAMP_TX==0) { LAMP_TX_COL=<0.5,0,0>; }
	if (LAMP_TX==1) { LAMP_TX_COL=<1,0,0>; }
	setlamps();
}

calculatebroadcastchannel() {
	vector loc=llGetRegionCorner();
	integer x=(integer)loc.x;
	integer y=(integer)loc.y;
	x=x/256;
	y=y/256;
	x=x&0xffff;
	y=y&0x0fff;
	integer output=(y*0x10000)+x;
	output=output^***REMOVED***;
	output=-llAbs(output);
	if (output>-1000) { output=output-99999; }
	broadcastchannel=output;
}
initComms(integer withcallback) {
	resetcomms();
	calculatebroadcastchannel();
	DEV=0;
	string inject="";
	if (llGetObjectDesc()=="DEV")
	{ 
		DEV=1;
		inject="dev";
	}
	SERVER_URL="http://sl"+inject+".coagulate.net/GPHUD/system";
	llListen(broadcastchannel,"",NULL_KEY,"");
	if (withcallback) { llRequestURL(); }
}

resetcomms() {
	LAMP_TX=-1; LAMP_RX=-1; updatelamps();
	if (comms_url!="") { llReleaseURL(comms_url); comms_url=""; }
	DEV=0;
	comms_reason="";
	comms_callback=FALSE;
	SHUTDOWN=FALSE;
	LAMP_RX_COL=<0,0,0>;
	LAMP_TX_COL=<0,0,0>;
	retjson="";
	broadcastchannel=0;
	RESET=FALSE;
}

httpsend(string message) {
	//llOwnerSay("Send message:"+message);
	LAMP_TX=1; updatelamps();
	message=appendoutbound(message);
	if (comms_url!="") { message=llJsonSetValue(message,["callback"],comms_url); }
    message=llJsonSetValue(message,["developerkey"],DEVELOPER_KEY);
	httpkey=llHTTPRequest(
                        SERVER_URL,
                        [HTTP_METHOD,"POST"],
                        message
                    );
}
httpcommand(string command,string json) {
    httpsend(llJsonSetValue(json,["command"],command));
}

comms_http_response(key id,integer status) {
	if (id!=httpkey) { return; }
	LAMP_TX=0; updatelamps();
	if (status!=200) {
		llSay(0,"Server gave error "+(string)status);
	}
	else
	{
		process("RETURN",NULL_KEY);
	}
}
integer DONOTRESPOND=FALSE;
comms_http_request(key id,string method) {
	retjson="";
	if (method=="POST") { LAMP_RX=1; updatelamps(); }
	DONOTRESPOND=FALSE;
	process(method,id);
	if (method=="POST" && DONOTRESPOND==FALSE) {
		llHTTPResponse(id,200,retjson);
	}
	llSetTimerEvent(0.1);
}
process(string method,key id) {
//llOwnerSay(json);
	if (method == URL_REQUEST_DENIED)
	{ comms_reason="Error getting callback URL:" + json; SHUTDOWN=TRUE; return; }
	if (method == URL_REQUEST_GRANTED)
	{
		if (comms_url!="") { llReleaseURL(comms_url); }
		comms_url = json;
		return;
	}
	//llOwnerSay("request:"+json);
	string incommand=jsonget("incommand");
	//llOwnerSay("INCMD:"+incommand);
	//if (incommand=="registering") {
	//	if (DEV) { llOwnerSay("Outbound registration complete."); }
	//}
	if (incommand=="registered") {
		// allow reregistration!
		//if (comms_callbackalive==FALSE) {
			//if (DEV) { llOwnerSay("Inbound registration complete."); }
			subprocess("registered",id);  // duplicate functionality?
		//}
		//else
		//{
//			llOwnerSay("Received duplicate registration response???");
		//}
		return;
	}
	if (incommand=="registrationdenied") { comms_reason="Registration denied by server"; SHUTDOWN=TRUE; return; }
	integer process=1;
	if (jsonget("message")!="") {
		typedSay(jsonget("message"));
	}
	if (jsonget("say")!="") {
		string oldname=llGetObjectName();
		string newname=jsonget("sayas");
		if (newname!="") { llSetObjectName(newname); }
		llSay(0,jsonget("say"));
		if (newname!="") { llSetObjectName(oldname); }
	}
	if (jsonget("error")!="") {
		typedSay("Server reported error with request:\n"+jsonget("error"));
		process=0;
	}
	if (jsonget("terminate")!="") {
		comms_reason=jsonget("terminate");
		typedSay("===TERMINATED===    "+comms_reason);
		SHUTDOWN=TRUE; process=0;
	}			
	if (incommand=="shutdown" || jsonget("shutdown")!="") {
		typedSay("---\nSHUTDOWN REQUESTED\n---");
		comms_reason=jsonget("shutdown"); process=0;
		SHUTDOWN=TRUE;
	}	
	if (incommand=="reboot" || jsonget("reboot")!="") {
		llOwnerSay("Rebooting at request from server: "+jsonget("reboot")); process=0;
		llResetScript();
	}
	if (process!=1) {
		return;
	}
	if (incommand=="ping") {
		retjson=llJsonSetValue("",["incommand"],"pong");
		retjson=llJsonSetValue(retjson,["callback"],comms_url);
	}
	subprocess(incommand,id);

}
