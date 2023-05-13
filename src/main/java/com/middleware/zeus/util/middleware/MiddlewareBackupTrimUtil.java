package com.middleware.zeus.util.middleware;

import com.middleware.zeus.common.model.middleware.MiddlewareBackupRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liyinlong
 * @since 2023/5/12 11:15 上午
 */
public class MiddlewareBackupTrimUtil {

    public static List<MiddlewareBackupRecord> trimBackup(List<MiddlewareBackupRecord> records){
        List<MiddlewareBackupRecord> filteredRecords = new ArrayList<>();
        if (records.size() == 2) {
            MiddlewareBackupRecord recordA = records.get(0);
            MiddlewareBackupRecord recordB = records.get(1);
            if(recordA.getPosition().equals(recordB.getPosition())){
                recordA.setSameActiveActiveBackup(true);
                filteredRecords.add(recordA);
                return filteredRecords;
            }
        }
        records.forEach(middlewareBackupRecord -> {
            middlewareBackupRecord.setSameActiveActiveBackup(false);
        });
        return records;
    }

    public static List<MiddlewareBackupRecord> trimScheduleBackup(List<MiddlewareBackupRecord> records) {
        List<MiddlewareBackupRecord> filteredRecords = new ArrayList<>();
        if (records.size() == 2) {
            MiddlewareBackupRecord recordA = records.get(0);
            MiddlewareBackupRecord recordB = records.get(1);
            if (recordA.getPosition().equals(recordB.getPosition())
                    && recordA.getCron().equals(recordB.getCron())
                    && (recordA.getLimitRecord().equals(recordB.getLimitRecord()))) {
                recordA.setSameActiveActiveBackup(true);
                filteredRecords.add(recordA);
                return filteredRecords;
            }
        }
        records.forEach(middlewareBackupRecord -> {
            middlewareBackupRecord.setSameActiveActiveBackup(false);
        });
        return records;
    }

}
