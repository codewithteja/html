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
package org.apache.maven.cli;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.DefaultArtifactCoordinate;
import org.apache.maven.internal.impl.DefaultSessionFactory;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.resolver.RepositorySystemSessionFactory;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenStatusCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenStatusCommand.class);

    /**
     * In order to verify artifacts can be downloaded from the remote repositories we want to resolve an actual
     * artifact. The Apache Maven artifact was chosen as it eventually, be it by proxy, mirror or directly, will be
     * gathered from the central repository. The version is chosen arbitrarily since any listed should work.
     */
    public static final Artifact APACHE_MAVEN_ARTIFACT =
            new DefaultArtifact("org.apache.maven", "apache-maven", null, "pom", "3.8.6");

    private final MavenExecutionRequestPopulator mavenExecutionRequestPopulator;
    private final ArtifactResolver artifactResolver;
    private final RemoteRepositoryConnectionVerifier remoteRepositoryConnectionVerifier;
    private final DefaultSessionFactory defaultSessionFactory;
    private final RepositorySystemSessionFactory repoSession;
    private final MavenRepositorySystem repositorySystem;
    private final SessionScope sessionScope;
    private Path tempLocalRepository;

    public MavenStatusCommand(final PlexusContainer container) throws ComponentLookupException {
        this.remoteRepositoryConnectionVerifier = new RemoteRepositoryConnectionVerifier(container);
        this.mavenExecutionRequestPopulator = container.lookup(MavenExecutionRequestPopulator.class);
        this.artifactResolver = container.lookup(ArtifactResolver.class);
        this.defaultSessionFactory = container.lookup(DefaultSessionFactory.class);
        this.repoSession = container.lookup(RepositorySystemSessionFactory.class);
        this.sessionScope = container.lookup(SessionScope.class);
        this.repositorySystem = container.lookup(MavenRepositorySystem.class);
    }

    public List<String> verify(final MavenExecutionRequest cliRequest) throws MavenExecutionRequestPopulationException {
        final MavenExecutionRequest mavenExecutionRequest = mavenExecutionRequestPopulator.populateDefaults(cliRequest);

        final ArtifactRepository localRepository = cliRequest.getLocalRepository();

        final List<String> localRepositoryIssues =
                verifyLocalRepository(Paths.get(URI.create(localRepository.getUrl())));

        // We overwrite the local repository with a temporary directory to avoid using a cached version of the artifact.
        setTemporaryLocalRepositoryPathOnRequest(cliRequest);

        final List<String> remoteRepositoryIssues =
                verifyRemoteRepositoryConnections(cliRequest.getRemoteRepositories(), mavenExecutionRequest);
        final List<String> artifactResolutionIssues = verifyArtifactResolution(mavenExecutionRequest);

        cleanupTempFiles();

        // Collect all issues into a single list
        return Stream.of(localRepositoryIssues, remoteRepositoryIssues, artifactResolutionIssues)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private void cleanupTempFiles() {
        if (tempLocalRepository != null) {
            try (Stream<Path> files = Files.walk(tempLocalRepository)) {
                files.sorted(Comparator.reverseOrder()) // Sort in reverse order so that directories are deleted last
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ioe) {
                LOGGER.debug("Failed to delete temporary local repository", ioe);
            }
        }
    }

    private void setTemporaryLocalRepositoryPathOnRequest(final MavenExecutionRequest request) {
        try {
            tempLocalRepository = Files.createTempDirectory("mvn-status").toAbsolutePath();
            request.setLocalRepositoryPath(tempLocalRepository.toString());
            request.setLocalRepository(repositorySystem.createLocalRepository(request, tempLocalRepository.toFile()));
        } catch (Exception ex) {
            LOGGER.debug("Could not create temporary local repository", ex);
            LOGGER.warn("Artifact resolution test is less accurate as it may use earlier resolution results.");
        }
    }

    private List<String> verifyRemoteRepositoryConnections(
            final List<ArtifactRepository> remoteRepositories, final MavenExecutionRequest mavenExecutionRequest) {
        final List<String> issues = new ArrayList<>();

        for (ArtifactRepository remoteRepository : remoteRepositories) {
            try (RepositorySystemSession.CloseableSession repositorySession = repoSession
                    .newRepositorySessionBuilder(mavenExecutionRequest)
                    .build()) {
                remoteRepositoryConnectionVerifier
                        .verifyConnectionToRemoteRepository(repositorySession, remoteRepository)
                        .ifPresent(issues::add);
            }
        }

        return issues;
    }

    private List<String> verifyArtifactResolution(final MavenExecutionRequest mavenExecutionRequest) {
        this.sessionScope.enter();
        try (RepositorySystemSession.CloseableSession repoSession = this.repoSession
                .newRepositorySessionBuilder(mavenExecutionRequest)
                .build()) {
            final Session session = this.defaultSessionFactory.newSession(
                    new MavenSession(repoSession, mavenExecutionRequest, new DefaultMavenExecutionResult()));
            InternalMavenSession internalSession = InternalMavenSession.from(session);
            sessionScope.seed(InternalMavenSession.class, internalSession);
            ArtifactCoordinate artifactCoordinate =
                    new DefaultArtifactCoordinate(InternalSession.from(session), APACHE_MAVEN_ARTIFACT);
            ArtifactResolverResult resolverResult =
                    artifactResolver.resolve(session, Collections.singleton(artifactCoordinate));
            resolverResult
                    .getArtifacts()
                    .forEach((key, value) -> LOGGER.debug("Successfully resolved {} to {}", key, value));

            return Collections.emptyList();
        } catch (ArtifactResolverException are) {
            return extractIssuesFromArtifactResolverException(are);
        } finally {
            this.sessionScope.exit();
            LOGGER.info("Artifact resolution check completed");
        }
    }

    private List<String> extractIssuesFromArtifactResolverException(final Exception exception) {
        final boolean isArtifactResolutionException = exception.getCause() instanceof ArtifactResolutionException;
        if (isArtifactResolutionException) {
            final ArtifactResolutionException are = (ArtifactResolutionException) exception.getCause();
            return are.getResults().stream()
                    .map(ArtifactResult::getExceptions)
                    .flatMap(List::stream)
                    .map(Throwable::getMessage)
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(exception.getMessage());
        }
    }

    private List<String> verifyLocalRepository(final Path localRepositoryPath) {
        final List<String> issues = new ArrayList<>();

        if (!Files.isDirectory(localRepositoryPath)) {
            issues.add(String.format("Local repository path '%s' is not a directory.", localRepositoryPath));
        }

        if (!Files.isReadable(localRepositoryPath)) {
            issues.add(String.format("No read permissions on local repository '%s'.", localRepositoryPath));
        }

        if (!Files.isWritable(localRepositoryPath)) {
            issues.add(String.format("No write permissions on local repository '%s'.", localRepositoryPath));
        }

        LOGGER.info("Local repository setup check completed");
        return issues;
    }
}
