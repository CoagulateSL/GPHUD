//#define DOINIT

#ifdef DOINIT
#include "SL/LSL/Library/FindPrim.lsl"
string createlistline="";
createList(string listname,string linkname,integer number) {
	createlistline+="\nlist "+listname+"=[";
	integer i=0;
	for (i=1;i<=number;i++) {
		createlistline+=(string)findPrim(linkname+((string)i));
		if(i<number) { createlistline+=","; }
	}
	createlistline+="];";
}

#endif
list effectslinks=[5,8,9,12,2,3];
list effectstimerlinks=[6,7,10,11,13,4];
list inventorylinks=[14,15,16,17,18,19,20,21,22,23,24,25,26];
resetPrims() {
	integer i=0;
	for (i=0;i<llGetListLength(effectslinks);i++) {
		integer p=llList2Integer(effectslinks,i);
		llSetLinkPrimitiveParamsFast(p,[PRIM_SIZE,<0.02,0.04,0.04>,PRIM_POSITION,<0,-0.14-(((float)i)*0.04),-0.04>,PRIM_TEXTURE,ALL_SIDES,(key)"b39860d0-8c5c-5d51-9dbf-3ef55dafe8a4",<1,1,1>,<0,0,0>,0]);
		p=llList2Integer(effectstimerlinks,i);
		llSetLinkPrimitiveParamsFast(p,[PRIM_SIZE,<0.02,0.04,0.00>,PRIM_POSITION,<0,-0.14-(((float)i)*0.04),-0.04>,PRIM_TEXT,"XX:XX",<1,1,1>,1,PRIM_TEXTURE,ALL_SIDES,TEXTURE_TRANSPARENT,<1,1,1>,<0,0,0>,0]);
	}
	for (i=0;i<llGetListLength(inventorylinks);i++) {
		integer p=llList2Integer(inventorylinks,i);
		key texture=TEXTURE_BLANK;
		if (i==0) { texture=TEXTURE_TRANSPARENT; }
		list l=[PRIM_SIZE,<0.02,0.08+0.08+0.08+0.24,0.039>,PRIM_POSITION,<0,0,0.1+0.08+0.04*((float)i)>,PRIM_TEXTURE,ALL_SIDES,texture,<1,1,1>,<0,0,0>,0];	
		if ((i+1)<llGetListLength(inventorylinks)) { l+=[PRIM_TEXT,"Hello there this is a line of text 6789401234567895123456789612345",<0,0,0>,1]; }
		else { l+=[PRIM_TEXT,"",<1,1,1>,1]; }
		llSetLinkPrimitiveParamsFast(p,l);
	}
}

default {
	state_entry() {
		#ifdef DOINIT
		createList("effectslinks","Effects",6);
		createList("effectstimerlinks","EffectsTimer",6);
		createList("inventorylinks","Inventory",13);
		llOwnerSay(createlistline);
		#else
		resetPrims();
		#endif
	}
}
