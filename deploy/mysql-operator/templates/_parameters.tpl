
{{- define "mysql.parameters" }}

{{- $memory := regexFind "[0-9]*[.]*[0-9]*" .Values.resources.limits.memory }}
{{- $memoryB := 0 }}


{{- if contains "." $memory }}
{{- $memoryLs := regexSplit "\\." ($memory | toString) -1 }}
{{- $memoryInt := index $memoryLs 0 }}
{{- $memoryDecimal := index $memoryLs 1 }}
{{- $memoryDecimalLen := len $memoryDecimal }}
{{- $memoryDecimalDigit := 1 }}
{{- range untilStep 0 $memoryDecimalLen 1 }}
{{- $memoryDecimalDigit = mul $memoryDecimalDigit 10 }}
{{- end }}
{{- if contains "GI" (upper .Values.resources.limits.memory) }}
{{- $memoryB = add (mul ($memoryInt | int) 1024 1024 1024) (div (mul ($memoryDecimal | int) 1024 1024 1024) $memoryDecimalDigit) }}
{{- else if contains "G" (upper .Values.resources.limits.memory) }}
{{- $memoryB = add (mul ($memoryInt | int) 1000 1000 1000) (div (mul ($memoryDecimal | int) 1000 1000 1000) $memoryDecimalDigit) }}
{{- else if contains "MI" (upper .Values.resources.limits.memory) }}
{{- $memoryB = add (mul ($memoryInt | int) 1024 1024) (div (mul ($memoryDecimal | int) 1024 1024) $memoryDecimalDigit) }}
{{- else if contains "M" (upper .Values.resources.limits.memory) }}
{{- $memoryB = add (mul ($memoryInt | int) 1000 1000) (div (mul ($memoryDecimal | int) 1000 1000) $memoryDecimalDigit) }}
{{- end }}
{{- else }}
{{- if contains "GI" (upper .Values.resources.limits.memory) }}
{{- $memoryB = mul $memory 1024 1024 1024 }}
{{- else if contains "G" (upper .Values.resources.limits.memory) }}
{{- $memoryB = mul $memory 1000 1000 1000 }}
{{- else if contains "MI" (upper .Values.resources.limits.memory) }}
{{- $memoryB = mul $memory 1024 1024 }}
{{- else if contains "M" (upper .Values.resources.limits.memory) }}
{{- $memoryB = mul $memory 1000 1000 }}
{{- end }}
{{- end }}


{{- $bufferMemory := div $memoryB 2 }}
{{- $singleConnectionMemory := mul (mul 3 1024) 1024 }}
{{- $maxConnectionsMemory := div (div (sub $memoryB $bufferMemory) $singleConnectionMemory) 2 }}
{{- $maxUserConnections := div (mul $maxConnectionsMemory  1) 2 }}

innodb_buffer_pool_size         ={{ .Values.args.innodb_buffer_pool_size | default $bufferMemory  }}
max_connections                 ={{ .Values.args.max_connections | default $maxConnectionsMemory }}
max_user_connections            ={{ .Values.args.max_user_connections | default $maxUserConnections }}
{{- end }}