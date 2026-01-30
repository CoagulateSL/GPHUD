local comms = require("../../SLCore/SLua/Comms")
local dev = require("../../SLCore/SLua/Dev")
local slcore = require("../../SLCore/SLua/SecondLifeConstants")
local gphud = require("./Constants")

local stage:number = 0
-- stages: 0=boot/claimURL -- probably useless

local function setshard(k:string)
    ll.SetLinkPrimitiveParamsFast(LINK_THIS,{PRIM_TEXTURE,0,k,vector(1,1,1),vector(0,0,0),0})
end
local function setlogo(k:string)
    ll.SetLinkPrimitiveParamsFast(LINK_THIS,{PRIM_TEXTURE,1,k,vector(1,1,1),vector(0,0,0),0})
end

local function comms_ready()
    ll.OwnerSay("Comms is online")
    gphud.banner_server()
    -- listen on broadcast and 0
    comms.httpcommand({version=gphud.VERSION,versiondate=gphud.COMPILEDATE,versiontime=gphud.COMPILETIME},"gphudserver.register","GPHUD/system")
end

local function process(t:table)
    local command:string=t.incommand
    local othercommand:string=t.command
    if (command=="registered") then
        ll.OwnerSay("Startup Complete!")
        -- do other stuff here
    end
    if (command=="broadcast") then
        comms.broadcast(t)
    end
    if (command=="disseminate") then
        --for _,v in t do
        --end
        error("Disseminate isn't implemented yet, teehee")
    end
    if (command=="messageto") then
        ll.RegionSayTo(t.target,0,t.sendthismessage)
    end
    if (command=="servergive") then
        ll.GiveInventory(t.giveto,t.itemname)
    end
    if (t.instancestatus) then
        ll.SetText(t.instancestatus.." V"..gphud.VERSION.."\n \n \n \n \n",tovector(t.statuscolor),1)
    end
    if (t.instancename) then
        name=t.instancename
        ll.SetObjectName(name.." GPHUD Region Server")
    end
    if(t.setlogo) then
        setlogo(t.setlogo)
    end
    if (t.rebootserver) then
        ll.ResetScript()
    end
    if (t.instantmessage) then
        ll.MessageLinked(LINK_THIS,gphud.LINK_INSTANT, Number, Text, ID)
    end
    if (t.error) then
        ll.Say(0,"ERROR: "..t.error)
    end
end

local function setup()
    local logo=slcore.SLCORE_COAGULATE_LOGO
    dev.announce()
    if (dev.dev()) then
        logo=slcore.SLCORE_COAGULATE_DEV_LOGO
    end
    setlogo(logo)
    setshard(logo)
    ll.SetObjectName("GPHUD Region Server")
    comms.setup(comms_ready,process)
end

ll.SetText("Rebooting",vector(1,.5,.5),1)
setup()

ll.OwnerSay("Boot complete")