package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * mysql 数据表引擎
 *
 * @author liyinlong
 * @since 2022/3/29 10:01 上午
 */
public enum MysqlEngineEnum {

    INNODB("InnoDB", new String[]{"INDEX", "UNIQUE", "FULLTEXT", "STATIAL", "PRIMARY"}, new String[]{"BTREE"}, true),
    MYISAM("MyISAM", new String[]{"INDEX", "UNIQUE", "FULLTEXT", "STATIAL", "PRIMARY"}, new String[]{"BTREE", "RTREE"}, false),
    NDBCLUSTER("ndbcluster", new String[]{"INDEX", "UNIQUE", "PRIMARY"}, new String[]{"BTREE", "HASH"}, true),
    MEMORY("MEMORY", new String[]{"INDEX", "UNIQUE", "PRIMARY"}, new String[]{"BTREE", "HASH"}, false),
    FEDERATED("FEDERATED", new String[]{"INDEX", "UNIQUE", "PRIMARY"}, new String[]{"BTREE"}, false),
    ARCHIVE("ARCHIVE", new String[]{"INDEX", "UNIQUE", "PRIMARY"}, new String[]{"BTREE"}, false),
    CSV("CSV", new String[]{"INDEX", "UNIQUE", "PRIMARY"}, new String[]{"BTREE"}, false),
    BLACKHOLE("BLACKHOLE", new String[]{"INDEX", "UNIQUE", "PRIMARY"}, new String[]{"BTREE"}, false),
    MRG_MYISAM("MRG_MyISAM", new String[]{"INDEX", "UNIQUE", "PRIMARY"}, new String[]{"BTREE"}, false),
    ;

    private final String engine;
    private final String[] indexTypes;
    private final String[] storageTypes;
    private final Boolean supportForeignKey;

    public static Map<String, MysqlEngineEnum> engineEnumMap = new HashMap<>(16);

    static {
        engineEnumMap.put(INNODB.engine, INNODB);
        engineEnumMap.put(MYISAM.engine, MYISAM);
        engineEnumMap.put(NDBCLUSTER.engine, NDBCLUSTER);
        engineEnumMap.put(MEMORY.engine, MEMORY);
        engineEnumMap.put(FEDERATED.engine, FEDERATED);
        engineEnumMap.put(ARCHIVE.engine, ARCHIVE);
        engineEnumMap.put(CSV.engine, CSV);
        engineEnumMap.put(BLACKHOLE.engine, BLACKHOLE);
        engineEnumMap.put(MRG_MYISAM.engine, MRG_MYISAM);
    }

    MysqlEngineEnum(String engine, String[] indexTypes, String[] storageTypes, Boolean supportForeignKey) {
        this.engine = engine;
        this.indexTypes = indexTypes;
        this.storageTypes = storageTypes;
        this.supportForeignKey = supportForeignKey;
    }

    public String getEngine() {
        return engine;
    }

    public String[] getIndexTypes() {
        return indexTypes;
    }

    public String[] getStorageTypes() {
        return storageTypes;
    }

    public Boolean getSupportForeignKey() {
        return supportForeignKey;
    }

    public static MysqlEngineEnum findByEngineName(String engine) {
        return engineEnumMap.get(engine);
    }

}
