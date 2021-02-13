#include "GPHUDHeader.lsl"
#include "configuration.lsl"
//#define DUMPLINKS

integer ATTACH_LOCATION=ATTACH_HUD_BOTTOM;
integer IN_EXPERIENCE=FALSE;
integer stageentry=0;
string system="???";
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
#endif
key suggestedowner=NULL_KEY;

#ifdef DUMPLINKS
dumpLinks() {
	integer i=0;
	//llOwnerSay("public static final int HUD_MAIN=1;");
	llOwnerSay("#define HUD_MAIN 1");
	for (i=2;i<=llGetNumberOfPrims();i++) {
		list data=llGetLinkPrimitiveParams(i,[PRIM_DESC]);
		string desc=llList2String(data,0);
		//llOwnerSay("public static final int "+desc+"="+((string)i)+";");
		llOwnerSay("#define "+desc+" "+((string)i));
	}
}
#endif
integer isTitler() { return llSubStringIndex(llGetObjectName(),"Titler")!=-1; }
terminate(string reason,vector color,float delay) {
	llSetText(reason,color,1.0);
	llSleep(delay);
}

trydetach() {
	if (llGetAttached()==0) { llDie(); }
	if ((llGetPermissions() & PERMISSION_ATTACH) && llGetPermissionsKey()==llGetOwner()) { llDetachFromAvatar(); llDie(); }
#ifndef NOEXPERIENCES
	if (IN_EXPERIENCE && llAgentInExperience(llGetOwner())) {
		llRequestExperiencePermissions(llGetOwner(),"");
		return;
	}
#endif
	llRequestPermissions(llGetOwner(),PERMISSION_ATTACH);
}

string name() {
  if (suggestedowner!=NULL_KEY) {
    return " ["+llKey2Name(suggestedowner)+"]";
  }
  return "";
}

status(string text) { llSetText("Attacher: ["+system+"] "+text+name(),<0.75,0.75,1>,1); }
die(string text) { llSetText("Attacher: ERROR - "+text+name(),<1,0.5,0.5>,1); llSleep(10.0); llDie(); }

integer dodie=FALSE;

default {
	state_entry() {
		if (isTitler()) { ATTACH_LOCATION=ATTACH_HEAD; }
		state standby;
	}
}
state shutdown {
	state_entry() { trydetach(); }
#ifndef NOEXPERIENCES
	experience_permissions_denied(key id,integer reason) { llRequestPermissions(llGetOwner(),PERMISSION_ATTACH); }
	experience_permissions(key agent) {	trydetach(); }	
#endif	
	run_time_permissions(integer perms) { trydetach(); }
}
state standby {
	state_entry() {
		suggestedowner=NULL_KEY;
		status("Script reset manually, we do nothing, we expect to be rezzed");
		#ifdef DUMPLINKS
		dumpLinks();
		#endif
	}
	
	// wake up method one - we get rezzed, either by the server or by the creator
	on_rez(integer n) {
		if (n==0) {
			//manual rez from inventory.  just go to sleep.
			status("Manual rez detected, do nothing");
			return;
		}
		status("Rez detected");
		dodie=TRUE;
		integer sysnum=n%3;
		key k=SLCORE_COAGULATE_LOGO;
		if (sysnum==0) { llSetObjectDesc("GPHUD Version "+VERSION+" - "+COMPILEDATE+" "+COMPILETIME); system="Production"; }
		if (sysnum==1) { llSetObjectDesc("DEV"); system="Testing";}
		if (sysnum==2) { llSetObjectDesc("DEV-iain"); system="Iain-Dev"; }
		if (sysnum!=0) { 
			k=SLCORE_COAGULATE_DEV_LOGO;
		}
		if (llGetObjectName()=="GPHUD") { llSetLinkPrimitiveParamsFast(LINK_THIS,[PRIM_TEXTURE,ALL_SIDES,k,<1,1,1>,<0,0,0>,0]); }
		llSetTimerEvent(20.0);
		calculatebroadcastchannel();
		llListen(broadcastchannel+2,"",NULL_KEY,"");
		llSay(broadcastchannel+1,(string)n);
	}
	timer() {
		die("Receiving attachment target timed out :(");
	}
	touch_start(integer n) { if (llDetectedKey(0)==SYSTEM_OWNER_UUID) { status("Sending fake startup message!"); llMessageLinked(LINK_THIS,LINK_GO,"",""); state comatose; }}
	listen(integer channel,string name,key id,string message) {
		if (channel==broadcastchannel+2) {
			if (llSubStringIndex(message,"|")==-1) {
				suggestedowner=(key)message;
			} else {
				suggestedowner=(key)llGetSubString(message,0,35);
				if (isTitler()) {
					ATTACH_LOCATION=(integer)llGetSubString(message,37,-1);
				}
			}
			state initiateattach;
		}
	}
}
state comatose {
	state_entry(){}
}
state initiateattach {
	// discern best attachment path for 'suggestedowner'
	state_entry() {
#ifndef NOEXPERIENCES
		validateExperience();
#else
		IN_EXPERIENCE=FALSE;
#endif		
		if (!IN_EXPERIENCE) {
			// we, the script, are not experience enabled.  resort to conventional permissions
			state getpermission;
		}
#ifndef NOEXPERIENCES
		// great... are THEY in our experience?
		if (llAgentInExperience(suggestedowner)) {
			state experiencepermission;
		}
	state experiencepermission;
#else
	state getpermission;
#endif	
	}
}

