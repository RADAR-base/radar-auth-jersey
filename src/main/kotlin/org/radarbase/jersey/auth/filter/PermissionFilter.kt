/*
 * Copyright (c) 2019. The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * See the file LICENSE in the root of this repository.
 */

package org.radarbase.jersey.auth.filter

import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.auth.ProjectService
import org.radarbase.jersey.exception.HttpForbiddenException
import org.radarcns.auth.authorization.Permission
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.Context
import javax.ws.rs.core.UriInfo

/**
 * Check that the token has given permissions.
 */
class PermissionFilter(
        @Context private val resourceInfo: ResourceInfo,
        @Context private val auth: Auth,
        @Context private val projectService: ProjectService,
        @Context private val uriInfo: UriInfo
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val resourceMethod = resourceInfo.resourceMethod

        val annotation = resourceMethod.getAnnotation(NeedsPermission::class.java)

        val permission = Permission(annotation.entity, annotation.operation)

        val projectId = annotation.projectPathParam
                .takeIf { it.isNotEmpty() }
                ?.let { uriInfo.pathParameters[it] }
                ?.firstOrNull()
        val userId = annotation.userPathParam
                .takeIf { it.isNotEmpty() }
                ?.let { uriInfo.pathParameters[it] }
                ?.firstOrNull()

        val isAuthorized = when {
            userId != null -> projectId != null && auth.token.hasPermissionOnSubject(permission, projectId, userId)
            projectId != null -> auth.token.hasPermissionOnProject(permission, projectId)
            else -> auth.token.hasPermission(permission)
        }

        auth.logPermission(isAuthorized, permission, projectId, userId)

        if (!isAuthorized) {
            val message = "$permission permission not given."
            throw HttpForbiddenException("insufficient_scope", message, additionalHeaders = listOf(
                    "WWW-Authenticate" to (AuthenticationFilter.BEARER_REALM
                    + " error=\"insufficient_scope\""
                    + " error_description=\"$message\""
                    + " scope=\"$permission\"")))
        }
        projectId?.let { projectService.ensureProject(it) }
    }
}
