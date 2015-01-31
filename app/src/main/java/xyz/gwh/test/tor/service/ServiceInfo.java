package xyz.gwh.test.tor.service;

import java.io.File;

/**
 * Immutable configuration object, resolves dependencies for service components.
 */
public final class ServiceInfo {

    public final int httpPort;
    public final int dnsPort;
    public final int socksPort;
    public final int proxyPort;

    public final File fileTor;
    public final File fileControlPort;
    public final File fileAuthCookie;
    public final File filePolipo;
    public final File filePolipoConfig;
    public final File dirCache;

    private ServiceInfo(Builder builder) {
        this.httpPort = builder.httpPort;
        this.dnsPort = builder.dnsPort;
        this.socksPort = builder.socksPort;
        this.proxyPort = builder.proxyPort;

        this.fileTor = builder.fileTor;
        this.fileControlPort = builder.fileControlPort;
        this.fileAuthCookie = builder.fileAuthCookie;
        this.filePolipo = builder.filePolipo;
        this.filePolipoConfig = builder.filePolipoConfig;
        this.dirCache = builder.dirCache;
    }

    public static class Builder {

        private int httpPort;
        private int dnsPort;
        private int socksPort;
        private int proxyPort;

        private File fileTor;
        private File fileControlPort;
        private File fileAuthCookie;
        private File filePolipo;
        private File filePolipoConfig;
        private File dirCache;

        public Builder() {
            // default empty Builder
        }

        public Builder setHttpPort(int httpPort) {
            this.httpPort = httpPort;
            return this;
        }

        public Builder setDnsPort(int dnsPort) {
            this.dnsPort = dnsPort;
            return this;
        }

        public Builder setSocksPort(int socksPort) {
            this.socksPort = socksPort;
            return this;
        }

        public Builder setProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public Builder setFileTor(File fileTor) {
            this.fileTor = fileTor;
            return this;
        }

        public Builder setFileControlPort(File fileControlPort) {
            this.fileControlPort = fileControlPort;
            return this;
        }

        public Builder setFileAuthCookie(File fileauthCookie) {
            this.fileAuthCookie = fileauthCookie;
            return this;
        }

        public Builder setFilePolipo(File filePolipo) {
            this.filePolipo = filePolipo;
            return this;
        }

        public Builder setFilePolipoConfig(File filePolipoConfig) {
            this.filePolipoConfig = filePolipoConfig;
            return this;
        }

        public Builder setDirCache(File dirCache) {
            this.dirCache = dirCache;
            return this;
        }

        public ServiceInfo build() {
            return new ServiceInfo(this);
        }
    }
}