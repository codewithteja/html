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
package org.apache.maven.plugin.internal;

import org.apache.maven.internal.impl.InternalSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.version.VersionSchemeSelector;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MavenPluginJavaPrerequisiteCheckerTest {

    @Test
    void testMatchesVersion() {
        RepositorySystemSession repositorySystemSession = mock(RepositorySystemSession.class);
        InternalSession internalSession = mock(InternalSession.class);
        when(internalSession.getSession()).thenReturn(repositorySystemSession);
        VersionSchemeSelector schemeSelector = mock(VersionSchemeSelector.class);
        when(schemeSelector.selectVersionScheme(any(RepositorySystemSession.class)))
                .thenReturn(new GenericVersionScheme());

        MavenPluginJavaPrerequisiteChecker checker =
                new MavenPluginJavaPrerequisiteChecker(() -> internalSession, schemeSelector);
        assertTrue(checker.matchesVersion("1.0", "1.8"));
        assertTrue(checker.matchesVersion("1.8", "9.0.1+11"));
        assertFalse(checker.matchesVersion("[1.0,2],[3,4]", "2.1"));
        assertTrue(checker.matchesVersion("[1.0,2],[3,4]", "3.1"));
        assertThrows(IllegalArgumentException.class, () -> checker.matchesVersion("(1.0,0)", "11"));
    }
}
