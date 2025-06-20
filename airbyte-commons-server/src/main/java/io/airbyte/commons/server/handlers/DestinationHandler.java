/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.server.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import io.airbyte.api.model.generated.ActorDefinitionVersionBreakingChanges;
import io.airbyte.api.model.generated.ActorStatus;
import io.airbyte.api.model.generated.ConnectionRead;
import io.airbyte.api.model.generated.DestinationCreate;
import io.airbyte.api.model.generated.DestinationIdRequestBody;
import io.airbyte.api.model.generated.DestinationRead;
import io.airbyte.api.model.generated.DestinationReadList;
import io.airbyte.api.model.generated.DestinationSearch;
import io.airbyte.api.model.generated.DestinationSnippetRead;
import io.airbyte.api.model.generated.DestinationUpdate;
import io.airbyte.api.model.generated.ListResourcesForWorkspacesRequestBody;
import io.airbyte.api.model.generated.PartialDestinationUpdate;
import io.airbyte.api.model.generated.WorkspaceIdRequestBody;
import io.airbyte.commons.entitlements.Entitlement;
import io.airbyte.commons.entitlements.LicenseEntitlementChecker;
import io.airbyte.commons.json.Jsons;
import io.airbyte.commons.server.converters.ApiPojoConverters;
import io.airbyte.commons.server.converters.ConfigurationUpdate;
import io.airbyte.commons.server.errors.BadRequestException;
import io.airbyte.commons.server.handlers.helpers.ActorDefinitionHandlerHelper;
import io.airbyte.commons.server.handlers.helpers.OAuthSecretHelper;
import io.airbyte.config.ActorDefinitionVersion;
import io.airbyte.config.Configs;
import io.airbyte.config.DestinationConnection;
import io.airbyte.config.ScopeType;
import io.airbyte.config.SecretPersistenceConfig;
import io.airbyte.config.StandardDestinationDefinition;
import io.airbyte.config.persistence.ActorDefinitionVersionHelper;
import io.airbyte.config.persistence.ActorDefinitionVersionHelper.ActorDefinitionVersionWithOverrideStatus;
import io.airbyte.config.secrets.JsonSecretsProcessor;
import io.airbyte.config.secrets.SecretsRepositoryWriter;
import io.airbyte.config.secrets.persistence.RuntimeSecretPersistence;
import io.airbyte.data.exceptions.ConfigNotFoundException;
import io.airbyte.data.helpers.ActorDefinitionVersionUpdater;
import io.airbyte.data.services.DestinationService;
import io.airbyte.data.services.SecretPersistenceConfigService;
import io.airbyte.data.services.shared.ResourcesQueryPaginated;
import io.airbyte.featureflag.FeatureFlagClient;
import io.airbyte.featureflag.Organization;
import io.airbyte.featureflag.UseRuntimeSecretPersistence;
import io.airbyte.metrics.MetricClient;
import io.airbyte.persistence.job.WorkspaceHelper;
import io.airbyte.persistence.job.factory.OAuthConfigSupplier;
import io.airbyte.protocol.models.v0.ConnectorSpecification;
import io.airbyte.validation.json.JsonSchemaValidator;
import io.airbyte.validation.json.JsonValidationException;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * DestinationHandler. Javadocs suppressed because api docs should be used as source of truth.
 */
@SuppressWarnings("ParameterName")
@Singleton
public class DestinationHandler {

  private final ConnectionsHandler connectionsHandler;
  private final Supplier<UUID> uuidGenerator;
  private final JsonSchemaValidator validator;
  private final ConfigurationUpdate configurationUpdate;
  private final JsonSecretsProcessor secretsProcessor;
  private final OAuthConfigSupplier oAuthConfigSupplier;
  private final ActorDefinitionVersionHelper actorDefinitionVersionHelper;
  private final DestinationService destinationService;
  private final ActorDefinitionHandlerHelper actorDefinitionHandlerHelper;
  private final ActorDefinitionVersionUpdater actorDefinitionVersionUpdater;
  private final ApiPojoConverters apiPojoConverters;
  private final WorkspaceHelper workspaceHelper;
  private final LicenseEntitlementChecker licenseEntitlementChecker;
  private final Configs.AirbyteEdition airbyteEdition;
  private final FeatureFlagClient featureFlagClient;
  private final SecretsRepositoryWriter secretsRepositoryWriter;
  private final MetricClient metricClient;
  private final SecretPersistenceConfigService secretPersistenceConfigService;

