
{{/* DO NOT EDIT: This file was autogenerated. */}}

{{/*
    Jobs Configuration
*/}}

{{/*
Renders the jobs secret name
*/}}
{{- define "airbyte.jobs.secretName" }}
{{- if .Values.global.jobs.secretName }}
    {{- .Values.global.jobs.secretName | quote }}
{{- else }}
    {{- .Release.Name }}-airbyte-secrets
{{- end }}
{{- end }}

{{/*
Renders the global.jobs.kube.serviceAccount value
*/}}
{{- define "airbyte.jobs.kube.serviceAccount" }}
    {{- .Values.global.serviceAccountName }}
{{- end }}

{{/*
Renders the jobs.kube.serviceAccount environment variable
*/}}
{{- define "airbyte.jobs.kube.serviceAccount.env" }}
- name: JOB_KUBE_SERVICEACCOUNT
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_SERVICEACCOUNT
{{- end }}

{{/*
Renders the global.jobs.kube.namespace value
*/}}
{{- define "airbyte.jobs.kube.namespace" }}
    {{- .Values.global.jobs.kube.namespace }}
{{- end }}

{{/*
Renders the jobs.kube.namespace environment variable
*/}}
{{- define "airbyte.jobs.kube.namespace.env" }}
- name: JOB_KUBE_NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
    
{{- end }}

{{/*
Renders the global.jobs.kube.localVolume.enabled value
*/}}
{{- define "airbyte.jobs.kube.localVolume.enabled" }}
    {{- .Values.global.jobs.kube.localVolume.enabled | default false }}
{{- end }}

{{/*
Renders the jobs.kube.localVolume.enabled environment variable
*/}}
{{- define "airbyte.jobs.kube.localVolume.enabled.env" }}
- name: JOB_KUBE_LOCAL_VOLUME_ENABLED
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_LOCAL_VOLUME_ENABLED
{{- end }}

{{/*
Renders the global.jobs.kube.images.busybox value
*/}}
{{- define "airbyte.jobs.kube.images.busybox" }}
    {{- include "imageUrl" (list .Values.global.jobs.kube.images.busybox $) }}
{{- end }}

{{/*
Renders the jobs.kube.images.busybox environment variable
*/}}
{{- define "airbyte.jobs.kube.images.busybox.env" }}
- name: JOB_KUBE_BUSYBOX_IMAGE
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_BUSYBOX_IMAGE
{{- end }}

{{/*
Renders the global.jobs.kube.images.socat value
*/}}
{{- define "airbyte.jobs.kube.images.socat" }}
    {{- include "imageUrl" (list .Values.global.jobs.kube.images.socat $) }}
{{- end }}

{{/*
Renders the jobs.kube.images.socat environment variable
*/}}
{{- define "airbyte.jobs.kube.images.socat.env" }}
- name: JOB_KUBE_SOCAT_IMAGE
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_SOCAT_IMAGE
{{- end }}

{{/*
Renders the global.jobs.kube.images.curl value
*/}}
{{- define "airbyte.jobs.kube.images.curl" }}
    {{- include "imageUrl" (list .Values.global.jobs.kube.images.curl $) }}
{{- end }}

{{/*
Renders the jobs.kube.images.curl environment variable
*/}}
{{- define "airbyte.jobs.kube.images.curl.env" }}
- name: JOB_KUBE_CURL_IMAGE
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_CURL_IMAGE
{{- end }}

{{/*
Renders the global.jobs.kube.main_container_image_pull_secret value
*/}}
{{- define "airbyte.jobs.kube.main_container_image_pull_secret" }}
    {{- .Values.global.jobs.kube.main_container_image_pull_secret }}
{{- end }}

{{/*
Renders the jobs.kube.main_container_image_pull_secret environment variable
*/}}
{{- define "airbyte.jobs.kube.main_container_image_pull_secret.env" }}
- name: JOB_KUBE_MAIN_CONTAINER_IMAGE_PULL_SECRET
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_MAIN_CONTAINER_IMAGE_PULL_SECRET
{{- end }}

{{/*
Renders the global.jobs.kube.annotations value
*/}}
{{- define "airbyte.jobs.kube.annotations" }}
    {{- .Values.global.jobs.kube.annotations | include "airbyte.flattenMap" }}
{{- end }}

{{/*
Renders the jobs.kube.annotations environment variable
*/}}
{{- define "airbyte.jobs.kube.annotations.env" }}
- name: JOB_KUBE_ANNOTATIONS
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_ANNOTATIONS
{{- end }}

