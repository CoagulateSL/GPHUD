#include "CommsHeader.lsl"
init() {
	llSetObjectName("GPHUD Item Giver");
	llSetText("GPHUD Item Giver\n \n \n \n \n",<.75,.75,.75>,1);
}
default {
	state_entry() {
		llSetMemoryLimit(llGetUsedMemory()+4096);
		init();
		calculatebroadcastchannel();
		llListen(broadcastchannel,"",NULL_KEY,"");
	}
	on_rez(integer n) { llResetScript(); }
	listen(integer channel,string name,key id,string message) {
		init();
		if (llGetOwnerKey(id)==llGetOwner()) {
			json=message;
			if (jsonget("subcommand")=="giveitem") {
				llGiveInventory((key)(jsonget("giveto")),jsonget("itemtogive"));
			}
		}
	}
}