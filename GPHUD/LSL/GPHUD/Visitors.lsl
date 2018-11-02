#include "Constants.lsl"
integer interval=60;

initialise()
{
	list avkeys=llGetAgentList(AGENT_LIST_REGION,[]);
	string pass="";
	pass+="{\"command\":\"gphudserver.syncavatars\",\"userlist\":\"";
	integer i=0;
	for (i=0;i<llGetListLength(avkeys);i++) {
		pass=pass+(string)llList2Key(avkeys,i)+"="+llKey2Name(llList2String(avkeys,i))+",";
	}	
	pass+="\"}";
	llMessageLinked(LINK_THIS,LINK_SET_USER_LIST,pass,NULL_KEY);
}

default {
	state_entry() {llMessageLinked(LINK_THIS,LINK_CAN_GO,"",""); }
	link_message (integer from,integer num,string message,key id) {
		if (num==LINK_GO) { state run; }
	}
}


state run {

	state_entry() {
		llOwnerSay("GPHUD Visitor module initialising, syncing sim state with server");
		initialise();
		llSetTimerEvent(interval);
	}
	timer () {
		initialise();
	}
    link_message(integer from,integer num,string message,key id) {
		if (num==LINK_STOP) { llOwnerSay("GPHUD Visitor tracking module shut down");llResetScript();}
		if (num==LINK_DIAGNOSTICS) { llSay(0,"Visitor free memory: "+(string)llGetFreeMemory()); }
	}	
}