{{/*
Renders the global.jobs.kube.labels value
*/}}
{{- define "airbyte.jobs.kube.labels" }}
    {{- .Values.global.jobs.kube.labels | include "airbyte.flattenMap" }}
{{- end }}

{{/*
Renders the jobs.kube.labels environment variable
*/}}
{{- define "airbyte.jobs.kube.labels.env" }}
- name: JOB_KUBE_LABELS
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_LABELS
{{- end }}

{{/*
Renders the global.jobs.kube.nodeSelector value
*/}}
{{- define "airbyte.jobs.kube.nodeSelector" }}
    {{- .Values.global.jobs.kube.nodeSelector | include "airbyte.flattenMap" }}
{{- end }}

{{/*
Renders the jobs.kube.nodeSelector environment variable
*/}}
{{- define "airbyte.jobs.kube.nodeSelector.env" }}
- name: JOB_KUBE_NODE_SELECTORS
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_NODE_SELECTORS
{{- end }}

{{/*
Renders the global.jobs.kube.tolerations value
*/}}
{{- define "airbyte.jobs.kube.tolerations" }}
    {{- .Values.global.jobs.kube.tolerations | include "airbyte.flattenArrayMap" }}
{{- end }}

{{/*
Renders the jobs.kube.tolerations environment variable
*/}}
{{- define "airbyte.jobs.kube.tolerations.env" }}
- name: JOB_KUBE_TOLERATIONS
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_KUBE_TOLERATIONS
{{- end }}

{{/*
Renders the jobs.errors.reportingStrategy environment variable
*/}}
{{- define "airbyte.jobs.errors.reportingStrategy.env" }}
- name: JOB_ERROR_REPORTING_STRATEGY
  valueFrom:
    configMapKeyRef:
      name: {{ .Release.Name }}-airbyte-env
      key: JOB_ERROR_REPORTING_STRATEGY
{{- end }}

{{/*
Renders the set of all jobs environment variables
*/}}
{{- define "airbyte.jobs.envs" }}
{{- include "airbyte.jobs.kube.serviceAccount.env" . }}
{{- include "airbyte.jobs.kube.namespace.env" . }}
{{- include "airbyte.jobs.kube.localVolume.enabled.env" . }}
{{- include "airbyte.jobs.kube.images.busybox.env" . }}
{{- include "airbyte.jobs.kube.images.socat.env" . }}
{{- include "airbyte.jobs.kube.images.curl.env" . }}
{{- include "airbyte.jobs.kube.main_container_image_pull_secret.env" . }}
{{- include "airbyte.jobs.kube.annotations.env" . }}
{{- include "airbyte.jobs.kube.labels.env" . }}
{{- include "airbyte.jobs.kube.nodeSelector.env" . }}
{{- include "airbyte.jobs.kube.tolerations.env" . }}
{{- include "airbyte.jobs.errors.reportingStrategy.env" . }}
{{- end }}

{{/*
Renders the set of all jobs config map variables
*/}}
{{- define "airbyte.jobs.configVars" }}
JOB_KUBE_SERVICEACCOUNT: {{ .Values.global.serviceAccountName | quote }}
JOB_KUBE_NAMESPACE: {{ include "airbyte.jobs.kube.namespace" . | quote }}
JOB_KUBE_LOCAL_VOLUME_ENABLED: {{ include "airbyte.jobs.kube.localVolume.enabled" . | quote }}
JOB_KUBE_BUSYBOX_IMAGE: {{ include "imageUrl" (list .Values.global.jobs.kube.images.busybox $) | quote }}
JOB_KUBE_SOCAT_IMAGE: {{ include "imageUrl" (list .Values.global.jobs.kube.images.socat $) | quote }}
JOB_KUBE_CURL_IMAGE: {{ include "imageUrl" (list .Values.global.jobs.kube.images.curl $) | quote }}
JOB_KUBE_MAIN_CONTAINER_IMAGE_PULL_SECRET: {{ include "airbyte.jobs.kube.main_container_image_pull_secret" . | quote }}
JOB_KUBE_ANNOTATIONS: {{ .Values.global.jobs.kube.annotations | include "airbyte.flattenMap" | quote }}
JOB_KUBE_LABELS: {{ .Values.global.jobs.kube.labels | include "airbyte.flattenMap" | quote }}
JOB_KUBE_NODE_SELECTORS: {{ .Values.global.jobs.kube.nodeSelector | include "airbyte.flattenMap" | quote }}
JOB_KUBE_TOLERATIONS: {{ .Values.global.jobs.kube.tolerations | include "airbyte.flattenArrayMap" | quote }}
JOB_ERROR_REPORTING_STRATEGY: {{ "logging" | quote }}
{{- end }}