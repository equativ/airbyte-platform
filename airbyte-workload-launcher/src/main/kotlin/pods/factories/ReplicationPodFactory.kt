/*
 * Copyright (c) 2020-2025 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.workload.launcher.pods.factories

import io.airbyte.featureflag.ANONYMOUS
import io.airbyte.featureflag.Connection
import io.airbyte.featureflag.FeatureFlagClient
import io.airbyte.featureflag.UseCustomK8sScheduler
import io.airbyte.workers.context.WorkloadSecurityContextProvider
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.LocalObjectReference
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.ResourceRequirements
import io.micronaut.context.annotation.Value
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
data class ReplicationPodFactory(
  private val featureFlagClient: FeatureFlagClient,
  private val initContainerFactory: InitContainerFactory,
  private val replContainerFactory: ReplicationContainerFactory,
  private val profilerContainerFactory: ProfilerContainerFactory,
  private val volumeFactory: VolumeFactory,
  private val workloadSecurityContextProvider: WorkloadSecurityContextProvider,
  @Value("\${airbyte.worker.job.kube.serviceAccount}") private val serviceAccount: String?,
  private val nodeSelectionFactory: NodeSelectionFactory,
  @Named("replicationImagePullSecrets") private val imagePullSecrets: List<LocalObjectReference>,
) {
  internal fun create(
    podName: String,
    allLabels: Map<String, String>,
    annotations: Map<String, String>,
    nodeSelectors: Map<String, String>,
    // TODO: Consider moving container creation to the caller to avoid prop drilling.
    orchImage: String,
    sourceImage: String,
    destImage: String,
    orchResourceReqs: ResourceRequirements?,
    sourceResourceReqs: ResourceRequirements?,
    destResourceReqs: ResourceRequirements?,
    orchRuntimeEnvVars: List<EnvVar>,
    sourceRuntimeEnvVars: List<EnvVar>,
    destRuntimeEnvVars: List<EnvVar>,
    isFileTransfer: Boolean,
    workspaceId: UUID,
    enableAsyncProfiler: Boolean = false,
    singleConnectorTest: Boolean = false,
    socketTest: Boolean = false,
    exposedOrchestratorPorts: List<Int> = emptyList(),
  ): Pod {
    // TODO: We should inject the scheduler from the ENV and use this just for overrides
    val schedulerName = featureFlagClient.stringVariation(UseCustomK8sScheduler, Connection(ANONYMOUS))

    val replicationVolumes = volumeFactory.replication(isFileTransfer, enableAsyncProfiler, socketTest)
    val initContainer =
      initContainerFactory.create(
        resourceReqs = orchResourceReqs,
        volumeMounts = replicationVolumes.orchVolumeMounts,
        runtimeEnvVars = orchRuntimeEnvVars,
        workspaceId = workspaceId,
      )

    val orchContainer =
      replContainerFactory.createOrchestrator(
        resourceReqs = orchResourceReqs,
        volumeMounts = replicationVolumes.orchVolumeMounts,
        runtimeEnvVars = orchRuntimeEnvVars,
        image = orchImage,
        exposedOrchestratorPorts = exposedOrchestratorPorts,
      )

    val sourceContainer =
      replContainerFactory.createSource(
        resourceReqs = sourceResourceReqs,
        volumeMounts = replicationVolumes.sourceVolumeMounts,
        runtimeEnvVars = sourceRuntimeEnvVars,
        image = sourceImage,
      )

    val destContainer =
      replContainerFactory.createDestination(
        resourceReqs = destResourceReqs,
        volumeMounts = replicationVolumes.destVolumeMounts,
        runtimeEnvVars = destRuntimeEnvVars,
        image = destImage,
      )

    val nodeSelection = nodeSelectionFactory.createReplicationNodeSelection(nodeSelectors, allLabels)

    val containers =
      when {
        singleConnectorTest -> mutableListOf(orchContainer)
        socketTest -> mutableListOf(orchContainer, sourceContainer)
        else -> mutableListOf(orchContainer, sourceContainer, destContainer)
      }

    if (enableAsyncProfiler) {
      containers.add(profilerContainerFactory.create(orchRuntimeEnvVars, replicationVolumes.profilerVolumeMounts))
    }

    return PodBuilder()
      .withApiVersion("v1")
      .withNewMetadata()
      .withName(podName)
      .withLabels<Any, Any>(allLabels)
      .withAnnotations<Any, Any>(annotations)
      .endMetadata()
      .withNewSpec()
      .withSchedulerName(schedulerName)
      .withServiceAccount(serviceAccount)
      .withAutomountServiceAccountToken(true)
      .withRestartPolicy("Never")
      .withInitContainers(initContainer)
      .withShareProcessNamespace(enableAsyncProfiler)
      .withContainers(containers)
      .withImagePullSecrets(imagePullSecrets)
      .withVolumes(replicationVolumes.allVolumes)
      .withNodeSelector<Any, Any>(nodeSelection.nodeSelectors)
      .withTolerations(nodeSelection.tolerations)
      .withAffinity(nodeSelection.podAffinity)
      .withAutomountServiceAccountToken(false)
      .withSecurityContext(
        if (enableAsyncProfiler) {
          workloadSecurityContextProvider.rootSecurityContext()
        } else {
          workloadSecurityContextProvider.defaultPodSecurityContext()
        },
      ).endSpec()
      .build()
  }

  internal fun createReset(
    podName: String,
    allLabels: Map<String, String>,
    annotations: Map<String, String>,
    nodeSelectors: Map<String, String>,
    // TODO: Consider moving container creation to the caller to avoid prop drilling.
    orchImage: String,
    destImage: String,
    orchResourceReqs: ResourceRequirements?,
    destResourceReqs: ResourceRequirements?,
    orchRuntimeEnvVars: List<EnvVar>,
    destRuntimeEnvVars: List<EnvVar>,
    isFileTransfer: Boolean,
    workspaceId: UUID,
    exposedOrchestratorPorts: List<Int>,
  ): Pod {
    // TODO: We should inject the scheduler from the ENV and use this just for overrides
    val schedulerName = featureFlagClient.stringVariation(UseCustomK8sScheduler, Connection(ANONYMOUS))

    val replicationVolumes = volumeFactory.replication(isFileTransfer)
    val initContainer =
      initContainerFactory.create(
        resourceReqs = orchResourceReqs,
        volumeMounts = replicationVolumes.orchVolumeMounts,
        runtimeEnvVars = orchRuntimeEnvVars,
        workspaceId = workspaceId,
      )

    val orchContainer =
      replContainerFactory.createOrchestrator(
        resourceReqs = orchResourceReqs,
        volumeMounts = replicationVolumes.orchVolumeMounts,
        runtimeEnvVars = orchRuntimeEnvVars,
        image = orchImage,
        exposedOrchestratorPorts = exposedOrchestratorPorts,
      )

    val destContainer =
      replContainerFactory.createDestination(
        resourceReqs = destResourceReqs,
        volumeMounts = replicationVolumes.destVolumeMounts,
        runtimeEnvVars = destRuntimeEnvVars,
        image = destImage,
      )

    val nodeSelection = nodeSelectionFactory.createResetNodeSelection(nodeSelectors)

    return PodBuilder()
      .withApiVersion("v1")
      .withNewMetadata()
      .withName(podName)
      .withLabels<Any, Any>(allLabels)
      .withAnnotations<Any, Any>(annotations)
      .endMetadata()
      .withNewSpec()
      .withSchedulerName(schedulerName)
      .withServiceAccount(serviceAccount)
      .withAutomountServiceAccountToken(true)
      .withRestartPolicy("Never")
      .withInitContainers(initContainer)
      .withContainers(orchContainer, destContainer)
      .withImagePullSecrets(imagePullSecrets)
      .withVolumes(replicationVolumes.allVolumes)
      .withNodeSelector<Any, Any>(nodeSelection.nodeSelectors)
      .withTolerations(nodeSelection.tolerations)
      .withAffinity(nodeSelection.podAffinity)
      .withAutomountServiceAccountToken(false)
      .withSecurityContext(workloadSecurityContextProvider.defaultPodSecurityContext())
      .endSpec()
      .build()
  }
}
