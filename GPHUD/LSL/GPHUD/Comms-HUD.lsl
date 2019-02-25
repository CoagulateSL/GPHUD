//---------- Enable the inbound circuit?
#define COMMS_OPEN_RX
//---------- Do we want comms LEDs in this version?
#define COMMS_LEDS
#define COMMS_LED_TX_LINK 3
#define COMMS_LED_TX_FACE -1
#define COMMS_LED_RX_LINK 2
#define COMMS_LED_RX_FACE -1
//#define COMMS_LED_STATUS_LINK 0
//#define COMMS_LED_STATUS_FACE 4

//---------- or can find them by dynamically searching prim names
//#define COMMS_LED_TX_PRIMNAME
//#define COMMS_LED_RX_PRIMNAME

//---------- lots of verbose stuff
#define COMMS_STABLE_TIMER 60.0
//#define COMMS_DEBUG
//#define OUTPUT_SAY_DEBUG
//#define OUTPUT_SAY
#define OUTPUT_SAY_OWNER
//#define OUTPUT_SETTEXT

#define COMMS_MANUAL_START // then call comms_start();


#include "SL/LSL/Comms/Core.lsl"
