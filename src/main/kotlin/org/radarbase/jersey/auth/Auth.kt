/*
 * Copyright (c) 2019. The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * See the file LICENSE in the root of this repository.
 */

package org.radarbase.jersey.auth

import com.fasterxml.jackson.databind.JsonNode
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpForbiddenException
import org.radarcns.auth.authorization.Permission
import org.radarcns.auth.token.RadarToken

interface Auth {
    /** ID of the OAuth client. */
    // TODO: parse client ID from RADAR token. Pending MP 0.5.7.
    val clientId: String?
        get() = getClaim("client_id").let { if (it.isTextual) it.asText() else null }
    /** Default project to apply operations to. */
    val defaultProject: String?

    val token: RadarToken

    /** User ID, if set in the authentication. This may be null if a client credentials grant type is used. */
    val userId: String?
        get() = token.subject?.takeUnless { it.isEmpty() }

    /**
     * Check whether the current authentication has given permissions on a subject in a project.
     *
     * @throws HttpBadRequestException if a parameter is null
     * @throws HttpForbiddenException if the current authentication does not authorize for the permission.
     */
    fun checkPermissionOnSubject(permission: Permission, projectId: String?, userId: String?) {
        if (!token.hasPermissionOnSubject(permission,
                        projectId ?: throw HttpBadRequestException("project_id_missing", "Missing project ID in request"),
                        userId ?: throw HttpBadRequestException("user_id_missing", "Missing user ID in request")
                        )) {
            throw HttpForbiddenException("permission_mismatch", "No permission to create measurement for " +
                    "project $projectId with user $userId")
        }
    }

    /**
     * Check whether the current authentication has given permissions on a project.
     *
     * @throws HttpBadRequestException if a parameter is null
     * @throws HttpForbiddenException if the current authentication does not authorize for the permission.
     */
    fun checkPermissionOnProject(permission: Permission, projectId: String?) {
        if (!token.hasPermissionOnProject(permission,
                        projectId ?: throw HttpBadRequestException("project_id_missing", "Missing project ID in request")
                        )) {
            throw HttpForbiddenException("permission_mismatch", "No permission to create measurement for " +
                    "project $projectId")
        }
    }

    /**
     * Check whether the current authentication has given permissions.
     *
     * @throws HttpBadRequestException if a parameter is null
     * @throws HttpForbiddenException if the current authentication does not authorize for the permission.
     */
    fun checkPermissionOnSource(permission: Permission, projectId: String?, userId: String?, sourceId: String?) {
        if (!token.hasPermissionOnSource(permission,
                        projectId ?: throw HttpBadRequestException("project_id_missing", "Missing project ID in request"),
                        userId ?: throw HttpBadRequestException("user_id_missing", "Missing user ID in request"),
                        sourceId ?: throw HttpBadRequestException("source_id_missing", "Missing source ID in request"))) {
            throw HttpForbiddenException("permission_mismatch", "No permission to create measurement for " +
                    "project $projectId with user $userId and source $sourceId")
        }
    }

    /**
     * Get a claim from the token used for this authentication.
     */
    fun getClaim(name: String): JsonNode

    /**
     * Whether the current authentication is for a user with a role in given project.
     */
    fun hasRole(projectId: String, role: String): Boolean
}
