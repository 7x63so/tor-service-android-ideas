package xyz.gwh.test.tor.controller;

import net.freehaven.tor.control.EventHandler;
import xyz.gwh.test.tor.resources.Torrc;

/**
 * Controller for jtorcontrol library.
 */
public interface TorController {

    public void startTor(Torrc torrc);

    public void stopTor();

    public void setConfig(Torrc torrc);

    public boolean isTorRunning();

    public void setEventHandler(EventHandler eventHandler);

    public void newIdentity();

}