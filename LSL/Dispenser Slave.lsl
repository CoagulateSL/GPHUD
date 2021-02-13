#include "GPHUDHeader.lsl"

integer channel=0;
integer listener=0;
integer scriptnumber=0;
key target=NULL_KEY;
integer TITLER=1;
rezSequence(key targetid,integer rezHUD,integer rezTitler) {
	if (rezHUD) { llRezObject("GPHUD",llGetPos()+<0,0,llFrand(3.5)+1.5>,ZERO_VECTOR,ZERO_ROTATION,(integer)((string)targetid)); }
	if (rezTitler) { llRezObject("GPHUD Titler",llGetPos()+<0,0,llFrand(3.5)+1.5>,ZERO_VECTOR,ZERO_ROTATION,(integer)((string)targetid)); }
}

default {
	state_entry(){llSetMemoryLimit(llGetUsedMemory()+4096);
		scriptnumber=((integer)("0"+llGetSubString(llGetScriptName(),16,-1)));
		//llOwnerSay("is number "+((string)scriptnumber)); 
	}
	link_message(integer prim,integer num,string message,key id) {
		if (num==LINK_DIAGNOSTICS) { llSay(0,llGetScriptName()+":"+(string)llGetUsedMemory()+" -> free: "+(string)llGetFreeMemory()); return; }
		if (num==(LINK_IM_SLAVE_0-scriptnumber)) {
			llInstantMessage(id,message);
		}
		if (num==1) {
			if (message==llGetScriptName()) {
				rezSequence(id,TRUE,TRUE); 
			}
		}
		if (num==2) {
			if (message==llGetScriptName()) {
				rezSequence(id,TRUE,FALSE); 
			}
		}
		if (num==3) {
			if (message==llGetScriptName()) {
				rezSequence(id,FALSE,TRUE); 
			}
		}		
	}	
}


