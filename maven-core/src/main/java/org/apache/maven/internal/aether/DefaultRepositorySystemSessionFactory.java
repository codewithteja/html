/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.aether;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.ModelBase;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.sisu.Nullable;

/**
 * @since 3.3.0
 */
@Named
public class DefaultRepositorySystemSessionFactory {
    private static final String MAVEN_RESOLVER_TRANSPORT_KEY = "maven.resolver.transport";

    private static final String MAVEN_RESOLVER_TRANSPORT_DEFAULT = "default";

    private static final String MAVEN_RESOLVER_TRANSPORT_WAGON = "wagon";

    private static final String MAVEN_RESOLVER_TRANSPORT_NATIVE = "native";

    private static final String MAVEN_RESOLVER_TRANSPORT_AUTO = "auto";

    private static final String WAGON_TRANSPORTER_PRIORITY_KEY = "aether.priority.WagonTransporterFactory";

    private static final String NATIVE_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.HttpTransporterFactory";

    private static final String NATIVE_FILE_TRANSPORTER_PRIORITY_KEY = "aether.priority.FileTransporterFactory";

    private static final String RESOLVER_MAX_PRIORITY = String.valueOf(Float.MAX_VALUE);

    @Inject
    private Logger logger;

    @Inject
    private ArtifactHandlerManager artifactHandlerManager;

    @Inject
    private RepositorySystem repoSystem;

    @Inject
    @Nullable
    @Named("simple")
    private LocalRepositoryManagerFactory simpleLocalRepoMgrFactory;

    @Inject
    @Nullable
    @Named("ide")
    private WorkspaceReader workspaceRepository;

    @Inject
    private SettingsDecrypter settingsDecrypter;

    @Inject
    private EventSpyDispatcher eventSpyDispatcher;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    @Inject
    private RuntimeInformation runtimeInformation;

    @SuppressWarnings("checkstyle:methodlength")
    public DefaultRepositorySystemSession newRepositorySession(MavenExecutionRequest request) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setCache(request.getRepositoryCache());

