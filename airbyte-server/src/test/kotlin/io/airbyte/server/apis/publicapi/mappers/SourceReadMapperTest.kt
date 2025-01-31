/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis.publicapi.mappers

import io.airbyte.api.model.generated.SourceRead
import io.airbyte.commons.json.Jsons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class SourceReadMapperTest {
  @Test
  fun `from should convert a SourceRead object from the config api to a SourceResponse`() {
    // Given
    val sourceRead =
      SourceRead().apply {
        this.sourceId = UUID.randomUUID()
        this.name = "sourceName"
        this.sourceDefinitionId = UUID.randomUUID()
        this.workspaceId = UUID.randomUUID()
        this.connectionConfiguration = Jsons.deserialize("{}")
        this.createdAt = 1L
      }

    // When
    val sourceResponse = SourceReadMapper.from(sourceRead)

    // Then
    assertEquals(sourceRead.sourceId.toString(), sourceResponse.sourceId)
    assertEquals(sourceRead.name, sourceResponse.name)
    assertEquals(DEFINITION_ID_TO_SOURCE_NAME.getOrDefault(sourceRead.sourceDefinitionId, ""), sourceResponse.sourceType)
    assertEquals(sourceRead.workspaceId.toString(), sourceResponse.workspaceId)
    assertEquals(sourceRead.connectionConfiguration, sourceResponse.configuration)
    assertEquals(sourceRead.sourceDefinitionId.toString(), sourceResponse.definitionId)
    assertEquals(sourceRead.createdAt, sourceResponse.createdAt)
  }
}
