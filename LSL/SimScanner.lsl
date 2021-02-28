#define COMMS_PROTOCOL "3"
#include "GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"

//#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
//#define COMMS_INCLUDEDIGEST
#include "configuration.lsl"
#define COMMS_DONT_CHECK_CALLBACK
#include "SLCore/LSL/CommsV3.lsl"
#include "SLCore/LSL/SetDev.lsl"

list keys=[];
//integer debug=FALSE;
integer slave=0;
integer IN_EXPERIENCE=TRUE;
integer IS_ACTIVE=FALSE;

integer autoattach=FALSE;
integer parcelonly=TRUE;

float minz=0;
float maxz=9999;

integer cycle=0;

gphud_hang(string reason) { llResetScript(); }
#ifndef NOEXPERIENCES
validateExperience() {
	if (llGetListLength(llGetExperienceDetails(NULL_KEY))==0) {
		IN_EXPERIENCE=FALSE;
	} else { IN_EXPERIENCE=TRUE; }
}

#endif
forcedispense(key who) {
	llMessageLinked(LINK_THIS,LINK_DISPENSER_FORCE,"",who);
}
adduser(key check,integer when) {
	if (minz!=0 || maxz!=9999) { 
		list details=llGetObjectDetails(check,[OBJECT_POS]);
		if (llGetListLength(details)==0) { return; }
		vector pos=llList2Vector(details,0);
		//llSay(0,llKey2Name(check)+" at z "+((string)(pos.z))+" range "+((string)minz)+" - "+((string)maxz));
		if ((pos.z)<minz || (pos.z)>maxz) { return; }
	}
	llMessageLinked(LINK_THIS,LINK_DISPENSER_ADD,(string)when,check);
	if (llListFindList(keys,[check])==-1) { keys=keys+[check]; }
}
execute() {
	integer now=llGetUnixTime();
	cycle++;
#ifndef NOEXPERIENCES
	// function removed to save memory in dispenser
	/*
	if ((cycle % 120) == 1) { 
		string experiencestatus=experienceError();
		if (experiencestatus!="") { llOwnerSay(experiencestatus); }
	}
	*/
#endif	
	if ((cycle % 2) == 1) {
		integer scope=AGENT_LIST_REGION;
		if (parcelonly) { scope=AGENT_LIST_PARCEL; }
		list newkeys=llGetAgentList(scope,[]);
		if ((cycle % 30) == 1) {
			json="";
			json+="{\"userlist\":\"";
			integer ii=0;
			for (ii=0;ii<llGetListLength(newkeys);ii++) {
				json=json+(string)llList2Key(newkeys,ii)+"="+llKey2Name(llList2String(newkeys,ii))+",";
			}	
			json+="\"}";
			httpcommand("gphudserver.setregionavatars","GPHUD/system");
			json="";
		}
		if (autoattach) {
			// purge leavers
			integer ii=0;
			for (ii=llGetListLength(keys)-1;ii>=0;ii--) {
				key check=llList2Key(keys,ii);
				integer index=llListFindList(newkeys,[check]);
				if (index==-1) {
					//if (debug) { llOwnerSay("Dispenser:Left user "+llKey2Name(check)); }
					keys=llDeleteSubList(keys,index,index);
					llMessageLinked(LINK_THIS,LINK_DISPENSER_DELETE,"",check);
				}
			}
			// add new people
			for (ii=0;ii<llGetListLength(newkeys);ii++) {
				key check=llList2Key(newkeys,ii);
				if (llListFindList(keys,[check])==-1) {
					adduser(check,now+5);
				}
			}
		}
	}
}

process(key id) {
	string command=jsonget("incommand");
	string othercommand=jsonget("command");
	if (jsonget("autoattach")!="")
	{
		if (jsonget("autoattach")=="true") {
			if (autoattach==FALSE) { llOwnerSay("SimScanner: Enabling auto attach"); }
			autoattach=TRUE;
		} else {
			if (autoattach==TRUE) { llOwnerSay("SimScanner: Disabling auto attach"); }
			autoattach=FALSE;
		}
	}
	if (jsonget("minz")) { minz=((float)jsonget("minz")); }
	if (jsonget("maxz")) { maxz=((float)jsonget("maxz")); }
	if (jsonget("parcelonly")!="")
	{
		if (jsonget("parcelonly")=="true") {
			if (parcelonly==FALSE) { llOwnerSay("SimScanner: Enabling parcel only"); }
			parcelonly=TRUE;
		} else {
			if (parcelonly==TRUE) { llOwnerSay("SimScanner: Disabling parcel only"); }
			parcelonly=FALSE;
		}
	}
}


integer processafter=-1;
default {
	state_entry() {
#ifndef NOEXPERIENCES	
		//llOwnerSay(validateExperience());
		validateExperience();
#endif		
		llOwnerSay("SimScanner: Standby...");
	}
	link_message(integer from,integer num,string message,key id) {
		if (num==LINK_SET_STAGE) {
			integer NEWBOOTSTAGE=((integer)message);
			if (NEWBOOTSTAGE!=BOOTSTAGE) {
				BOOTSTAGE=NEWBOOTSTAGE;
				if (BOOTSTAGE==BOOT_COMPLETE) {
					setDev(FALSE);
					llSetTimerEvent(2.0);
					calculatebroadcastchannel();
					processafter=llGetUnixTime()+30;
				}
			}
		}
		if (num==LINK_DIAGNOSTICS) { llSay(0,"SimScanner mem: "+(string)llGetFreeMemory()+" elements "+(string)llGetListLength(keys)); }
	}	
		
	timer() {
		if (!IS_ACTIVE && llGetUnixTime()>processafter) { llOwnerSay("SimScanner: Beginning Scans"); IS_ACTIVE=TRUE; }
		if (IS_ACTIVE) {execute(); }
	}
	on_rez(integer n) { llResetScript(); }
	http_response( key request_id, integer status, list metadata, string body ) {
		#ifdef DEBUG
		llOwnerSay("REPLY:"+body);
		#endif
		if (status==200) {
			json=body; body="";
			process(NULL_KEY);
		}
	}	
}


