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

import static java.util.Objects.requireNonNull;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.sisu.EagerSingleton;

/**
 * Maven internal component that bridges container "shut down" and "on session end" to {@link RepositorySystem).
 *
 * @since 3.9.0
 */
@Named
@EagerSingleton
public final class ResolverLifecycle extends AbstractMavenLifecycleParticipant {
    private final Provider<RepositorySystem> repositorySystemProvider;

    @Inject
    public ResolverLifecycle(Provider<RepositorySystem> repositorySystemProvider) {
        this.repositorySystemProvider = requireNonNull(repositorySystemProvider);
    }

    @Override
    public void afterSessionStart(MavenSession session) {
        repositorySystemProvider.get().sessionStarted(session.getRepositorySession());
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        repositorySystemProvider.get().sessionEnded(session.getRepositorySession());
    }

    @PreDestroy
    public void shutdown() {
        repositorySystemProvider.get().shutdown();
    }
}
