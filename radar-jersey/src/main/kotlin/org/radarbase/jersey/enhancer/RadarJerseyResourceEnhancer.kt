/*
 * Copyright (c) 2019. The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * See the file LICENSE in the root of this repository.
 */

package org.radarbase.jersey.enhancer

import jakarta.inject.Singleton
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.jackson.JacksonFeature
import org.glassfish.jersey.process.internal.RequestScoped
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.AuthConfig
import org.radarbase.jersey.auth.filter.AuthenticationFilter
import org.radarbase.jersey.auth.filter.AuthorizationFeature
import org.radarbase.jersey.auth.jwt.AuthFactory

/**
 * Add RADAR auth to a Jersey project. This requires a {@link ProjectService} implementation to be
 * added to the Binder first.
 *
 * @param includeMapper is set, this also instantiates [MapperResourceEnhancer].
 * @param includeHttpClient is set, this also includes [OkHttpResourceEnhancer].
 */
class RadarJerseyResourceEnhancer(
    private val config: AuthConfig,
    includeMapper: Boolean = true,
    includeHttpClient: Boolean = true,
): JerseyResourceEnhancer {
    /**
     * Utilities. Set to `null` to avoid injection. Modify utility mapper or client to inject
     * a different mapper or client.
     */
    private val okHttpResourceEnhancer: OkHttpResourceEnhancer? = if (includeHttpClient) OkHttpResourceEnhancer() else null
    private val mapperResourceEnhancer: MapperResourceEnhancer? = if (includeMapper) MapperResourceEnhancer() else null

    override val classes = arrayOf(
        AuthenticationFilter::class.java,
        AuthorizationFeature::class.java,
    )

    override fun ResourceConfig.enhance() {
        register(JacksonFeature.withoutExceptionMappers())
        okHttpResourceEnhancer?.enhanceResources(this)
        mapperResourceEnhancer?.enhanceResources(this)
    }

    override fun AbstractBinder.enhance() {
        bind(config.withEnv())
            .to(AuthConfig::class.java)
            .`in`(Singleton::class.java)

        // Bind factories.
        bindFactory(AuthFactory::class.java)
            .proxy(true)
            .proxyForSameScope(true)
            .to(Auth::class.java)
            .`in`(RequestScoped::class.java)

        okHttpResourceEnhancer?.enhanceBinder(this)
        mapperResourceEnhancer?.enhanceBinder(this)
    }
}