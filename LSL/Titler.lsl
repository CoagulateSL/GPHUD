#include "GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"
setlamps() {}
detach() {
	if (llGetInventoryType("Attacher")!=INVENTORY_SCRIPT) {
		llSetLinkPrimitiveParamsFast(LINK_SET,[PRIM_TEXT,"",<0,0,0>,0,PRIM_COLOR,ALL_SIDES,<0,0,0>,0]);
	}
	llDetachFromAvatar();
}
integer permitted=FALSE;
go() {
	llSetText("Starting Up...",<1,1,1>,1);
	if (llGetAttached()!=0) { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerreplace\":\"titlerreplace\"}"); }
	if (!permitted) { llRequestExperiencePermissions(llGetOwner(),""); }
	string desc=llGetObjectDesc();
	if (desc=="DEV" || desc=="DEV-iain") {
		llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<.25,.25,0>,PRIM_COLOR,ALL_SIDES,<1,1,1>,1]);
	} else {
		llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<.001,.001,0>,PRIM_COLOR,ALL_SIDES,<1,1,1>,0]);
	}	
}
default {
	state_entry () {
		calculatebroadcastchannel();
		llListen(broadcastchannel,"",NULL_KEY,"");
		string desc=llGetObjectDesc();
		if (desc=="DEV" || desc=="DEV-iain") {
			llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<.25,.25,0>,PRIM_COLOR,ALL_SIDES,<1,1,1>,1]);
		} else {
			llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_SIZE,<.001,.001,0>,PRIM_COLOR,ALL_SIDES,<1,1,1>,0]);
		}
		if (llGetAttached()!=0) { llRegionSayTo(llGetOwner(),broadcastchannel,"{\"titlerreplace\":\"titlerreplace\"}"); }
		if (llGetInventoryType("Attacher")==INVENTORY_SCRIPT) { llSetText("Waiting GO...",<1,1,1>,1); }
		else { go(); }
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
				string message="";
				if ((split+1)<llStringLength(line)) { message=llGetSubString(line,split+1,-1); }
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
			go();
        }
	}
	
	on_rez(integer n) { llResetScript(); }
	
	changed(integer change)
    {
        if (change & (CHANGED_OWNER))
        {
            llResetScript();
        }
    }
	experience_permissions(key id) { permitted=TRUE; }
	run_time_permissions(integer perm) { if (perm & PERMISSION_ATTACH) { permitted=TRUE; }}
}