#include "Constants.lsl"

// shared secret
string DEVELOPER_KEY="***REMOVED***";

// list of servers urls
list servers=[];
list servertimeout;

// developer mode
integer DEV=0;

// reason for shutdown
string comms_reason="";
string comms_url="";
integer comms_callback=FALSE;
integer comms_callbackalive=FALSE;
integer SHUTDOWN=FALSE;
integer RESET=FALSE;
string prefix="";
integer serveractive;
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

selectServer() {
    serveractive=-1;
    integer n=0;
    for (n=0;n<llGetListLength(servers);n++) {
        if (llList2Integer(servertimeout,n)<llGetUnixTime()) { serveractive=n; return; }
    }
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
	servers=[];
	DEV=0;
	string inject="";
	if (llGetObjectDesc()=="DEV")
	{ 
		llOwnerSay("Booting in DEV mode"); 
		DEV=1;
		inject="dev";
	}
	prefix="https://sl"+inject+".coagulate.net/GPHUD/hud/";
	servers=["https://sl1"+inject+".coagulate.net/GPHUD/system"];
	servers+=["https://sl2"+inject+".coagulate.net/GPHUD/system"];
	servers+=["https://sl3"+inject+".coagulate.net/GPHUD/system"];
	while (llGetListLength(servers)>llGetListLength(servertimeout)) { servertimeout+=[0]; }
	serveractive=(integer)llFrand(llGetListLength(servers));
	llListen(broadcastchannel,"",NULL_KEY,"");
	if (withcallback) { llRequestURL(); }
}

list txid;
list txmessage;
list txserver;
resetcomms() {
	LAMP_TX=-1; LAMP_RX=-1; updatelamps();
	if (comms_url!="") { llReleaseURL(comms_url); comms_url=""; }
	servers=[];
	DEV=0;
	comms_reason="";
	comms_callback=FALSE;
	comms_callbackalive=FALSE;
	SHUTDOWN=FALSE;
	prefix="";
	servertimeout=[];
	serveractive=0;
	LAMP_RX_COL=<0,0,0>;
	LAMP_TX_COL=<0,0,0>;
	retjson="";
	broadcastchannel=0;
	RESET=FALSE;
	txid=[];
	txmessage=[];
	txserver=[];
}
trigger() {

	if (serveractive<0) { selectServer(); }
    if (serveractive<0) {
		LAMP_TX=-1;
		LAMP_RX=-1;
		if (comms_url!="") { llReleaseURL(comms_url); comms_url=""; }
		updatelamps();
		integer seconds=60;
		RESET=TRUE; return;
	}
    integer i=0;
    for (i=0;i<llGetListLength(txid);i++) {
        if (llList2Key(txid,i)==NULL_KEY) {
			LAMP_TX=1; updatelamps();
			//llOwnerSay("Post to "+llList2String(servers,serveractive)+" - "+llList2String(txmessage,i));
            key httpkey=llHTTPRequest(
                        llList2String(servers,serveractive),
                        [HTTP_METHOD,"POST"],
                        llList2String(txmessage,i)
                    );
            txid=llListReplaceList(
                txid,
                [httpkey]
                ,i,i
            );
            txserver=llListReplaceList(txserver,[serveractive],i,i);
            if (httpkey==NULL_KEY) { llSetTimerEvent(15.0); return; }
        }
    }
}

httpsend(string message) {
	//llOwnerSay("Send message:"+message);
	LAMP_TX=1; updatelamps();
    txid+=[NULL_KEY];
	message=appendoutbound(message);
	if (comms_url!="") { message=llJsonSetValue(message,["callback"],comms_url); }
    txmessage+=[llJsonSetValue(message,["developerkey"],DEVELOPER_KEY)];
    txserver+=[serveractive];
    trigger();
}
httpcommand(string command,string json) {
    httpsend(llJsonSetValue(json,["command"],command));
}

comms_http_response(key id,integer status) {
	integer n=llListFindList(txid,[id]);
	if (n==-1) {
		return;
	}
	LAMP_TX=0; updatelamps();
	if (status!=200) {
		//llSay(0,"Server gave error "+(string)status+", activating failover");
		integer serverid=llList2Integer(txserver,n);
		servertimeout=llListReplaceList(servertimeout,[llGetUnixTime()+60],serverid,serverid);
		selectServer();
		txid=llListReplaceList(txid,[NULL_KEY],n,n);
		trigger();
	}
	else
	{
		process("RETURN",NULL_KEY);
		txid=llDeleteSubList(txid,n,n);
		txmessage=llDeleteSubList(txmessage,n,n);
		txserver=llDeleteSubList(txserver,n,n);            
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
			subregistered();// duplicate functionality?
			subprocess("registered",id);  // duplicate functionality?
			comms_callbackalive=TRUE;
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
		typedSay("---\nTERMINATE:"+comms_reason+"\n---");
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
