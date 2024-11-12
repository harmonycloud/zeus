package com.middleware.zeus.common.model.dashboard;

import com.middleware.zeus.common.model.BackupPositionDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author wangpenglei
 * @data 2024/11/8 14:23
 */
@Data
@Accessors(chain = true)
public class BackupPositionDTOList {

    private List<BackupPositionDTO> backupPositionDTOList;
}
