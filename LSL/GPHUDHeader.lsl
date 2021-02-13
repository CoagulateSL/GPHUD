#ifndef _INCLUDE_GPHUD_HEADER
#define _INCLUDE_GPHUD_HEADER
#include "configuration.lsl"
#include "GPHUD/LSL/Constants.lsl"
#include "SLCore/LSL/SetDev.lsl"

// reason for shutdown
integer broadcastchannel=0;
typedSay(string s) {
	#ifdef MESSAGE_IS_SAY
	llSay(0,s);
	#else
	llOwnerSay(s);
	#endif
}

calculatebroadcastchannel() {
	vector loc=llGetRegionCorner();
	integer x=(integer)loc.x;
	integer y=(integer)loc.y;
	x=x/256;
	y=y/256;
	x=x&0xffff;
	y=y&0x0fff;
	integer output=(y*0x10000)+x;
	output=output^ CHANNEL_MUTATOR ;
	output=-llAbs(output);
	if (output>-1000) { output=output-99999; }
	broadcastchannel=output;
	setDev(FALSE);
	if (DEV) { broadcastchannel-=10; }
}

#endif
