#include "CommsHeader.lsl"
list keys=[]; // key,stage, "time" (to next pay attention to this, starts at 0 :P)
// stages are
// 0 - querying them for an existing HUD
// 1 - rezzed hud, waiting for it to query us
// 2 - hud rezzed and told where to go, should get a checkin response (eventually)
// -1 - complete
integer cycle=0;
list stage=[];
list time=[];
list secret=[];

integer autoattach=FALSE;
integer parcelonly=TRUE;
integer debug=FALSE;
integer slave=0;
integer IN_EXPERIENCE=TRUE;
string getSlaveScript() {
    string name="Dispenser Slave";
    if (slave>0) { name+=" "+(string)slave; }
    slave++;
    if (llGetInventoryType("Dispenser Slave "+(string)slave)!=INVENTORY_SCRIPT) { slave=0; }
    return name;
}

listdel(integer i)
{
	keys=llDeleteSubList(keys,i,i);
	stage=llDeleteSubList(stage,i,i);
	time=llDeleteSubList(time,i,i);
	secret=llDeleteSubList(secret,i,i);

}
string validateExperience() {
	list experience=llGetExperienceDetails(NULL_KEY);
	if (llGetListLength(experience)==0) {
		IN_EXPERIENCE=FALSE;
		return "";
	}
	IN_EXPERIENCE=TRUE;
	string experiencename=llList2String(experience,0);
	string statemessage=llList2String(experience,4);
	return experiencename+" - "+statemessage;
}
string experienceError() {
	list experience=llGetExperienceDetails(NULL_KEY);
	if (llGetListLength(experience)==0) {
		return "";
	}
	if (llList2Integer(experience,3)==0) { return ""; }
	string experiencename=llList2String(experience,0);
	string statemessage=llList2String(experience,4);
	return "Experience error : "+experiencename+" - "+statemessage;
}
adduser(key check,integer when)
{
	if (debug) { llOwnerSay("Dispenser:New user "+llKey2Name(check)); }
	keys+=[check];
	stage+=[0];
	time+=[when];
	integer asecret=1000+((integer)llFrand(999999999));
	asecret=asecret/3;
	asecret=asecret*3;
	// secret mod 3 is now zero, we would like to change this as follows:
	if (llGetObjectDesc()=="DEV") { asecret+=1; }
	if (llGetObjectDesc()=="DEV-iain") { asecret+=2; }
	secret+=[asecret];
	llRegionSayTo(check,broadcastchannel,"GOTHUD");
}
execute() {
	integer now=llGetUnixTime();
	cycle++;
	if ((cycle % 120) == 1) { 
		string experiencestatus=experienceError();
		if (experiencestatus!="") { llOwnerSay(experiencestatus); }
	}
	if ((cycle % 2) == 1) {
		if (autoattach) {
			integer scope=AGENT_LIST_REGION;
			if (parcelonly) { scope=AGENT_LIST_PARCEL; }
			list newkeys=llGetAgentList(scope,[]);
			// purge leavers
			integer i=0;
			for (i=llGetListLength(keys)-1;i>=0;i--) {
				key check=llList2Key(keys,i);
				if (llListFindList(newkeys,[check])==-1) {
					if (debug) { llOwnerSay("Dispenser:Left user "+llKey2Name(check)); }
					listdel(i);
				}
			}
			// add new people
			for (i=0;i<llGetListLength(newkeys);i++) {
				key check=llList2Key(newkeys,i);
				if (llListFindList(keys,[check])==-1) {
					adduser(check,now+5);
				}
			}
		}
	}
	// every cycle, progress a number of these "things"
	integer actions=3;
	integer i=0;
	for (i=0;i<llGetListLength(keys);i++) {
		key ik=llList2Key(keys,i);
		integer istage=llList2Integer(stage,i);
		integer itime=llList2Integer(time,i);
		integer isecret=llList2Integer(secret,i);
		if (istage>=0 && itime<=now) {
			actions--;
			if (debug) { llOwnerSay("Dispenser:Acting on "+llKey2Name(ik)+" in stage "+(string)istage+", "+(string)actions+" actions remain"); }
			
			// actions
			if (istage==0) { //querying timed out, rez a hud
				// so we shoudl rez them a hud
				string slscript=getSlaveScript();
				llMessageLinked(LINK_THIS,1,slscript,(key)((string)isecret));
				if (debug) { llOwnerSay("Dispenser:Rez for "+llKey2Name(ik)+" with secret "+(string)isecret+" via slave "+slscript); }
				stage=llListReplaceList(stage,[1],i,i);
				time=llListReplaceList(time,[now+10],i,i);
			}
			if (istage==1) { //hud querying us timed out?
				llSay(0,"Dispenser Module: Alert: Slow rez or rez failure detected for "+llKey2Name(ik)+", resetting their entry");
				listdel(i);
				return;
			}
			if (istage==2) { //hud attaching timed out?
				listdel(i); // we just delete them, this will cause us to ping their HUD :)
				return;			
			}
			
			if (actions==0) { return; }
		}
	}
}

