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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;

/**
 * A dependency's {@code Type} represents a known kind of dependencies.
 * Such types are often associated to an extension and possibly
 * a classifier, for example {@code java-source} has a {@code jar}
 * extension and a {@code sources} classifier.
 * It is also used to determine if a given dependency should be
 * included in the classpath or if its transitive dependencies should.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Type {

    String POM = "pom";
    String JAR = "jar";
    String JAVA_SOURCE = "java-source";
    String JAVADOC = "javadoc";
    String MAVEN_PLUGIN = "maven-plugin";
    String TEST_JAR = "test-jar";

    /**
     * Returns the dependency type name.
     *
     * @return the type name
     */
    String getName();

    /**
     * Get the file extension associated to the file represented by the dependency type.
     *
     * @return the file extension
     */
    String getExtension();

    /**
     * Get the classifier associated to the dependency type.
     *
     * @return the classifier
     */
    String getClassifier();

    boolean isIncludesDependencies();

    boolean isAddedToClasspath();
}
