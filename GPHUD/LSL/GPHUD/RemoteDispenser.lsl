#include "SL/LSL/GPHUD/GPHUDHeader.lsl"
#include "SL/LSL/Comms/API.lsl"
#include "SL/LSL/Library/JsonTools.lsl"
default {
	state_entry() {
		calculatebroadcastchannel();
	}
	touch_start(integer touchers) {
		for (touchers--;touchers>=0;touchers--) {
			json=llJsonSetValue("",["dispense"],(string)llDetectedKey(touchers));
			llRegionSay(broadcastchannel,json);
			llRegionSayTo(llDetectedKey(touchers),0,"Request for HUD sent!");
		}
	}
}