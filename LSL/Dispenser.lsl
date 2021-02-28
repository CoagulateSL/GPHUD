#include "GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"

#include "configuration.lsl"
#include "SLCore/LSL/CommsV3.lsl"
#include "SLCore/LSL/SetDev.lsl"

list keys=[]; // key,stage, "time" (to next pay attention to this, starts at 0 :P)
// stages are
// stage % 100
// 0 - querying them for an existing HUD
// 1 - rezzed hud, waiting for it to query us
// 2 - hud rezzed and told where to go, should get a checkin response (eventually)
// 99 - complete
// stage / 100 is the attachment point (INTEGER DIVISION - ALWAYS ROUND DOWN)
list stage=[];
list time=[];
list secret=[];

integer cycle=0;

//integer debug=FALSE;
integer slave=0;
integer IN_EXPERIENCE=TRUE;
integer IS_ACTIVE=FALSE;


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
/*
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
}*/
validateExperience() {
	if (llGetListLength(llGetExperienceDetails(NULL_KEY))==0) {
		IN_EXPERIENCE=FALSE;
	} else { IN_EXPERIENCE=TRUE; }
}

// removed to save memory
/*string experienceError() {
	list experience=llGetExperienceDetails(NULL_KEY);
	if (llGetListLength(experience)==0) {
		return "";
	}
	if (llList2Integer(experience,3)==0) { return ""; }
	string experiencename=llList2String(experience,0);
	string statemessage=llList2String(experience,4);
	return "Experience error : "+experiencename+" - "+statemessage;
}*/
#endif
forcedispense(key who) {
	adduser(who,llGetUnixTime()+10);
	integer i=llListFindList(keys,[who]);
	if (i==-1) { return; }
	stage=llListReplaceList(stage,[0],i,i);
	time=llListReplaceList(time,[0],i,i);
}
adduser(key check,integer when) {
	if (llListFindList(keys,[check])!=-1) { return; }
	//if (debug) { llOwnerSay("Dispenser:New user "+llKey2Name(check)+" @"+(string)when); }
	keys+=[check];
	if (when==-1) { stage+=[299]; } else { stage+=[200]; } // default attachment point 2, stage is 0 or 99 (start or completed)
	time+=[when];
	integer asecret=1000+((integer)llFrand(999999999));
	asecret=asecret/3;
	asecret=asecret*3;
	// secret mod 3 is now zero, we would like to change this as follows:
	if (llGetObjectDesc()=="DEV") { asecret+=1; }
	if (llGetObjectDesc()=="DEV-iain") { asecret+=2; }
	secret+=[asecret];
	//attachment+=[2];
	llRegionSayTo(check,broadcastchannel,"GOTHUD");
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
	// every cycle, progress a number of these "things"
	integer actions=1;
	integer i=0;
	for (i=0;i<llGetListLength(keys);i++) {
		key ik=llList2Key(keys,i);
		integer istage=llList2Integer(stage,i) % 100;
		integer itime=llList2Integer(time,i);
		integer isecret=llList2Integer(secret,i);
		if (istage>=0 && istage<99 && itime<=now && actions>0) {
			actions--;
			//if (debug) { llOwnerSay("Dispenser:Acting on "+llKey2Name(ik)+" in stage "+(string)istage+", "+(string)actions+" actions remain"); }
			
			// actions
			if (istage==0) { //querying timed out, rez a hud
				// so we shoudl rez them a hud
				string slscript=getSlaveScript();
				llMessageLinked(LINK_THIS,2,slscript,(key)((string)isecret));
				//if (debug) { llOwnerSay("Dispenser:Rez for "+llKey2Name(ik)+" with secret "+(string)isecret+" via slave "+slscript); }
				stage=llListReplaceList(stage,[1],i,i);
				time=llListReplaceList(time,[now+10],i,i);
			}
			/*
			if (istage==1) { //hud querying us timed out?
				llSay(0,"Slow rez or failure for "+llKey2Name(ik)+", resetting");
				listdel(i);
				return;
			}
			if (istage==2) { //hud attaching timed out?
				listdel(i); // we just delete them, this will cause us to ping their HUD :)
				return;			
			}*/
			if (istage==1 || istage==2) { listdel(i); }
			if (actions==0) { return; }
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
		llOwnerSay("Dispenser: Standby...");
	}
	link_message(integer from,integer num,string message,key id) {
		if (num==LINK_DISPENSER_FORCE) { forcedispense(id); }
		if (num==LINK_DISPENSER_ADD) { adduser(id,(integer)message); }
		if (num==LINK_DISPENSER_DELETE) {
			integer i=llListFindList(keys,[id]);
			if (i!=-1) { listdel(i); }
		}
		if (num==LINK_SET_STAGE) {
			integer NEWBOOTSTAGE=((integer)message);
			if (NEWBOOTSTAGE!=BOOTSTAGE) {
				BOOTSTAGE=NEWBOOTSTAGE;
				if (BOOTSTAGE==BOOT_COMPLETE) {
					setDev(FALSE);
					// our startup!
					llOwnerSay("Dispenser: HUD Scan");
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
		if (num==LINK_DIAGNOSTICS) { llSay(0,"Dispenser mem: "+(string)llGetFreeMemory()+" elements "+(string)llGetListLength(keys)); }
		if (num==LINK_DISPENSE) { forcedispense(id); }
		if (num==LINK_DISPENSE_TITLER) {
			string slscript=getSlaveScript();
			integer match=-1;
			integer i=0;
			for (i=0;i<llGetListLength(keys);i++) {
				if (id==llList2Key(keys,i)) { match=i; }
			}
			if (match!=-1) {
				integer newstage=llList2Integer(stage,match); newstage=newstage%100; // wipe the attachment
				newstage=newstage+(((integer)message)*100);			
				stage=llListReplaceList(stage,[newstage],match,match);
				llMessageLinked(LINK_THIS,3,slscript,(key)((string)llList2Integer(secret,match)));
			}
		}		
	}	
		
	timer() {
		if (!IS_ACTIVE && llGetUnixTime()>processafter) { llOwnerSay("Dispenser: Active"); IS_ACTIVE=TRUE; }
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
					//if (debug) { llOwnerSay("Responded to attachment query for "+llKey2Name(llList2Key(keys,i))); }
					integer attachment=llList2Integer(stage,i); attachment=attachment/100;
					llRegionSayTo(id,broadcastchannel+2,(string)llList2Key(keys,i)+"|"+(string)attachment);
					integer newstage=llList2Integer(stage,i); newstage=newstage/100; newstage=newstage*100; // wipe the stage
					newstage=newstage+2;
					stage=llListReplaceList(stage,[newstage],i,i);
					time=llListReplaceList(time,[llGetUnixTime()+180],i,i);
				}
			}
			if (channel==broadcastchannel) {
				if (message=="GOTHUD") {
					key k=llGetOwnerKey(id);
					//if (debug) { llOwnerSay("Dispenser:Has Hud "+llKey2Name(k)); }
					integer n=llListFindList(keys,[k]);
					if (n!=-1) {
						integer newstage=llList2Integer(stage,n);
						newstage=newstage/100;
						newstage=newstage*100; // wipe the stage
						newstage=newstage+99;
						stage=llListReplaceList(stage,[newstage],n,n);
					}
				}
				json=message;
				if (jsonget("forcedispense")!="") {
					forcedispense((key)jsonget("forcedispense"));
				}
			}
		}

	}
}


