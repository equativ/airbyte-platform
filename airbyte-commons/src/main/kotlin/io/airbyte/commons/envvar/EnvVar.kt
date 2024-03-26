package io.airbyte.commons.envvar

/**
 * A collection of an environment variables currently used by the Airbyte platform.
 *
 * The enum value _must exactly_ match the name of environment-variables.
 *
 * These are defined in alphabetical order for findability/readability reasons.
 */
enum class EnvVar {
  AIRBYTE_ROLE,
  AIRBYTE_VERSION,
  AWS_ACCESS_KEY_ID,
  AWS_ASSUME_ROLE_SECRET_ACCESS_KEY,
  AWS_ASSUME_ROLE_SECRET_NAME,
  AWS_DEFAULT_REGION,
  AWS_SECRET_ACCESS_KEY,

  CDK_ENTRYPOINT,
  CDK_PYTHON,
  CONFIG_ROOT,
  CUSTOMERIO_API_KEY,

  DATABASE_PASSWORD,
  DATABASE_URL,
  DATABASE_USER,
  DD_AGENT_HOST,
  DD_CONSTANT_TAGS,
  DD_DOGSTATSD_PORT,
  DD_SERVICE,
  DD_VERSION,
  DEPLOYMENT_ENV,
  DEPLOYMENT_MODE,
  DOCKER_HOST,
  DOCKER_NETWORK,

  FEATURE_FLAG_CLIENT,

  GOOGLE_APPLICATION_CREDENTIALS,

  JAVA_OPTS,
  JOB_DEFAULT_ENV_,
  JOB_DEFAULT_ENV_MAP,
  JOB_ISOLATED_KUBE_NODE_SELECTORS,
  JOB_KUBE_ANNOTATIONS,
  JOB_KUBE_BUSYBOX_IMAGE,
  JOB_KUBE_CURL_IMAGE,
  JOB_KUBE_LABELS,
  JOB_KUBE_MAIN_CONTAINER_IMAGE_PULL_POLICY,
  JOB_KUBE_MAIN_CONTAINER_IMAGE_PULL_SECRET,
  JOB_KUBE_NAMESPACE,
  JOB_KUBE_NODE_SELECTORS,
  JOB_KUBE_SERVICEACCOUNT,
  JOB_KUBE_SIDECAR_CONTAINER_IMAGE_PULL_POLICY,
  JOB_KUBE_SOCAT_IMAGE,
  JOB_KUBE_TOLERATIONS,
  JOB_MAIN_CONTAINER_CPU_LIMIT,
  JOB_MAIN_CONTAINER_CPU_REQUEST,
  JOB_MAIN_CONTAINER_MEMORY_LIMIT,
  JOB_MAIN_CONTAINER_MEMORY_REQUEST,

  LAUNCHDARKLY_KEY,
  LOCAL,
  LOCAL_CONNECTOR_CATALOG_PATH,
  LOCAL_DOCKER_MOUNT,
  LOCAL_ROOT,
  LOG4J_CONFIGURATION_FILE,

  METRIC_CLIENT,
  MINIO_ENDPOINT,

  OTEL_COLLECTOR_ENDPOINT,

  PUBLISH_METRICS,

  REMOTE_DATAPLANE_SERVICEACCOUNTS,

  SERVICE_NAME,
  SIDECAR_KUBE_CPU_LIMIT,
  SIDECAR_KUBE_CPU_REQUEST,
  SIDECAR_KUBE_MEMORY_LIMIT,
  SIDECAR_MEMORY_REQUEST,
  SOCAT_KUBE_CPU_LIMIT,
  SOCAT_KUBE_CPU_REQUEST,
  STORAGE_BUCKET_LOG,
  STORAGE_BUCKET_STATE,
  STORAGE_BUCKET_WORKLOAD_OUTPUT,
  STORAGE_TYPE,
  SYNC_JOB_INIT_RETRY_TIMEOUT_MINUTES,

  TEMPORAL_HISTORY_RETENTION_IN_DAYS,

  USE_CUSTOM_NODE_SELECTOR,

  WORKER_ENVIRONMENT,
  WORKSPACE_DOCKER_MOUNT,
  WORKSPACE_ROOT,

  /** These exist testing purposes only! DO NOT USE in non-test code! */
  Z_TESTING_PURPOSES_ONLY_1,
  Z_TESTING_PURPOSES_ONLY_2,
  Z_TESTING_PURPOSES_ONLY_3,
  ;

  /**
   * Fetch the value of this [EnvVar], returning [default] if the value is null or an empty string
   *
   * @param default value to return if this environment variable is null or empty
   */
  @JvmOverloads
  fun fetch(default: String? = null): String? = System.getenv(this.name).takeUnless { it.isNullOrBlank() } ?: default
}