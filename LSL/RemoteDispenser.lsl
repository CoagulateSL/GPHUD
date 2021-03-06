#include "GPHUDHeader.lsl"
#include "SLCore/LSL/JsonTools.lsl"
default {
	state_entry() {
		calculatebroadcastchannel();
	}
	touch_start(integer touchers) {
		for (touchers--;touchers>=0;touchers--) {
			json=llJsonSetValue("",["forcedispense"],(string)llDetectedKey(touchers));
			llRegionSay(broadcastchannel,json);
			llRegionSayTo(llDetectedKey(touchers),0,"Request for HUD sent!");
		}
	}
	on_rez(integer n) { llResetScript(); }
}