        Map<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, request.isInteractiveMode());
        configProps.put("maven.startTime", request.getStartTime());
        // First add properties populated from settings.xml
        configProps.putAll(getPropertiesFromRequestedProfiles(request));
        // Resolver's ConfigUtils solely rely on config properties, that is why we need to add both here as well.
        configProps.putAll(request.getSystemProperties());
        configProps.putAll(request.getUserProperties());

        session.setOffline(request.isOffline());
        session.setChecksumPolicy(request.getGlobalChecksumPolicy());
        if (request.isNoSnapshotUpdates()) {
            session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
        } else if (request.isUpdateSnapshots()) {
            session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        } else {
            session.setUpdatePolicy(null);
        }

        int errorPolicy = 0;
        errorPolicy |= request.isCacheNotFound()
                ? ResolutionErrorPolicy.CACHE_NOT_FOUND
                : ResolutionErrorPolicy.CACHE_DISABLED;
        errorPolicy |= request.isCacheTransferError()
                ? ResolutionErrorPolicy.CACHE_TRANSFER_ERROR
                : ResolutionErrorPolicy.CACHE_DISABLED;
        session.setResolutionErrorPolicy(
                new SimpleResolutionErrorPolicy(errorPolicy, errorPolicy | ResolutionErrorPolicy.CACHE_NOT_FOUND));

        session.setArtifactTypeRegistry(RepositoryUtils.newArtifactTypeRegistry(artifactHandlerManager));

        if (request.getWorkspaceReader() != null) {
            session.setWorkspaceReader(request.getWorkspaceReader());
        } else {
            session.setWorkspaceReader(workspaceRepository);
        }

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(request.getProxies());
        decrypt.setServers(request.getServers());
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt(decrypt);

        if (logger.isDebugEnabled()) {
            for (SettingsProblem problem : decrypted.getProblems()) {
                logger.debug(problem.getMessage(), problem.getException());
            }
        }

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : request.getMirrors()) {
            mirrorSelector.add(
                    mirror.getId(),
                    mirror.getUrl(),
                    mirror.getLayout(),
                    false,
                    mirror.isBlocked(),
                    mirror.getMirrorOf(),
                    mirror.getMirrorOfLayouts());
        }
        session.setMirrorSelector(mirrorSelector);

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (Proxy proxy : decrypted.getProxies()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            proxySelector.add(
                    new org.eclipse.aether.repository.Proxy(
                            proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                    proxy.getNonProxyHosts());
        }
        session.setProxySelector(proxySelector);

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : decrypted.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());

            if (server.getConfiguration() != null) {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for (int i = dom.getChildCount() - 1; i >= 0; i--) {
                    Xpp3Dom child = dom.getChild(i);
                    if ("wagonProvider".equals(child.getName())) {
                        dom.removeChild(i);
                    }
                }

                XmlPlexusConfiguration config = new XmlPlexusConfiguration(dom);
                configProps.put("aether.connector.wagon.config." + server.getId(), config);
            }

            configProps.put("aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
        session.setAuthenticationSelector(authSelector);

        Object transport = configProps.getOrDefault(MAVEN_RESOLVER_TRANSPORT_KEY, MAVEN_RESOLVER_TRANSPORT_DEFAULT);
        if (MAVEN_RESOLVER_TRANSPORT_DEFAULT.equals(transport)) {
            // The "default" mode (user did not set anything) needs to tweak resolver default priorities
            // that are coded like this (default values):
            //
            // org.eclipse.aether.transport.http.HttpTransporterFactory.priority = 5.0f;
            // org.eclipse.aether.transport.wagon.WagonTransporterFactory.priority = -1.0f;
            //
            // Hence, as both are present on classpath, HttpTransport would be selected, while
            // we want to retain "default" behaviour of Maven and use Wagon. To achieve that,
            // we set explicitly priority of WagonTransport to 6.0f (just above of HttpTransport),
            // to make it "win" over HttpTransport. We do this to NOT interfere with possibly
            // installed OTHER transports and their priorities, as unlike "wagon" or "native"
            // transport setting, that sets priorities to MAX, hence prevents any 3rd party
            // transport to get into play (inhibits them), in default mode we want to retain
            // old behavior. Also, this "default" mode is different from "auto" setting,
            // as it does not alter resolver priorities at all, and uses priorities as is.

            configProps.put(WAGON_TRANSPORTER_PRIORITY_KEY, "6");
        } else if (MAVEN_RESOLVER_TRANSPORT_NATIVE.equals(transport)) {
            // Make sure (whatever extra priority is set) that resolver native is selected
            configProps.put(NATIVE_FILE_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
            configProps.put(NATIVE_HTTP_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (MAVEN_RESOLVER_TRANSPORT_WAGON.equals(transport)) {
            // Make sure (whatever extra priority is set) that wagon is selected
            configProps.put(WAGON_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (!MAVEN_RESOLVER_TRANSPORT_AUTO.equals(transport)) {
            throw new IllegalArgumentException("Unknown resolver transport '" + transport
                    + "'. Supported transports are: " + MAVEN_RESOLVER_TRANSPORT_WAGON + ", "
                    + MAVEN_RESOLVER_TRANSPORT_NATIVE + ", " + MAVEN_RESOLVER_TRANSPORT_AUTO);
        }

        session.setUserProperties(request.getUserProperties());
        session.setSystemProperties(request.getSystemProperties());
        session.setConfigProperties(configProps);

        session.setTransferListener(request.getTransferListener());

        session.setRepositoryListener(eventSpyDispatcher.chainListener(new LoggingRepositoryListener(logger)));

        mavenRepositorySystem.injectMirror(request.getRemoteRepositories(), request.getMirrors());
        mavenRepositorySystem.injectProxy(session, request.getRemoteRepositories());
        mavenRepositorySystem.injectAuthentication(session, request.getRemoteRepositories());

        mavenRepositorySystem.injectMirror(request.getPluginArtifactRepositories(), request.getMirrors());
        mavenRepositorySystem.injectProxy(session, request.getPluginArtifactRepositories());
        mavenRepositorySystem.injectAuthentication(session, request.getPluginArtifactRepositories());

        setUpLocalRepositoryManager(request, session);

        return session;
    }

    private void setUpLocalRepositoryManager(MavenExecutionRequest request, DefaultRepositorySystemSession session) {
        LocalRepository localRepo =
                new LocalRepository(request.getLocalRepository().getBasedir());

        if (request.isUseLegacyLocalRepository()) {
            try {
                session.setLocalRepositoryManager(simpleLocalRepoMgrFactory.newInstance(session, localRepo));
                logger.info("Disabling enhanced local repository: using legacy is strongly discouraged to ensure"
                        + " build reproducibility.");
            } catch (NoLocalRepositoryManagerException e) {
                logger.error("Failed to configure legacy local repository: falling back to default");
                session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
            }
        } else {
            session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
        }
    }

    private Map<?, ?> getPropertiesFromRequestedProfiles(MavenExecutionRequest request) {

        List<String> activeProfileId = request.getActiveProfiles();

        return request.getProfiles().stream()
                .filter(profile -> activeProfileId.contains(profile.getId()))
                .map(ModelBase::getProperties)
                .flatMap(properties -> properties.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k2));
    }

    private String getUserAgent() {
        String version = runtimeInformation.getMavenVersion();
        version = version.isEmpty() ? version : "/" + version;
        return "Apache-Maven" + version + " (Java " + System.getProperty("java.version") + "; "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
    }
}
