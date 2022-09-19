
{{/*设置组件label中的key*/}}
{{- define "middleware.proxy.key" -}}
app.kubernetes.io/instance
{{- end }}

{{/*设置组件label中的name*/}}
{{- define "middleware.proxy.name" -}}
{{ include "mysql.fullname" . }}-proxy
{{- end }}


{{- define "middlware.proxy.tolerations" }}
{{- with .Values.proxy.tolerations }}
tolerations:
{{- toYaml .| nindent 0  }}
{{- end }}
{{- end }}

{{- define "middlware.proxy.topologySpreadConstraints" }}
{{- if eq (semverCompare ">= 1.19-0" .Capabilities.KubeVersion.Version) true }}
topologySpreadConstraints:
{{- if ne .Values.proxy.podAntiAffinityTopologKey "kubernetes.io/hostname"}}
- maxSkew: 1
  topologyKey: {{ .Values.proxy.podAntiAffinityTopologKey | default "" }}
  whenUnsatisfiable: DoNotSchedule
  labelSelector:
      matchLabels:
        {{ include "middleware.proxy.key" . }}: {{ include "middleware.proxy.name" . }}
{{- end }}
- maxSkew: 1
  topologyKey: "kubernetes.io/hostname"
  whenUnsatisfiable: DoNotSchedule
  labelSelector:
      matchLabels:
        {{ include "middleware.proxy.key" . }}: {{ include "middleware.proxy.name" . }}
{{- if .Values.proxy.nodeAffinity }}
affinity:
  {{- with .Values.proxy.nodeAffinity }}
  nodeAffinity:
  {{- toYaml . | nindent 8 }}
  {{- end }}
{{- else if .Values.nodeAffinity  }}
  {{- with .Values.nodeAffinity }}
  nodeAffinity:
  {{- toYaml . | nindent 8 }}
  {{- end }}
{{- end }}
{{- end }}
{{- end }}

{{- define "middlware.proxy.affinity" }}
{{- if eq (semverCompare ">= 1.19-0" .Capabilities.KubeVersion.Version) false }}
affinity:
  {{- if eq .Values.proxy.podAntiAffinity "hard"}}
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        topologyKey: {{ .Values.podAntiAffinityTopologKey }}
        matchExpressions:
        - key: {{ include "middleware.proxy.key" . }}
          operator: In
          values:
          - {{ include "middleware.proxy.name" . }}
  {{- else if eq .Values.proxy.podAntiAffinity "soft"}}
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
  {{- with .Values.proxy.nodeAffinity }}
  nodeAffinity:
  {{- toYaml . | nindent 8 }}
  {{- end }}
  {{- end }}
{{- end }}


{{/*组件拓扑分布*/}}
{{- define "middlware.proxy.topologyDistribution" }}
  {{- if .Values.proxy.tolerations }}
    {{- include "middlware.proxy.tolerations" . -}}
  {{- else -}}
    {{- include "middlware.tolerations" . -}}
  {{- end -}}

  {{- if .Values.proxy.podAntiAffinityTopologKey }}
    {{- include "middlware.proxy.topologySpreadConstraints" . -}}
    {{- include "middlware.proxy.affinity" . -}}
  {{- else -}}
    {{- include "middlware.proxy.override.topologySpreadConstraints" . -}}
    {{- include "middlware.proxy.override.affinity" . -}}
  {{- end -}}
{{- end -}}