  @VisibleForTesting
  public DestinationHandler(final JsonSchemaValidator integrationSchemaValidation,
                            final ConnectionsHandler connectionsHandler,
                            @Named("uuidGenerator") final Supplier<UUID> uuidGenerator,
                            @Named("jsonSecretsProcessorWithCopy") final JsonSecretsProcessor secretsProcessor,
                            final ConfigurationUpdate configurationUpdate,
                            final OAuthConfigSupplier oAuthConfigSupplier,
                            final ActorDefinitionVersionHelper actorDefinitionVersionHelper,
                            final DestinationService destinationService,
                            final ActorDefinitionHandlerHelper actorDefinitionHandlerHelper,
                            final ActorDefinitionVersionUpdater actorDefinitionVersionUpdater,
                            final ApiPojoConverters apiPojoConverters,
                            final WorkspaceHelper workspaceHelper,
                            final LicenseEntitlementChecker licenseEntitlementChecker,
                            final Configs.AirbyteEdition airbyteEdition,
                            final FeatureFlagClient featureFlagClient,
                            final SecretsRepositoryWriter secretsRepositoryWriter,
                            final MetricClient metricClient,
                            final SecretPersistenceConfigService secretPersistenceConfigService) {
    this.validator = integrationSchemaValidation;
    this.connectionsHandler = connectionsHandler;
    this.uuidGenerator = uuidGenerator;
    this.configurationUpdate = configurationUpdate;
    this.secretsProcessor = secretsProcessor;
    this.oAuthConfigSupplier = oAuthConfigSupplier;
    this.actorDefinitionVersionHelper = actorDefinitionVersionHelper;
    this.destinationService = destinationService;
    this.actorDefinitionHandlerHelper = actorDefinitionHandlerHelper;
    this.actorDefinitionVersionUpdater = actorDefinitionVersionUpdater;
    this.apiPojoConverters = apiPojoConverters;
    this.workspaceHelper = workspaceHelper;
    this.licenseEntitlementChecker = licenseEntitlementChecker;
    this.airbyteEdition = airbyteEdition;
    this.featureFlagClient = featureFlagClient;
    this.secretsRepositoryWriter = secretsRepositoryWriter;
    this.metricClient = metricClient;
    this.secretPersistenceConfigService = secretPersistenceConfigService;
  }

  public DestinationRead createDestination(final DestinationCreate destinationCreate)
      throws ConfigNotFoundException, IOException, JsonValidationException {
    if (destinationCreate.getResourceAllocation() != null && airbyteEdition == Configs.AirbyteEdition.CLOUD) {
      throw new BadRequestException(String.format("Setting resource allocation is not permitted on %s", airbyteEdition));
    }

    // validate configuration
    final ConnectorSpecification spec = getSpecForWorkspaceId(destinationCreate.getDestinationDefinitionId(), destinationCreate.getWorkspaceId());
    validateDestination(spec, destinationCreate.getConnectionConfiguration());

    // persist
    final UUID destinationId = uuidGenerator.get();
    persistDestinationConnection(
        destinationCreate.getName() != null ? destinationCreate.getName() : "default",
        destinationCreate.getDestinationDefinitionId(),
        destinationCreate.getWorkspaceId(),
        destinationId,
        destinationCreate.getConnectionConfiguration(),
        false,
        spec,
        destinationCreate.getResourceAllocation());

    // read configuration from db
    return buildDestinationRead(destinationService.getDestinationConnection(destinationId), spec);
  }