default {
	state_entry() {
		llMessageLinked(LINK_THIS,LINK_GET_DISPENSER_CONFIG,"","");
		llSetTimerEvent(60.0);
	}
	timer() {
		llMessageLinked(LINK_THIS,LINK_GET_DISPENSER_CONFIG,"","");
	}	
	link_message(integer from,integer num,string message,key id) {
		if (num==LINK_DISPENSER_CONFIG) {
			autoattach=(integer)message;
			parcelonly=(integer)((string)id);
			state go;
		}
		if (num==LINK_LEGACY_PACKAGE) { llOwnerSay("Dispenser resetting into standby"); llResetScript(); }
		if (num==LINK_DIAGNOSTICS) { llSay(0,"Dispenser (STANDBY) free memory: "+(string)llGetFreeMemory()); }

		if (num==LINK_GO) { llMessageLinked(LINK_THIS,LINK_GET_DISPENSER_CONFIG,"",""); }
	}	
}
state go{
	state_entry() {
		string auto="DISABLED";
		string parcel="off";
		if (autoattach) { auto="on"; }
		if (parcelonly) { parcel="ENABLED"; }
		llOwnerSay("GPHUD Dispenser module initialising, autoattach is "+auto+" and parcelonly is "+parcel);
		llOwnerSay(validateExperience());
		calculatebroadcastchannel();
		llListen(broadcastchannel,"",NULL_KEY,"");
		llListen(broadcastchannel+1,"",NULL_KEY,"");
		execute();
		llSetTimerEvent(1.0);
	}
	listen(integer channel,string name,key id,string message) {
		if (channel==broadcastchannel+1) {
			integer sec=(integer)message;
			integer i=llListFindList(secret,[sec]);
			if (i!=-1) {
				if (debug) { llOwnerSay("Responded to attachment query for "+llKey2Name(llList2Key(keys,i))); }
				llRegionSayTo(id,broadcastchannel+2,(string)llList2Key(keys,i));
				stage=llListReplaceList(stage,[2],i,i);
				time=llListReplaceList(time,[llGetUnixTime()+180],i,i);
			}
		}
		if (channel==broadcastchannel) {
			if (message=="GOTHUD") {
				key k=llGetOwnerKey(id);
				if (debug) { llOwnerSay("Dispenser:Has Hud "+llKey2Name(k)); }
				integer n=llListFindList(keys,[k]);
				if (n!=-1) {
					stage=llListReplaceList(stage,[-1],n,n);
				}
			}
		}
	}
	timer() {
		execute();
	}
    link_message(integer from,integer num,string message,key id) {
		if (num==LINK_LEGACY_PACKAGE) { llOwnerSay("Dispenser resetting into standby"); llResetScript(); }
		if (num==LINK_DIAGNOSTICS) { llSay(0,"Dispenser free memory: "+(string)llGetFreeMemory()+" tracked elements "+(string)llGetListLength(keys)); }
		if (num==LINK_DISPENSE) { adduser(id,0); }
	}	
}
