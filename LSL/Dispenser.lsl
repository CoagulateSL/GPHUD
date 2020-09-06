#include "GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"

//#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
//#define COMMS_INCLUDEDIGEST
#include "configuration.lsl"
#define COMMS_DONT_CHECK_CALLBACK
#include "SLCore/LSL/CommsV3.lsl"
#include "SLCore/LSL/SetDev.lsl"

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
integer listens=4;

integer autoattach=FALSE;
integer parcelonly=TRUE;
integer debug=FALSE;
integer slave=0;
integer IN_EXPERIENCE=TRUE;
integer IS_ACTIVE=FALSE;

float minz=0;
float maxz=9999;


integer getSlaveId() {
	integer oldslave=slave;
    slave++;
    if (llGetInventoryType("Dispenser Slave "+(string)slave)!=INVENTORY_SCRIPT) { slave=0; }
    return oldslave;
}
string getSlaveScript() {
    string name="Dispenser Slave";
	integer slave=getSlaveId();
    if (slave>0) { name+=" "+(string)slave; }
    return name;
}
gphud_hang(string reason) { llResetScript(); }
listdel(integer i) {
	keys=llDeleteSubList(keys,i,i);
	stage=llDeleteSubList(stage,i,i);
	time=llDeleteSubList(time,i,i);
	secret=llDeleteSubList(secret,i,i);

}
#ifndef NOEXPERIENCES
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
#endif
forcedispense(key who) {
	adduser(who,llGetUnixTime()+10);
	integer i=llListFindList(keys,[who]);
	if (i==-1) { return; }
	stage=llListReplaceList(stage,[0],i,i);
	time=llListReplaceList(time,[0],i,i);
}
adduser(key check,integer when) {
	if (minz!=0 || maxz!=9999) { 
		list details=llGetObjectDetails(check,[OBJECT_POS]);
		if (llGetListLength(details)==0) { return; }
		vector pos=llList2Vector(details,0);
		//llSay(0,llKey2Name(check)+" at z "+((string)(pos.z))+" range "+((string)minz)+" - "+((string)maxz));
		if ((pos.z)<minz || (pos.z)>maxz) { return; }
	}
	if (llListFindList(keys,[check])!=-1) { return; }
	if (debug) { llOwnerSay("Dispenser:New user "+llKey2Name(check)+" @"+(string)when); }
	keys+=[check];
	if (when==-1) { stage+=[-1]; } else { stage+=[0]; }
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
#ifndef NOEXPERIENCES
	if ((cycle % 120) == 1) { 
		string experiencestatus=experienceError();
		if (experiencestatus!="") { llOwnerSay(experiencestatus); }
	}
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
		}
		if (autoattach) {
			// purge leavers
			integer ii=0;
			for (ii=llGetListLength(keys)-1;ii>=0;ii--) {
				key check=llList2Key(keys,ii);
				if (llListFindList(newkeys,[check])==-1) {
					if (debug) { llOwnerSay("Dispenser:Left user "+llKey2Name(check)); }
					listdel(ii);
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
	// every cycle, progress a number of these "things"
	integer actions=1;
	integer i=0;
	for (i=0;i<llGetListLength(keys);i++) {
		key ik=llList2Key(keys,i);
		integer istage=llList2Integer(stage,i);
		integer itime=llList2Integer(time,i);
		integer isecret=llList2Integer(secret,i);
		if (istage>=0 && itime<=now && actions>0) {
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

process(key id) {
	string command=jsonget("incommand");
	string othercommand=jsonget("command");
	if (jsonget("url")!="") { comms_url=jsonget("url"); } // llOwnerSay("Dispenser: Inherited URL"); }
	if (jsonget("autoattach")!="")
	{
		if (jsonget("autoattach")=="true") {
			if (autoattach==FALSE) { llOwnerSay("Dispenser: Enabling auto attach"); }
			autoattach=TRUE;
		} else {
			if (autoattach==TRUE) { llOwnerSay("Dispenser: Disabling auto attach"); }
			autoattach=FALSE;
		}
	}
	if (jsonget("minz")) { minz=((float)jsonget("minz")); }
	if (jsonget("maxz")) { maxz=((float)jsonget("maxz")); }
	if (jsonget("parcelonly")!="")
	{
		if (jsonget("parcelonly")=="true") {
			if (parcelonly==FALSE) { llOwnerSay("Dispenser: Enabling parcel only"); }
			parcelonly=TRUE;
		} else {
			if (parcelonly==TRUE) { llOwnerSay("Dispenser: Disabling parcel only"); }
			parcelonly=FALSE;
		}
	}
}


integer processafter=-1;
default {
	state_entry() {
#ifndef NOEXPERIENCES	
		llOwnerSay(validateExperience());
#endif		
		llOwnerSay("Dispenser: Awaiting Server Boot Complete");
	}
	link_message(integer from,integer num,string message,key id) {
		if (num==LINK_SET_STAGE) {
			integer NEWBOOTSTAGE=((integer)message);
			if (NEWBOOTSTAGE!=BOOTSTAGE) {
				BOOTSTAGE=NEWBOOTSTAGE;
				if (BOOTSTAGE==BOOT_COMPLETE) {
					setDev(FALSE);
					// our startup!
					llOwnerSay("Dispenser: Server Booted ; searching existing HUDs");
					llSetTimerEvent(2.0);
					processafter=llGetUnixTime()+30;
					calculatebroadcastchannel();
					llListen(broadcastchannel,"",NULL_KEY,"");
					llListen(broadcastchannel+1,"",NULL_KEY,"");
					llRegionSay(broadcastchannel,"GOTHUD");
				}
			}
		}
		if (num==LINK_INSTANT_MESSAGE_SEND) {
			integer scriptnumber=getSlaveId();
			llMessageLinked(LINK_THIS,LINK_IM_SLAVE_0-scriptnumber,message,id);
		}
		if (num==LINK_DIAGNOSTICS) { llSay(0,"Dispenser free memory: "+(string)llGetFreeMemory()+" tracked elements "+(string)llGetListLength(keys)); }
		if (num==LINK_DISPENSE) { forcedispense(id); }
	}	
		
	timer() {
		if (!IS_ACTIVE && llGetUnixTime()>processafter) { llOwnerSay("Dispenser: Startup complete"); IS_ACTIVE=TRUE; }
		if (IS_ACTIVE) {execute(); }
	}
	on_rez(integer n) { llResetScript(); }
	listen(integer channel,string name,key id,string message) {
		if (!IS_ACTIVE) {
			if (channel==broadcastchannel) {
				if (message=="GOTHUD") {
					adduser(llGetOwnerKey(id),-1);
				}
			}
		}
		else  // else ACTIVE is TRUE
		{
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
				json=message;
				if (jsonget("forcedispense")!="") {
					forcedispense((key)jsonget("forcedispense"));
				}
			}
		}

	}
	http_response( key request_id, integer status, list metadata, string body ) {
		#ifdef DEBUG
		llOwnerSay("REPLY:"+body);
		#endif
		if (status==200) {
			json=body;
			process(NULL_KEY);
		}
	}	
}


