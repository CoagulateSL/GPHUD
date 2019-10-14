#ifndef _INCLUDE_GPHUD_HEADER
#define _INCLUDE_GPHUD_HEADER
#include "SL/LSL/GPHUD/Constants.lsl"

// reason for shutdown
integer broadcastchannel=0;
integer message_is_say=FALSE;
typedSay(string s) {
	if (message_is_say) { llSay(0,s); } else { llOwnerSay(s); }
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


integer gphud_process() {
	string incommand=jsonget("incommand");
	if (jsonget("message")!="") { typedSay(jsonget("message")); return TRUE; }
	if (jsonget("say")!="") {
		string oldname=llGetObjectName();
		string newname=jsonget("sayas");
		if (newname!="") { llSetObjectName(newname); }
		llSay(0,jsonget("say"));
		if (newname!="") { llSetObjectName(oldname); }
		return TRUE;
	}
	if (jsonget("error")!="") {
		typedSay(jsonget("error"));
		return TRUE;
	}
	if (jsonget("terminate")!="") {
		typedSay("===TERMINATED===\n"+jsonget("terminate"));
		gphud_hang();
	}			
	if (incommand=="shutdown" || jsonget("shutdown")!="") {
		typedSay("---SHUTDOWN REQUESTED---\n"+jsonget("shutdown"));
		gphud_hang();
	}	
	if (incommand=="reboot" || jsonget("reboot")!="") {
		llOwnerSay("Rebooting at request from server: "+jsonget("reboot"));
		llResetScript();
	}
	if (incommand=="ping") {
		//json=llJsonSetValue("",["incommand"],"pong");
		return TRUE;
	}
	return FALSE;
}
#endif
