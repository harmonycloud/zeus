package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.model.dashboard.mysql.ColumnDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/10/24 3:14 下午
 */
@Slf4j
public class ExcelUtil {

    /**
     * 创建一个Excel文件，并写入指定内容
     * @param filePath 文件路径
     * @param table 表名
     * @param columnDtoList 列信息
     * @return 文件绝对路径
     */
    public static String createTableExcel(String filePath, String table, List<ColumnDto> columnDtoList) {
        String pathName = filePath + table + ".xlsx";
        try {
            File file = new File(pathName);
            file.deleteOnExit();
            if (!file.createNewFile()) {
                log.error("创建文件失败");
            }

            //创建一个excel文件
            XSSFWorkbook workbook = new XSSFWorkbook();
            //创建一个sheet，名称为 table
            XSSFSheet sheet = workbook.createSheet(table);
            XSSFRow titleRow = sheet.createRow(0);
            XSSFCell index = titleRow.createCell(0);
            index.setCellValue("");

            XSSFCell column = titleRow.createCell(1);
            column.setCellValue("字段名");

            XSSFCell dataType = titleRow.createCell(2);
            dataType.setCellValue("类型");

            XSSFCell comment = titleRow.createCell(3);
            comment.setCellValue("描述");

            XSSFCell nullAble = titleRow.createCell(4);
            nullAble.setCellValue("可空");

            XSSFCell autoIncrement = titleRow.createCell(5);
            autoIncrement.setCellValue("自增");

            XSSFCell columnDefault = titleRow.createCell(6);
            columnDefault.setCellValue("默认值");

            int rowNum = 1;
            for (ColumnDto columnDto : columnDtoList) {
                XSSFRow valueRow = sheet.createRow(rowNum);

                XSSFCell indexCell = valueRow.createCell(0);
                indexCell.setCellValue(rowNum);

                XSSFCell columnCell = valueRow.createCell(1);
                columnCell.setCellValue(columnDto.getColumn());

                XSSFCell dataTypeCell = valueRow.createCell(2);
                dataTypeCell.setCellValue(columnDto.getDateType());

                XSSFCell commentCell = valueRow.createCell(3);
                commentCell.setCellValue(columnDto.getComment());

                XSSFCell nullAbleCell = valueRow.createCell(4);
                nullAbleCell.setCellValue(columnDto.getNullable());

                XSSFCell autoIncrementCell = valueRow.createCell(5);
                autoIncrementCell.setCellValue(columnDto.isAutoIncrement());

                XSSFCell columnDefaultCell = valueRow.createCell(6);
                columnDefaultCell.setCellValue(columnDto.getColumnDefault());

                rowNum++;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(pathName);
            workbook.write(fileOutputStream);
        } catch (IOException e) {
            log.error("创建Excel文件失败", e);
        }
        return pathName;
    }

}
