
{{/*设置组件label中的key*/}}
{{- define "middleware.key" -}}
app
{{- end }}

{{/*设置组件label中的name*/}}
{{- define "middleware.name" -}}
{{ include "mysql.fullname" . }}
{{- end }}


{{- define "middlware.tolerations" }}
{{- with .Values.tolerations }}
tolerations:
{{- toYaml .| nindent 0  }}
{{- end }}
{{- end }}

{{- define "middlware.topologySpreadConstraints" }}
{{- if eq (semverCompare ">= 1.19-0" .Capabilities.KubeVersion.Version) true }}
topologySpreadConstraints:
{{- if ne .Values.podAntiAffinityTopologKey "kubernetes.io/hostname"}}
- maxSkew: 1
  topologyKey: {{ .Values.podAntiAffinityTopologKey | default "" }}
  whenUnsatisfiable: DoNotSchedule
  labelSelector:
      matchLabels:
        {{ include "middleware.key" . }}: {{ include "middleware.name" . }}
{{- end }}
- maxSkew: 1
  topologyKey: "kubernetes.io/hostname"
  whenUnsatisfiable: DoNotSchedule
  labelSelector:
      matchLabels:
        {{ include "middleware.key" . }}: {{ include "middleware.name" . }}
{{- if .Values.nodeAffinity }}
affinity:
  {{- with .Values.nodeAffinity }}
  nodeAffinity:
  {{- toYaml . | nindent 8 }}
  {{- end }}
{{- end }}
{{- end }}
{{- end }}

{{- define "middlware.affinity" }}
{{- if eq (semverCompare ">= 1.19-0" .Capabilities.KubeVersion.Version) false }}
affinity:
  {{- if eq .Values.podAntiAffinity "hard"}}
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        topologyKey: {{ .Values.podAntiAffinityTopologKey }}
        matchExpressions:
        - key: {{ include "middleware.key" . }}
          operator: In
          values:
          - {{ include "middleware.name" . }}
  {{- else if eq .Values.podAntiAffinity "soft"}}
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        topologyKey: {{ .Values.podAntiAffinityTopologKey }}
        labelSelector:
          matchExpressions:
          - key: {{ include "middleware.key" . }}
            operator: In
            values:
            - {{ include "middleware.name" . }}
  {{- end }}
  {{- with .Values.nodeAffinity }}
  nodeAffinity:
  {{- toYaml . | nindent 8 }}
  {{- end }}
  {{- end }}
{{- end }}


{{/*组件拓扑分布*/}}
{{- define "middlware.topologyDistribution" }}
{{- include "middlware.tolerations" . -}}
{{- include "middlware.topologySpreadConstraints" . -}}
{{- include "middlware.affinity" . -}}
{{- end }}

