#include "GPHUDHeader.lsl"
#include "../Library/JsonTools.lsl"
setlamps() {}
detach() {
	llSetLinkPrimitiveParamsFast(LINK_SET,[PRIM_TEXT,"",<0,0,0>,0,PRIM_COLOR,ALL_SIDES,<0,0,0>,0]);
	llDetachFromAvatar();
}
default {
	state_entry () {
		calculatebroadcastchannel();
		llListen(broadcastchannel,"",NULL_KEY,"");
		llRequestExperiencePermissions(llGetOwner(),"");
		string desc=llGetObjectDesc();
		if (desc=="DEV" || desc=="DEV-iain") {
			llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<.25,.25,0>,PRIM_COLOR,ALL_SIDES,<1,1,1>,1]);
		} else {
			llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<.001,.001,0>,PRIM_COLOR,ALL_SIDES,<1,1,1>,0]);
		}		
		if (llGetAttached()!=0) { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerreplace\":\"titlerreplace\"}"); }
	}
	
	experience_permissions_denied(key id,integer reason) { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	
	listen(integer channel,string name,key id,string message) {
		//llOwnerSay(message);
		if (llGetOwnerKey(id)==llGetOwner() && channel==broadcastchannel) {
			json=message;
			string line=jsonget("titler");
			if (line!="") { 
				integer split=llSubStringIndex(line,"|");
				string color=llGetSubString(line,0,split-1);
				string message=llGetSubString(line,split+1,-1);
				//llOwnerSay("'"+color+"'");
				//llOwnerSay("'"+message+"'");
				llSetText(message,(vector)color,1);
				return;
			}
			if (jsonget("titlerz")!="") { llSetPos(<0,0,(float)(jsonget("titlerz"))>); }
			if (jsonget("titlerreplace")!="") { llOwnerSay("Duplicate TITLER attached, detaching one"); detach(); }
			if (jsonget("titlerremove")!="") { detach(); }
		}
	}
	
    link_message(integer from,integer num,string message,key id) {
        if (num==LINK_GO) {
			llSetText("Starting Up...",<1,1,1>,1);
			if (llGetAttached()!=0) { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerreplace\":\"titlerreplace\"}"); }
        }
	}
	
	on_rez(integer n) { llResetScript(); }
	
	changed(integer change)
    {
        if (change & (CHANGED_INVENTORY))
        {
            llResetScript();
        }
		if (change & (CHANGED_REGION))
		{
			detach();
		}
    }
	experience_permissions(key id) {}
}