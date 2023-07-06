package com.middleware.zeus.common.constants;

/**
 * @author liusenze
 * @Description:
 * @since 2020/12/29 2:31 下午
 */
public class CommonConstant {
    public static final String ESCAPE_DOT = "\\.";
    public static final String BLANKSTRING = " ";
    public static final String ENTER ="\n";
    public static final String LINE = "-";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String COMMA = ",";
    public static final String STAR = "*";
    public static final String EQUAL = "=";

    public static final String LICENSE = "license";
    public static final String LICENSE_SALT = "salt";
    public static final String LICENSE_MYSQL_LIMIT = "MysqlLimit";
    public static final String LICENSE_REDIS_LIMIT = "RedisLimit";
    public static final String LICENSE_ELASTIC_SEARCH_LIMIT = "ElasticsearchLimit";
    public static final String LICENSE_ROCKET_MQ_LIMIT = "RocketMQLimit";

    public static final int NUM_ZERO = 0;
    public static final int NUM_ONE = 1;
    public static final int NUM_TWO = 2;
    public static final int NUM_THREE = 3;
    public static final int NUM_FOUR = 4;
    public static final int NUM_FIVE = 5;
    public static final int NUM_SIX = 6;
    public static final int NUM_SEVEN = 7;
    public static final int NUM_EIGHT = 8;
    public static final int NUM_NINE = 9;
    public static final int NUM_TEN = 10;
    public static final int NUM_ELEVEN = 11;
    public static final int NUM_TWELVE = 12;
    public static final int NUM_THRITY_ONE = 31;
    public static final int NUM_ONE_THOUSAND = 1000;

    // 生效时间
    public static final String LICENSE_EFFECT_TIME="effectTime";
    // 失效时间
    public static final String LICENSE_INVALID_TIME="invalidTime";
    // 权限列表id
    public static final String LICENSE_PRIVILEGE_LIST_ID="privilegeList";
    // 状态
    public static final String LICENSE_STATUS="status";
    // 已授权
    public static final String LICENSE_STATUS_AUTHORIZED="authorized";
    // 未授权
    public static final String LICENSE_STATUS_UNAUTHORIZED="unauthorized";
    // 已失效
    public static final String LICENSE_STATUS_INVALID="invalid";

    public static final int LICENSE_UNLIMIT = 0;

    public static final int DEFAULT_PAGE_SIZE_10 = 10;
    public static final int DEFAULT_PAGE_SIZE_20 = 20;
    public static final int DEFAULT_PAGE_SIZE_200 = 200;
    public static final int MAX_PAGE_SIZE_1000 = 1000;
    public static final int DEFAULT_LOG_QUERY_TIME = 30;
    public static final String TIME_UNIT_MINUTES = "m";
    public static final String ES_INDEX_RENAME_REPALCEMENT = "$0";
    public static final String ES_REPOSITORY_LOCATION = "location";
    public static final String ES_REPOSITORY_MAX_SNAPSHOT_SPEED = "max_snapshot_bytes_per_sec";
    public static final String ES_REPOSITORY_MAX_RESTORE_SPEED = "max_restore_bytes_per_sec";
    public static final String ES_REPOSITORY_TYPE = "fs";
    public static final String ES_RESTORE_RENAME_PATTERN = ".+";
    public static final String ES_INDEX_SNAPSHOT_RESTORE = "_snapshot";
    public static final String ES_INDEX_LOGSTASH_DATE_FORMAT = "yyyy.MM.dd";
    public static final int ES_INDEX_START_DATE_ADD_DAY_ONE = 1;
    public static final String ES_SNAPSHOT_CREATE_AUTO_PREFIX = "log_auto_";
    public static final String ES_SNAPSHOT_CREATE_MANUAL_PREFIX = "log_manual_";
    public static final String UP = "up";

    public static final int CHAR_2KB = 2048;

    public static final String SIMPLE = "simple";

    public static final String INFO = "info";
    public static final String WARNING = "warning";
    public static final String CRITICAL = "critical";

    public static final String ALREADY_EXISTED = "already exists";

    public static final String RESOURCE_ALREADY_EXISTED = "rendered manifests contain a resource that already exists";

    public static final String PROJECT_ID = "projectId";
    public static final String TYPE = "type";
    public static final String MIDDLEWARE_TYPE = "middlewareType";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String ASTERISK = "*";
    public static final String ALIAS_NAME = "aliasName";
    public static final String REQUEST_QUOTA = "requestQuota";

    public static final String ZONE = "zone";
    public static final String ZONE_A = "zoneA";
    public static final String ZONE_B = "zoneB";

    public static final String INC = "inc";
    public static final String INCR = "incr";

    public static final String MIDDLEWARE_VERSION_PLACEHOLDER_STRING = "middleware_version_placeHolder_string";

    public static final String ON = "ON";
    public static final String OFF = "OFF";

    public static final String PAUSE = "pause";

    public static final String ASC = "asc";
    public static final String DESC = "desc";

    public static final String SKIP_PORT_CONFLICT = "skipPortConflict";
    public static final String HOST_NETWORK = "hostNetwork";

    public static final String CRD_S = "crds";
    public static final String CRD_S_V1 = "crdsv1";
    public static final String CRD_S_V1BETA1 = "crdsv1beta1";
}