  public void deleteDestination(final DestinationIdRequestBody destinationIdRequestBody)
      throws JsonValidationException, IOException, ConfigNotFoundException, io.airbyte.config.persistence.ConfigNotFoundException {
    // get existing implementation
    final DestinationRead destination = buildDestinationRead(destinationIdRequestBody.getDestinationId());

    deleteDestination(destination);
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  public void deleteDestination(final DestinationRead destination)
      throws JsonValidationException, IOException, ConfigNotFoundException, io.airbyte.config.persistence.ConfigNotFoundException {
    // disable all connections associated with this destination
    // Delete connections first in case it fails in the middle, destination will still be visible
    final WorkspaceIdRequestBody workspaceIdRequestBody = new WorkspaceIdRequestBody().workspaceId(destination.getWorkspaceId());
    for (final ConnectionRead connectionRead : connectionsHandler.listConnectionsForWorkspace(workspaceIdRequestBody).getConnections()) {
      if (!connectionRead.getDestinationId().equals(destination.getDestinationId())) {
        continue;
      }

      connectionsHandler.deleteConnection(connectionRead.getConnectionId());
    }

    final ConnectorSpecification spec =
        getSpecForDestinationId(destination.getDestinationDefinitionId(), destination.getWorkspaceId(), destination.getDestinationId());

    // Delete secrets and config in this destination and mark it tombstoned.
    try {
      destinationService.tombstoneDestination(
          destination.getName(),
          destination.getWorkspaceId(),
          destination.getDestinationId(), spec);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  public DestinationRead updateDestination(final DestinationUpdate destinationUpdate)
      throws ConfigNotFoundException, IOException, JsonValidationException, io.airbyte.config.persistence.ConfigNotFoundException {
    if (destinationUpdate.getResourceAllocation() != null && airbyteEdition == Configs.AirbyteEdition.CLOUD) {
      throw new BadRequestException(String.format("Setting resource allocation is not permitted on %s", airbyteEdition));
    }

    // get existing implementation
    final DestinationConnection updatedDestination = configurationUpdate
        .destination(destinationUpdate.getDestinationId(), destinationUpdate.getName(), destinationUpdate.getConnectionConfiguration());

    final ConnectorSpecification spec =
        getSpecForDestinationId(updatedDestination.getDestinationDefinitionId(), updatedDestination.getWorkspaceId(),
            updatedDestination.getDestinationId());

    // validate configuration
    validateDestination(spec, updatedDestination.getConfiguration());

    // persist
    persistDestinationConnection(
        updatedDestination.getName(),
        updatedDestination.getDestinationDefinitionId(),
        updatedDestination.getWorkspaceId(),
        updatedDestination.getDestinationId(),
        updatedDestination.getConfiguration(),
        updatedDestination.getTombstone(),
        spec,
        destinationUpdate.getResourceAllocation());

    // read configuration from db
    return buildDestinationRead(
        destinationService.getDestinationConnection(destinationUpdate.getDestinationId()), spec);
  }

  public DestinationRead partialDestinationUpdate(final PartialDestinationUpdate partialDestinationUpdate)
      throws ConfigNotFoundException, IOException, JsonValidationException, io.airbyte.config.persistence.ConfigNotFoundException {
    if (partialDestinationUpdate.getResourceAllocation() != null && airbyteEdition == Configs.AirbyteEdition.CLOUD) {
      throw new BadRequestException(String.format("Setting resource allocation is not permitted on %s", airbyteEdition));
    }

    // get existing implementation
    final DestinationConnection updatedDestination = configurationUpdate
        .partialDestination(partialDestinationUpdate.getDestinationId(), partialDestinationUpdate.getName(),
            partialDestinationUpdate.getConnectionConfiguration());

    final ConnectorSpecification spec =
        getSpecForDestinationId(updatedDestination.getDestinationDefinitionId(), updatedDestination.getWorkspaceId(),
            updatedDestination.getDestinationId());

    OAuthSecretHelper.validateNoSecretsInConfiguration(spec, partialDestinationUpdate.getConnectionConfiguration());

    // validate configuration
    validateDestination(spec, updatedDestination.getConfiguration());

    // persist
    persistDestinationConnection(
        updatedDestination.getName(),
        updatedDestination.getDestinationDefinitionId(),
        updatedDestination.getWorkspaceId(),
        updatedDestination.getDestinationId(),
        updatedDestination.getConfiguration(),
        updatedDestination.getTombstone(),
        spec,
        partialDestinationUpdate.getResourceAllocation());

    // read configuration from db
    return buildDestinationRead(
        destinationService.getDestinationConnection(partialDestinationUpdate.getDestinationId()), spec);
  }

  /**
   * Upgrades the destination to the destination definition's default version.
   *
   * @param destinationIdRequestBody - ID of the destination to upgrade
   */
  public void upgradeDestinationVersion(final DestinationIdRequestBody destinationIdRequestBody)
      throws IOException, JsonValidationException, ConfigNotFoundException {
    final DestinationConnection destinationConnection = destinationService.getDestinationConnection(destinationIdRequestBody.getDestinationId());
    final StandardDestinationDefinition destinationDefinition =
        destinationService.getStandardDestinationDefinition(destinationConnection.getDestinationDefinitionId());
    actorDefinitionVersionUpdater.upgradeActorVersion(destinationConnection, destinationDefinition);
  }

  public DestinationRead getDestination(final DestinationIdRequestBody destinationIdRequestBody)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    return buildDestinationRead(destinationIdRequestBody.getDestinationId());
  }

  public DestinationReadList listDestinationsForWorkspace(final WorkspaceIdRequestBody workspaceIdRequestBody)
      throws ConfigNotFoundException, IOException, JsonValidationException {

    final List<DestinationRead> destinationReads = new ArrayList<>();
    final List<DestinationConnection> destinationConnections =
        destinationService.listWorkspaceDestinationConnection(workspaceIdRequestBody.getWorkspaceId());
    for (final DestinationConnection destinationConnection : destinationConnections) {
      destinationReads.add(buildDestinationReadWithStatus(destinationConnection));
    }

    return new DestinationReadList().destinations(destinationReads);
  }

  private DestinationRead buildDestinationReadWithStatus(final DestinationConnection destinationConnection)
      throws JsonValidationException, ConfigNotFoundException, IOException {
    final DestinationRead destinationRead = buildDestinationRead(destinationConnection);
    // add destination status into destinationRead
    if (destinationService.isDestinationActive(destinationConnection.getDestinationId())) {
      destinationRead.status(ActorStatus.ACTIVE);
    } else {
      destinationRead.status(ActorStatus.INACTIVE);
    }
    return destinationRead;
  }

  public DestinationReadList listDestinationsForWorkspaces(final ListResourcesForWorkspacesRequestBody listResourcesForWorkspacesRequestBody)
      throws ConfigNotFoundException, IOException, JsonValidationException {

    final List<DestinationRead> reads = Lists.newArrayList();
    final List<DestinationConnection> destinationConnections = destinationService.listWorkspacesDestinationConnections(
        new ResourcesQueryPaginated(
            listResourcesForWorkspacesRequestBody.getWorkspaceIds(),
            listResourcesForWorkspacesRequestBody.getIncludeDeleted(),
            listResourcesForWorkspacesRequestBody.getPagination().getPageSize(),
            listResourcesForWorkspacesRequestBody.getPagination().getRowOffset(), null));
    for (final DestinationConnection destinationConnection : destinationConnections) {
      reads.add(buildDestinationReadWithStatus(destinationConnection));
    }
    return new DestinationReadList().destinations(reads);
  }

  public DestinationReadList listDestinationsForDestinationDefinition(final UUID destinationDefinitionId)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    final List<DestinationRead> reads = Lists.newArrayList();

    for (final DestinationConnection destinationConnection : destinationService
        .listDestinationsForDefinition(destinationDefinitionId)) {
      reads.add(buildDestinationRead(destinationConnection));
    }

    return new DestinationReadList().destinations(reads);
  }

  public DestinationReadList searchDestinations(final DestinationSearch destinationSearch)
      throws ConfigNotFoundException, IOException, JsonValidationException {
    final List<DestinationRead> reads = Lists.newArrayList();

    for (final DestinationConnection dci : destinationService.listDestinationConnection()) {
      if (!dci.getTombstone()) {
        final DestinationRead destinationRead = buildDestinationRead(dci);
        if (MatchSearchHandler.matchSearch(destinationSearch, destinationRead)) {
          reads.add(destinationRead);
        }
      }
    }

    return new DestinationReadList().destinations(reads);
  }

  private void validateDestination(final ConnectorSpecification spec, final JsonNode configuration) throws JsonValidationException {
    validator.ensure(spec.getConnectionSpecification(), configuration);
  }

  public ConnectorSpecification getSpecForDestinationId(final UUID destinationDefinitionId, final UUID workspaceId, final UUID destinationId)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    final StandardDestinationDefinition destinationDefinition = destinationService.getStandardDestinationDefinition(destinationDefinitionId);
    final ActorDefinitionVersion destinationVersion =
        actorDefinitionVersionHelper.getDestinationVersion(destinationDefinition, workspaceId, destinationId);
    return destinationVersion.getSpec();
  }

