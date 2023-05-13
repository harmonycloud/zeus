package com.middleware.zeus.common.constants;

/**
 * @author wangpenglei
 * @Date 2023/2/6 上午9:53
 **/
public class CmdConstant {
    public final static String POSTGRESQL_HAND_SWITCH = "kubectl exec {0} -n {1} -c postgres --server={2} --token={3} --insecure-skip-tls-verify=true -- bash -c \"curl -s -X POST -w '''\\n%{http_code}\\n''' http://{4}:{5}/failover -d '''{\\\"candidate\\\": \\\"'{6}'\\\"}'''\"";
    public final static String POSTGRESQL_AUTO_SWITCH = "kubectl exec {0} -n {1} -c postgres --server={2} --token={3} --insecure-skip-tls-verify=true -- bash -c \"curl -s -X PATCH -d '''{\\\"pause\\\": '{4}' }''' http://{5}:{6}/config | jq .\"";
    public final static String POSTGRESQL_AUTO_SWITCH_STATUS = "kubectl exec {0} -n {1} -c postgres --server={2} --token={3} --insecure-skip-tls-verify=true -- bash  -c \"curl -s http://{4}:{5}/patroni | jq .\"";

    public final static String MYSQL_HAND_SWITCH = "kubectl exec {0} -n {1} -c mysql --server={2} --token={3} --insecure-skip-tls-verify=true -- bash -c \"curl -s -X POST -w '''\\n%{http_code}\\n''' http://mysql-operator.middleware-operator:8080/failover -d '''{\\\"candidate\\\": \\\"'{4}'\\\",\\\"namespace\\\":\\\"'{5}'\\\",\\\"name\\\":\\\"'{6}'\\\"}'''\"";

    public static final String ZEUS_MYSQL_VALUES = "helm get values -n %s zeus-mysql -a";
}
