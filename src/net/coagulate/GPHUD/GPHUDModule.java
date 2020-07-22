package net.coagulate.GPHUD;

import net.coagulate.SL.Config;
import net.coagulate.SL.HTTPPipelines.PageMapper;
import net.coagulate.SL.SL;
import net.coagulate.SL.SLModule;

import javax.annotation.Nonnull;

public class GPHUDModule extends SLModule {
    @Nonnull
    @Override
    public String getName() {
        return "GPHUD";
    }

    @Override
    public String getDescription() {
        return "General Purpose Heads Up Display Toolkit";
    }

    @Override
    public void initialise() {

    }

    @Override
    public void maintenance() {
        if (nextRun("GPHUD-Maintenance",60)) { Maintenance.gphudMaintenance(); }
    }

    @Override
    public void startup() {
        GPHUD.initialiseAsModule(SL.DEV, Config.getGPHUDJdbc(), Config.getHostName());
        PageMapper.exact("/GPHUD/external",new net.coagulate.GPHUD.Interfaces.External.Interface());
        PageMapper.exact("/GPHUD/system",new net.coagulate.GPHUD.Interfaces.System.Interface());
        PageMapper.prefix("/GPHUD/",new net.coagulate.GPHUD.Interfaces.User.Interface());

    }

    @Override
    public void shutdown() {

    }
}
