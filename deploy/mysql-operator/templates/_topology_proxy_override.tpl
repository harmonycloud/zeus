{{/*覆盖proxy的配置*/}}

{{- define "middlware.proxy.override.topologySpreadConstraints" }}
{{- if eq (semverCompare ">= 1.19-0" .Capabilities.KubeVersion.Version) true }}
topologySpreadConstraints:
{{- if ne .Values.podAntiAffinityTopologKey "kubernetes.io/hostname"}}
- maxSkew: 1
  topologyKey: {{ .Values.podAntiAffinityTopologKey | default "" }}
  {{- include "middlware.proxy.whenUnsatisfiable" . }}
  labelSelector:
      matchLabels:
        {{ include "middleware.proxy.key" . }}: {{ include "middleware.proxy.name" . }}
{{- end }}
- maxSkew: 1
  topologyKey: "kubernetes.io/hostname"
  {{- include "middlware.proxy.whenUnsatisfiable" . }}
  labelSelector:
      matchLabels:
        {{ include "middleware.proxy.key" . }}: {{ include "middleware.proxy.name" . }}
{{- if .Values.nodeAffinity }}
affinity:
  {{- with .Values.nodeAffinity }}
  nodeAffinity:
  {{- toYaml . | nindent 8 }}
  {{- end }}
{{- end }}
{{- end }}
{{- end }}

{{- define "middlware.proxy.override.affinity" }}
{{- if eq (semverCompare ">= 1.19-0" .Capabilities.KubeVersion.Version) false }}
affinity:
  {{- if eq .Values.podAntiAffinity "hard"}}
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        topologyKey: {{ .Values.podAntiAffinityTopologKey }}
        matchExpressions:
        - key: {{ include "middleware.proxy.key" . }}
          operator: In
          values:
          - {{ include "middleware.proxy.name" . }}
  {{- else if eq .Values.podAntiAffinity "soft"}}
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        topologyKey: {{ .Values.podAntiAffinityTopologKey }}
        labelSelector:
          matchExpressions:
          - key: {{ include "middleware.proxy.key" . }}
            operator: In
            values:
            - {{ include "middleware.proxy.name" . }}
  {{- end }}
  {{- with .Values.nodeAffinity }}
  nodeAffinity:
  {{- toYaml . | nindent 8 }}
  {{- end }}
  {{- end }}
{{- end }}