  public ConnectorSpecification getSpecForWorkspaceId(final UUID destinationDefinitionId, final UUID workspaceId)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    final StandardDestinationDefinition destinationDefinition = destinationService.getStandardDestinationDefinition(destinationDefinitionId);
    final ActorDefinitionVersion destinationVersion = actorDefinitionVersionHelper.getDestinationVersion(destinationDefinition, workspaceId);
    return destinationVersion.getSpec();
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  private void persistDestinationConnection(final String name,
                                            final UUID destinationDefinitionId,
                                            final UUID workspaceId,
                                            final UUID destinationId,
                                            final JsonNode configurationJson,
                                            final boolean tombstone,
                                            final ConnectorSpecification spec,
                                            final io.airbyte.api.model.generated.ScopedResourceRequirements resourceRequirements)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    final UUID organizationId = workspaceHelper.getOrganizationForWorkspace(workspaceId);
    licenseEntitlementChecker.ensureEntitled(organizationId, Entitlement.DESTINATION_CONNECTOR, destinationDefinitionId);

    final JsonNode oAuthMaskedConfigurationJson =
        oAuthConfigSupplier.maskDestinationOAuthParameters(destinationDefinitionId, workspaceId, configurationJson, spec);
    final DestinationConnection destinationConnection = new DestinationConnection()
        .withName(name)
        .withDestinationDefinitionId(destinationDefinitionId)
        .withWorkspaceId(workspaceId)
        .withDestinationId(destinationId)
        .withConfiguration(oAuthMaskedConfigurationJson)
        .withTombstone(tombstone)
        .withResourceRequirements(apiPojoConverters.scopedResourceReqsToInternal(resourceRequirements));

