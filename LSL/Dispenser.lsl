#define COMMS_PROTOCOL "5"
#include "GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"

//#define COMMS_INCLUDECOOKIE
#define COMMS_INCLUDECALLBACK
//#define COMMS_INCLUDEDIGEST
#include "configuration.lsl"
#define COMMS_DONT_CHECK_CALLBACK
#include "SLCore/LSL/CommsV3.lsl"
#include "SLCore/LSL/SetDev.lsl"
//#define DEBUG

// ----- Storage for known avatars - replaced
// using linkset data - key value is avatar UUID, value value (!) is a space separated string consisting of <stage time secret>
// stages are
// stage % 100
// 0 - querying them for an existing HUD
// 1 - rezzed hud, waiting for it to query us
// 2 - hud rezzed and told where to go, should get a checkin response (eventually)
// 99 - complete
// stage / 100 is the attachment point (INTEGER DIVISION - ALWAYS ROUND DOWN)

integer cycle=0;

integer listens=4;

integer autoattach=FALSE;
integer parcelonly=TRUE;
integer slave=0;
integer IN_EXPERIENCE=TRUE;
integer IS_ACTIVE=FALSE;
integer TERMINATED=FALSE;
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
removeUser(key remove) {
  llLinksetDataDelete((string)remove);
}
integer existsUser(key check) {
	return llGetListLength(llLinksetDataFindKeys((string)check,0,100));
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
integer getStage(key who) {
	return ((integer)(llList2String(llParseString2List(llLinksetDataRead((string)who),[" "],[]),0)));
}
integer getTime(key who) {
	return ((integer)(llList2String(llParseString2List(llLinksetDataRead((string)who),[" "],[]),1)));
}
integer getSecret(key who) {
	return ((integer)(llList2String(llParseString2List(llLinksetDataRead((string)who),[" "],[]),2)));
}
key findSecret(integer secret) {
	integer ii=0;
	for (ii=llLinksetDataCountKeys()-1;ii>=0;ii--) {
		key check=((key)(llList2String(llLinksetDataFindKeys(".*",ii,1),0)));
		if (((integer)getSecret(check))==secret) { return check; }
	}
	return "";
}
forcedispense(key who) {
	adduser(who,llGetUnixTime()+10);
	integer secret=getSecret(who);
	if (secret==0) { return; }
	llLinksetDataWrite((string)who,"0 0 "+((string)getSecret(who)));
}
adduser(key check,integer when) {
	if (minz!=0 || maxz!=9999) { 
		list details=llGetObjectDetails(check,[OBJECT_POS]);
		if (llGetListLength(details)==0) { return; }
		vector pos=llList2Vector(details,0);
		//llSay(0,llKey2Name(check)+" at z "+((string)(pos.z))+" range "+((string)minz)+" - "+((string)maxz));
		if ((pos.z)<minz || (pos.z)>maxz) { return; }
	}
	if (existsUser(check)>0) { return; }
	#ifdef DEBUG
	llOwnerSay("Dispenser:New user "+llKey2Name(check)+" @"+(string)when);
	#endif
	integer stage=200;
	if (when==-1) { stage=299; } // default attachment point 2, stage is 0 or 99 (start or completed)
	integer asecret=1000+((integer)llFrand(999999999));
	asecret=asecret/3;
	asecret=asecret*3;
	// secret mod 3 is now zero, we would like to change this as follows:
	if (llGetObjectDesc()=="DEV") { asecret+=1; }
	if (llGetObjectDesc()=="DEV-iain") { asecret+=2; }
	llLinksetDataWrite((string)check,((string)stage)+" "+((string)when)+" "+((string)asecret));
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
	if ((cycle % 2) == 1) {
		integer scope=AGENT_LIST_REGION;
		if (parcelonly) { scope=AGENT_LIST_PARCEL; }
		list newkeys=llGetAgentList(scope,[]);
		if ((cycle % 30) == 1) {
			// report our known avatars list to the server every 30 seconds
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
			for (ii=llLinksetDataCountKeys()-1;ii>=0;ii--) {
				key check=((key)(llList2String(llLinksetDataFindKeys(".*",ii,1),0)));
				if (llListFindList(newkeys,[check])==-1) {
					#ifdef DEBUG
					llOwnerSay("Dispenser:Left user "+llKey2Name(check));
					#endif
					removeUser(check);
				}
			}
			// add new people
			for (ii=0;ii<llGetListLength(newkeys);ii++) {
				key check=llList2Key(newkeys,ii);
				if (existsUser(check)==0) {
					adduser(check,now+5);
				}
			}
		}
	}
	// every cycle, progress a number of these "things"
	integer actions=1;
	integer i=0;
	for (i=0;i<llLinksetDataCountKeys();i++) {
		key ik=((key)(llList2String(llLinksetDataFindKeys(".*",i,1),0)));
		integer istage=getStage(ik) % 100;
		integer itime=getTime(ik);
		integer isecret=getSecret(ik);
		if (istage>=0 && istage<99 && itime<=now && actions>0) {
			actions--;
			#ifdef DEBUG
			llOwnerSay("Dispenser:Acting on "+llKey2Name(ik)+" in stage "+(string)istage+", "+(string)actions+" actions remain");
			#endif
			
			// actions
			if (istage==0) { //querying timed out, rez a hud
				if (llGetParcelMaxPrims(llGetPos(),TRUE)-llGetParcelPrimCount(llGetPos(),PARCEL_COUNT_TOTAL,TRUE)<37) {
					llOwnerSay("Parcel has low prim count, HUD rez may fail");
					llSay(0,"Parcel has low prim count, HUD rez may fail");
				}
				// so we shoudl rez them a hud
				string slscript=getSlaveScript();
				llMessageLinked(LINK_THIS,2,slscript,(key)((string)isecret));
				#ifdef DEBUG
				llOwnerSay("Dispenser:Rez for "+llKey2Name(ik)+" with secret "+(string)isecret+" via slave "+slscript);
				#endif
				llLinksetDataWrite((string)ik,"1 "+((string)(now+10))+" "+((string)(getSecret(ik))));
				
			}			
			if (istage==1) { //hud querying us timed out?
				llSay(0,"Slow HUD rez or failure for "+llKey2Name(ik));
				llOwnerSay("Slow HUD rez or failure for "+llKey2Name(ik));
				removeUser(ik);
				return;
			}
			/*
			if (istage==2) { //hud attaching timed out?
				removeUser(ik); // we just delete them, this will cause us to ping their HUD :)
				return;			
			}*/
			if (istage==1 || istage==2) { removeUser(ik); }
			if (actions==0) { return; }
		}
	}
}

process(key id) {
	string command=jsonget("incommand");
	string othercommand=jsonget("command");
	// if (jsonget("url")!="") { comms_url=jsonget("url"); } // llOwnerSay("Dispenser: Inherited URL"); } // do we need this ? why?
	if (jsonget("autoattach")!="")
	{
		if (jsonget("autoattach")=="true") {
			//if (autoattach==FALSE) { llOwnerSay("Dispenser: Enabling auto attach"); }
			autoattach=TRUE;
		} else {
			//if (autoattach==TRUE) { llOwnerSay("Dispenser: Disabling auto attach"); }
			autoattach=FALSE;
		}
	}
	if (jsonget("minz")) { minz=((float)jsonget("minz")); }
	if (jsonget("maxz")) { maxz=((float)jsonget("maxz")); }
	if (jsonget("terminate")) { llOwnerSay("Dispenser: Terminated"); IS_ACTIVE=FALSE; TERMINATED=TRUE; }
	if (jsonget("parcelonly")!="")
	{
		if (jsonget("parcelonly")=="true") {
			//if (parcelonly==FALSE) { llOwnerSay("Dispenser: Enabling parcel only"); }
			parcelonly=TRUE;
		} else {
			//if (parcelonly==TRUE) { llOwnerSay("Dispenser: Disabling parcel only"); }
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
		llLinksetDataReset();
		llOwnerSay("Dispenser: Standby...");
	}
	link_message(integer from,integer num,string message,key id) {
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
		if (num==LINK_DIAGNOSTICS) { llSay(0,"Dispenser mem: "+(string)llGetFreeMemory()+" elements "+(string)llLinksetDataCountKeys()); }
		if (num==LINK_DISPENSE) { forcedispense(id); }
		if (num==LINK_DISPENSE_TITLER) {
			string slscript=getSlaveScript();
			integer match=-1;
			if (existsUser(id)>0) {
				integer newstage=getStage(id); newstage=newstage%100; // wipe the attachment
				newstage=newstage+(((integer)message)*100);			
				llLinksetDataWrite((string)id,((string)newstage)+" "+((string)getTime(id))+" "+((string)getSecret(id)));
				llMessageLinked(LINK_THIS,3,slscript,(key)((string)getSecret(id)));
			}
		}		
	}	
		
	timer() {
	    if (TERMINATED) { llSetTimerEvent(0); return; }
		if (!IS_ACTIVE && llGetUnixTime()>processafter) { llOwnerSay("Dispenser: Active"); IS_ACTIVE=TRUE; }
		if (IS_ACTIVE) {execute(); }
	}
	on_rez(integer n) { llResetScript(); }
	listen(integer channel,string name,key id,string message) {
		if (!IS_ACTIVE || TERMINATED) {
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
				key userkey=findSecret(sec);
				if (userkey!="") {
					#ifdef DEBUG
					llOwnerSay("Responded to attachment query for "+llKey2Name(userkey));
					#endif
					integer attachment=getStage(userkey); attachment=attachment/100;
					llRegionSayTo(id,broadcastchannel+2,(string)userkey+"|"+(string)attachment);
					integer newstage=getStage(userkey); newstage=newstage/100; newstage=newstage*100; // wipe the stage
					newstage=newstage+2;
					llLinksetDataWrite((string)userkey,((string)newstage)+" "+((string)(llGetUnixTime()+180))+" "+((string)sec));
				}
			}
			if (channel==broadcastchannel) {
				if (message=="GOTHUD") {
					key k=llGetOwnerKey(id);
					#ifdef DEBUG
					llOwnerSay("Dispenser:Has Hud "+llKey2Name(k));
					#endif
					if (existsUser(k)) {
						integer newstage=getStage(k);
						newstage=newstage/100;
						newstage=newstage*100; // wipe the stage
						newstage=newstage+99;
						llLinksetDataWrite((string)k,((string)newstage)+" "+((string)(getTime(k)))+" "+((string)(getSecret(k))));
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
			json=body; body="";
			process(NULL_KEY);
		}
	}	
}