#ifndef NOEXPERIENCES
state experiencepermission {
	//attempt to get permissions via experience system
	state_entry() {
		status("Request experience permissions");
		stageentry=llGetUnixTime();
		llSetTimerEvent(5.01);
		llRequestExperiencePermissions(suggestedowner,"");
	}
	
	experience_permissions_denied(key id,integer reason) {
		status("Experience permissions denied - "+llGetExperienceErrorMessage(reason));
		llSetTimerEvent(0.0);
		llSleep(1.0);
		state getpermission;
	}
	
	experience_permissions(key agent) {
		state doattach;
	}
	
	timer() {
		// wait no more than 15 seconds
		if (llGetUnixTime()>(stageentry+120.0)) {
			die("Experience permissions timed out.");
		}
	}
}
#endif

state getpermission {
	//attempt to get permissions via legacy system
	state_entry() {
		status("Request legacy attachment permissions");
		stageentry=llGetUnixTime();
		llSetTimerEvent(5.01);
		llRequestPermissions(suggestedowner,PERMISSION_ATTACH);
	}
	
	run_time_permissions(integer perm) {
		if (perm & PERMISSION_ATTACH) {
			state doattach;
		}
		status("Legacy permissions not fully granted");
	}
	
	timer() {
		// wait no more than 15 seconds
		if (llGetUnixTime()>(stageentry+120.0)) {
			die("Legacy permissions timed out");
		}
	}
}

state doattach {
	state_entry ()
	{
		status("GPHUD Attaching, Greetings");
		stageentry=llGetUnixTime();
		llSetTimerEvent(1.01);
		llAttachToAvatarTemp(ATTACH_LOCATION);
		if (llGetAttached()!=0 && llGetOwner()==suggestedowner) { state finished; }
	}
	attach (key id) {
		if (id) {
			if (llGetAttached()!=0 && llGetOwner()==suggestedowner) { state finished; }
		}
	}
	
	timer() {
		if (llGetAttached()!=0 && llGetOwner()==suggestedowner) { state finished; }
		// wait no more than 15 seconds
		if (llGetUnixTime()>(stageentry+120.0)) {
			die("Attaching timed out?");
		}
	}
}

state finished {
	state_entry() { status("Attached, starting the GPHUD"); 
		llMessageLinked(LINK_THIS,LINK_GO,"","");
		if (dodie) { llSleep(1.0);
			llRemoveInventory(llGetScriptName());
			return;
		}
		llOwnerSay("Not purging attacher stub");
	}
}