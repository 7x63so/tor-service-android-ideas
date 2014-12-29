/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */
package xyz.gwh.test.tor;

public class TorServiceConstants {

	public static final String TOR_APP_USERNAME = "org.torproject.android";
	public static final String ORWEB_APP_USERNAME = "info.guardianproject.browser";
	
	public static final String DIRECTORY_TOR_BINARY = "bin";
	public static final String DIRECTORY_TOR_DATA = "data";
	
	//name of the tor C binary
	public static final String TOR_ASSET_KEY = "tor";
	
	//torrc (tor config file)
	public static final String TORRC_ASSET_KEY = "torrc";
	public static final String TORRCDIAG_ASSET_KEY = "torrcdiag";
	public static final String TORRC_TETHER_KEY = "torrctether";
	
	public static final String TOR_CONTROL_COOKIE = "control_auth_cookie";
	
	//privoxy
	public static final String POLIPO_ASSET_KEY = "polipo";
	
	//privoxy.config
	public static final String POLIPOCONFIG_ASSET_KEY = "torpolipo.conf";
	
	//geoip data file asset key
	public static final String GEOIP_ASSET_KEY = "geoip";
	public static final String GEOIP6_ASSET_KEY = "geoip6";
	
	
	//various console cmds
	public static final String SHELL_CMD_CHMOD = "chmod";
	public static final String SHELL_CMD_KILL = "kill -9";
	public static final String SHELL_CMD_RM = "rm";
	public static final String SHELL_CMD_PS = "toolbox ps";
	public static final String SHELL_CMD_PS_ALT = "ps";
	
	//public static final String SHELL_CMD_PIDOF = "pidof";
	public static final String SHELL_CMD_LINK = "ln -s";
	public static final String SHELL_CMD_CP = "cp";
	

	public static final String CHMOD_EXE_VALUE = "770";

	public static final int FILE_WRITE_BUFFER_SIZE = 1024;
	
	//HTTP Proxy server port
	public static int PORT_HTTP = 8118; //just like Privoxy!
	
	//Socks port client connects to, server is the Tor binary
	public static String PORT_SOCKS_DEFAULT = "9050";
	
	
	//what is says!
	public static final String IP_LOCALHOST = "127.0.0.1";
	public static final int UPDATE_TIMEOUT = 1000;
	public static final int TOR_TRANSPROXY_PORT_DEFAULT = 9040;
	public static final int STANDARD_DNS_PORT = 53;
	public static final int TOR_DNS_PORT_DEFAULT = 5400;
	
	//path to check Tor against
	public static final String URL_TOR_CHECK = "https://check.torproject.org";

    //control port 
    public static final String TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE = "Bootstrapped 100%";
    
    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;
    public static final int STATUS_CONNECTING = 2;
    
    public static final int STATUS_MSG = 1;
    public static final int ENABLE_TOR_MSG = 2;
    public static final int DISABLE_TOR_MSG = 3;
    public static final int LOG_MSG = 4;
    
    public static final String CMD_START = "start";
    public static final String CMD_STOP = "stop";
    public static final String CMD_FLUSH = "flush";
    public static final String CMD_NEWNYM = "newnym";
    public static final String CMD_INIT = "init";
    public static final String CMD_VPN = "vpn";
    public static final String CMD_UPDATE = "update";

    public static final String BINARY_TOR_VERSION = "0.2.5.10-openssl1.0.1i-nonPIE-polipofix";
    public static final String PREF_BINARY_TOR_VERSION_INSTALLED = "BINARY_TOR_VERSION_INSTALLED";
    
    //obfsproxy 
    public static final String OBFSCLIENT_ASSET_KEY = "obfsclient";
    
	public static final int MESSAGE_TRAFFIC_COUNT = 5;
	

	//name of the iptables binary
	public static final String IPTABLES_ASSET_KEY = "xtables";
	
	public static final int DEFAULT_CONTROL_PORT = 9051;
	

}
