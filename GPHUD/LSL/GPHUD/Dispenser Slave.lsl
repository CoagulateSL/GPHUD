#include "CommsHeader.lsl"

integer channel=0;
integer listener=0;
key target=NULL_KEY;
integer TITLER=1;
rezSequence(key targetid) {
	llRezObject("GPHUD",llGetPos()+<0,0,llFrand(3.5)+1.5>,ZERO_VECTOR,ZERO_ROTATION,(integer)((string)targetid));
	llRezObject("GPHUD Titler",llGetPos()+<0,0,llFrand(3.5)+1.5>,ZERO_VECTOR,ZERO_ROTATION,(integer)((string)targetid));
}

default {
	state_entry(){llSetMemoryLimit(llGetUsedMemory()+4096);}
	link_message(integer prim,integer num,string message,key id) {
		if (num==LINK_DIAGNOSTICS) { llSay(0,llGetScriptName()+":"+(string)llGetUsedMemory()+" -> free: "+(string)llGetFreeMemory()); return; }
		if (num==1) {
			if (message==llGetScriptName()) {
				rezSequence(id); 
			}
		}
	}	
}
