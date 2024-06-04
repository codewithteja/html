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
package org.apache.maven.api.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.maven.api.annotations.Experimental;

/**
 * Indicates that a given field will be injected with the result of
 * a dependency collection or resolution request. Whether a collection
 * or resolution request is performed is controlled by the {@link #pathScope()}
 * field.
 * <p>
 * If a collection request is to be done, the type of the field annotated with
 * this annotation can be either <ul>
 * <li>a {@link org.apache.maven.api.services.DependencyCollectorResult DependencyCollectorResult},</li>
 * <li>or a {@link org.apache.maven.api.Node Node} object.</li>
 * </ul>
 * <p>
 * If a resolution request is to be done, the type of the annotated field can be either <ul>
 * <li>a {@link org.apache.maven.api.services.DependencyResolverResult DependencyResolverResult},</li>
 * <li>a {@code List<}{@link org.apache.maven.api.Node Node}{@code >},</li>
 * <li>a {@code List<}{@link java.nio.file.Path Path}{@code >},</li>
 * <li>a {@code Map<}{@link org.apache.maven.api.PathType PathType}{@code , List<}{@link java.nio.file.Path Path}{@code >>},</li>
 * <li>or a {@code Map<}{@link org.apache.maven.api.Dependency Dependency}{@code , }{@link java.nio.file.Path Path}{@code >}.</li>
 * </ul>
 *
 * @since 4.0.0
 */
@Experimental
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Dependencies {

    /**
     * The id of a {@link org.apache.maven.api.PathScope} enum value.
     * If specified, a dependency resolution request will be issued,
     * else a dependency collection request will be done.
     *
     * @return the id of the path scope
     */
    String pathScope() default "";
}
