/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.data.services.impls.data.mappers

import io.airbyte.data.repositories.entities.PartialUserConfig

typealias EntityPartialUserConfig = PartialUserConfig
typealias ModelPartialUserConfig = io.airbyte.config.PartialUserConfig

fun EntityPartialUserConfig.toConfigModel(): ModelPartialUserConfig {
  this.id ?: throw IllegalStateException("Cannot map Partial User Config entity that lacks an id")
  return ModelPartialUserConfig(
    id = this.id!!,
    workspaceId = this.workspaceId,
    configTemplateId = this.configTemplateId,
    partialUserConfigProperties = this.partialUserConfigProperties,
  )
}

fun ModelPartialUserConfig.toEntity(): EntityPartialUserConfig =
  EntityPartialUserConfig(
    id = this.id,
    workspaceId = this.workspaceId,
    configTemplateId = this.configTemplateId,
    partialUserConfigProperties = this.partialUserConfigProperties,
  )