    final Optional<JsonNode> previousDestinationConfig =
        destinationService.getDestinationConnectionIfExists(destinationId).map(DestinationConnection::getConfiguration);

    RuntimeSecretPersistence secretPersistence = null;
    if (featureFlagClient.boolVariation(UseRuntimeSecretPersistence.INSTANCE, new Organization(organizationId))) {
      final SecretPersistenceConfig secretPersistenceConfig = secretPersistenceConfigService.get(ScopeType.ORGANIZATION, organizationId);
      secretPersistence = new RuntimeSecretPersistence(secretPersistenceConfig, metricClient);
    }
    final JsonNode partialConfig;
    if (previousDestinationConfig.isPresent()) {
      partialConfig = secretsRepositoryWriter.updateFromConfig(
          workspaceId,
          previousDestinationConfig.get(),
          destinationConnection.getConfiguration(),
          spec.getConnectionSpecification(),
          secretPersistence);
    } else {
      partialConfig = secretsRepositoryWriter.createFromConfig(
          workspaceId,
          destinationConnection.getConfiguration(),
          spec.getConnectionSpecification(),
          secretPersistence);
    }
    destinationConnection.setConfiguration(partialConfig);

    destinationService.writeDestinationConnectionNoSecrets(destinationConnection);
  }

  public DestinationRead buildDestinationRead(final UUID destinationId)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    return buildDestinationRead(destinationService.getDestinationConnection(destinationId));
  }

  private DestinationRead buildDestinationRead(final DestinationConnection destinationConnection)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    final ConnectorSpecification spec =
        getSpecForDestinationId(destinationConnection.getDestinationDefinitionId(), destinationConnection.getWorkspaceId(),
            destinationConnection.getDestinationId());
    return buildDestinationRead(destinationConnection, spec);
  }

  private DestinationRead buildDestinationRead(final DestinationConnection destinationConnection, final ConnectorSpecification spec)
      throws ConfigNotFoundException, IOException, JsonValidationException {

    // remove secrets from config before returning the read
    final DestinationConnection dci = Jsons.clone(destinationConnection);
    dci.setConfiguration(secretsProcessor.prepareSecretsForOutput(dci.getConfiguration(), spec.getConnectionSpecification()));

    final StandardDestinationDefinition standardDestinationDefinition =
        destinationService.getStandardDestinationDefinition(dci.getDestinationDefinitionId());
    return toDestinationRead(dci, standardDestinationDefinition);
  }

  protected DestinationRead toDestinationRead(final DestinationConnection destinationConnection,
                                              final StandardDestinationDefinition standardDestinationDefinition)
      throws JsonValidationException, ConfigNotFoundException, IOException {

    final ActorDefinitionVersionWithOverrideStatus destinationVersionWithOverrideStatus =
        actorDefinitionVersionHelper.getDestinationVersionWithOverrideStatus(
            standardDestinationDefinition, destinationConnection.getWorkspaceId(), destinationConnection.getDestinationId());

    final Optional<ActorDefinitionVersionBreakingChanges> breakingChanges =
        actorDefinitionHandlerHelper.getVersionBreakingChanges(destinationVersionWithOverrideStatus.actorDefinitionVersion());

    final UUID organizationId = workspaceHelper.getOrganizationForWorkspace(destinationConnection.getWorkspaceId());
    final boolean isEntitled = licenseEntitlementChecker.checkEntitlement(organizationId, Entitlement.DESTINATION_CONNECTOR,
        standardDestinationDefinition.getDestinationDefinitionId());

    return new DestinationRead()
        .destinationDefinitionId(standardDestinationDefinition.getDestinationDefinitionId())
        .destinationId(destinationConnection.getDestinationId())
        .workspaceId(destinationConnection.getWorkspaceId())
        .destinationDefinitionId(destinationConnection.getDestinationDefinitionId())
        .connectionConfiguration(destinationConnection.getConfiguration())
        .name(destinationConnection.getName())
        .destinationName(standardDestinationDefinition.getName())
        .icon(standardDestinationDefinition.getIconUrl())
        .isVersionOverrideApplied(destinationVersionWithOverrideStatus.isOverrideApplied())
        .isEntitled(isEntitled)
        .breakingChanges(breakingChanges.orElse(null))
        .supportState(apiPojoConverters.toApiSupportState(destinationVersionWithOverrideStatus.actorDefinitionVersion().getSupportState()))
        .createdAt(destinationConnection.getCreatedAt())
        .resourceAllocation(apiPojoConverters.scopedResourceReqsToApi(destinationConnection.getResourceRequirements()));
  }

  protected DestinationSnippetRead toDestinationSnippetRead(final DestinationConnection destinationConnection,
                                                            final StandardDestinationDefinition standardDestinationDefinition) {

    return new DestinationSnippetRead()
        .destinationId(destinationConnection.getDestinationId())
        .name(destinationConnection.getName())
        .destinationDefinitionId(standardDestinationDefinition.getDestinationDefinitionId())
        .destinationName(standardDestinationDefinition.getName())
        .icon(standardDestinationDefinition.getIconUrl());
  }

}
