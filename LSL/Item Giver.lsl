#include "GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"
string floattext="";
init() {
	llSetObjectName("GPHUD Item Giver");
	llSetText(floattext,<.75,.75,1>,1);
}
text(string add,integer warning) {
	if (warning) { llSay(DEBUG_CHANNEL,add); }
	floattext+="\n"+llGetSubString(llGetTimestamp(),5,16)+add;
	string header="GPHUD Item Giver\n \n \n";
	floattext=llGetSubString(floattext,-254+llStringLength(header),-1);
	floattext=header+floattext;
	init();
}
give(string item,key target,string name,string callit) {
	integer type=llGetInventoryType(item);
	if (type==INVENTORY_NONE) { text("Item "+item+" missing for "+name,TRUE); return; }
	if (type==INVENTORY_SCRIPT) { text("Item "+item+" is a script for "+name,TRUE); return; }
	llGiveInventory(target,item);
	text("Gave "+callit+" to "+name,FALSE);
}

default {
	state_entry() {
		llSetMemoryLimit(llGetUsedMemory()+4096);
		init();
		calculatebroadcastchannel();
		llListen(broadcastchannel,"",NULL_KEY,"");
		integer i=0; for (i=0;i<255;i++) { floattext+=" "; }
		text("Startup",FALSE);
	}
	on_rez(integer n) { llResetScript(); }
	listen(integer channel,string name,key id,string message) {
		init();
		if (llGetOwnerKey(id)==llGetOwner()) {
			json=message;
			if (jsonget("subcommand")=="giveitem") {
				give(jsonget("itemtogive"),(key)jsonget("giveto"),jsonget("givetoname"),jsonget("itemtogive"));
			}
			if (jsonget("subcommand")=="giveitemprefix") {
				string prefix=jsonget("itemtogive");
				integer i=0; integer doneany=FALSE;
				for (i=0;i<llGetInventoryNumber(INVENTORY_ALL);i++) {
					string itemname=llGetInventoryName(INVENTORY_ALL,i);
					if (llSubStringIndex(itemname,prefix)==0) {
						give(itemname,(key)jsonget("giveto"),jsonget("givetoname"),prefix);
						doneany=TRUE;
					}
				}
				if (doneany==FALSE) { text("Prefix "+prefix+" no matches for "+jsonget("givetoname"),TRUE); }
			}			
		}
	}
}

