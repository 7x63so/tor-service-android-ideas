package xyz.gwh.test.tor.controller;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class TorifiedApp implements Comparable {

    private boolean enabled;
    private int uid;
    private String username;
    private String procname;
    private String name;
    private Drawable icon;

    private int[] enabledPorts;
    private boolean torified = false;
    private boolean usesInternet = false;

    public boolean usesInternet() {
        return usesInternet;
    }

    public void setUsesInternet(boolean usesInternet) {
        this.usesInternet = usesInternet;
    }

    public boolean isTorified() {
        return torified;
    }

    public void setTorified(boolean torified) {
        this.torified = torified;
    }

    public int[] getEnabledPorts() {
        return enabledPorts;
    }

    public void setEnabledPorts(int[] enabledPorts) {
        this.enabledPorts = enabledPorts;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProcname() {
        return procname;
    }

    public void setProcname(String procname) {
        this.procname = procname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    @Override
    public int compareTo(@NonNull Object another) {
        return this.toString().compareTo(another.toString());
    }

    @Override
    public String toString() {
        return getName();
    }
}
