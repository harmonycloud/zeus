package com.middleware.zeus.util.excel;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class ExcelUtil {

    public static final String OFFICE_EXCEL_XLS = "xls";
    public static final String OFFICE_EXCEL_XLSX = "xlsx";

    /**
     * 读取指定Sheet也的内容
     *
     * @param filepath filepath 文件全路径
     * @param sheetNo  sheet序号,从0开始,如果读取全文sheetNo设置null
     */
    public static String readExcel(String filepath, Integer sheetNo)
            throws EncryptedDocumentException, InvalidFormatException, IOException {
        StringBuilder sb = new StringBuilder();
        Workbook workbook = getWorkbook(filepath);
        if (workbook != null) {
            if (sheetNo == null) {
                int numberOfSheets = workbook.getNumberOfSheets();
                for (int i = 0; i < numberOfSheets; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    if (sheet == null) {
                        continue;
                    }
                    sb.append(readExcelSheet(sheet));
                }
            } else {
                Sheet sheet = workbook.getSheetAt(sheetNo);
                if (sheet != null) {
                    sb.append(readExcelSheet(sheet));
                }
            }
        }
        return sb.toString();
    }

    /**
     * 根据文件路径获取Workbook对象
     *
     * @param filepath 文件全路径
     */
    public static Workbook getWorkbook(String filepath)
            throws EncryptedDocumentException, InvalidFormatException, IOException {
        InputStream is = null;
        Workbook wb = null;
        if (StringUtils.isBlank(filepath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        } else {
            String suffiex = getSuffiex(filepath);
            if (StringUtils.isBlank(suffiex)) {
                throw new IllegalArgumentException("文件后缀不能为空");
            }
            if (OFFICE_EXCEL_XLS.equals(suffiex) || OFFICE_EXCEL_XLSX.equals(suffiex)) {
                try {
                    is = new FileInputStream(filepath);
                    wb = WorkbookFactory.create(is);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                    if (wb != null) {
                        wb.close();
                    }
                }
            } else {
                throw new IllegalArgumentException("该文件非Excel文件");
            }
        }
        return wb;
    }

    /**
     * 获取后缀
     *
     * @param filepath filepath 文件全路径
     */
    private static String getSuffiex(String filepath) {
        if (StringUtils.isBlank(filepath)) {
            return "";
        }
        int index = filepath.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return filepath.substring(index + 1, filepath.length());
    }

    private static String readExcelSheet(Sheet sheet) {
        StringBuilder sb = new StringBuilder();

        if (sheet != null) {
            int rowNos = sheet.getLastRowNum();// 得到excel的总记录条数
            for (int i = 0; i <= rowNos; i++) {// 遍历行
                Row row = sheet.getRow(i);
                if (row != null) {
                    int columNos = row.getLastCellNum();// 表头总共的列数
                    for (int j = 0; j < columNos; j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null) {
                            cell.setCellType(CellType.STRING);
                            sb.append(cell.getStringCellValue() + " ");
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * 读取指定Sheet页的表头
     *
     * @param filepath filepath 文件全路径
     * @param sheetNo  sheet序号,从0开始,必填
     */
    public static Row readTitle(String filepath, int sheetNo)
            throws IOException, EncryptedDocumentException, InvalidFormatException {
        Row returnRow = null;
        Workbook workbook = getWorkbook(filepath);
        if (workbook != null) {
            Sheet sheet = workbook.getSheetAt(sheetNo);
            returnRow = readTitle(sheet);
        }
        return returnRow;
    }

    /**
     * 读取指定Sheet页的表头
     */
    public static Row readTitle(Sheet sheet) throws IOException {
        Row returnRow = null;
        int totalRow = sheet.getLastRowNum();// 得到excel的总记录条数
        for (int i = 0; i < totalRow; i++) {// 遍历行
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            returnRow = sheet.getRow(0);
            break;
        }
        return returnRow;
    }

    /**
     * 创建Excel文件
     *
     * @param suffiex      文件后缀
     * @param sheetName    新Sheet页的名字
     * @param beans        需要合并的单元格，仅支持在标题上方合并 (目前仅支持同一行多列合并)
     * @param secondTitles 表头
     * @param values       每行的单元格
     */
    public static void writeExcel(String suffiex, String sheetName, List<ExcelBean> beans, Map<String, String> secondTitles,
                                  List<Map<String, Object>> values, HttpServletResponse response, HttpServletRequest request) throws IOException {

        Workbook workbook;
        if ("xls".equals(suffiex.toLowerCase())) {
            workbook = new HSSFWorkbook();
        } else {
            workbook = new XSSFWorkbook();
        }

        // 生成一个表格
        Sheet sheet;
        if (StringUtils.isBlank(sheetName)) {
            // name 为空则使用默认值
            sheet = workbook.createSheet();
        } else {
            sheet = workbook.createSheet(sheetName);
        }
        // 设置表格默认列宽度为15个字节
        sheet.setDefaultColumnWidth((short) 15);
        // 设置样式
        XSSFCellStyle cellStyle = (XSSFCellStyle) workbook.createCellStyle();
        cellStyle.setWrapText(true); // 自动换行
        cellStyle.setAlignment(HorizontalAlignment.CENTER); // 居中

        // 设置需要合并单元格的行
        int unused = 0; // 还未被占用的行号
        Map<String, Row> rowMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(beans)) {
            // 单元格结束行
            Integer lastRow = null;

            for (ExcelBean bean : beans) {
                CellRangeAddress callRangeAddress = bean.getCallRangeAddress();
                if (callRangeAddress.getFirstColumn() != callRangeAddress.getLastColumn()) {
                    sheet.addMergedRegion(callRangeAddress);
                }
                // 仅支持同一行合并
                if (lastRow != null && lastRow != callRangeAddress.getLastRow()) {
                    return;
                }
                lastRow = callRangeAddress.getLastRow();
                if (lastRow >= unused) {
                    unused = lastRow + 1;
                }

                Row row = rowMap.get(lastRow + "");
                if (row == null) {
                    row = sheet.createRow(lastRow);
                    rowMap.put(lastRow + "", row);
                }
                Cell cell = row.createCell(callRangeAddress.getFirstColumn());
                // 设置cell的样式
                cell.setCellStyle(cellStyle);
                cell.setCellValue(bean.getValue());
            }
        }
        // 创建标题行
        Row row = sheet.createRow(unused);
        //加载单元格样式
        // 存储标题在Excel文件中的序号
        Map<String, Integer> titleOrder = Maps.newHashMap();

        Set<String> titles = secondTitles.keySet();
        for (String titleKey : titles) {
            Integer index = Integer.valueOf(titleKey);
            Cell cell = row.createCell(index);
            // 设置cell的样式
            cell.setCellStyle(cellStyle);
            cell.setCellValue(secondTitles.get(titleKey));
            titleOrder.put(titleKey, index);
        }
        // 写入正文
        Iterator<Map<String, Object>> iterator = values.iterator();
        // 行号
        int index = unused + 1;
        while (iterator.hasNext()) {
            row = sheet.createRow(index);
            Map<String, Object> value = iterator.next();
            for (Map.Entry<String, Object> map : value.entrySet()) {
                // 获取列名
                String title = map.getKey();
                // 根据列名获取序号
                int i = titleOrder.get(title);
                // 在指定序号处创建cell
                Cell cell = row.createCell(i);
                // 获取列的值
                Object object = map.getValue();
                // 判断object的类型
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (object instanceof Double) {
                    cell.setCellValue((Double) object);
                } else if (object instanceof Date) {
                    String time = simpleDateFormat.format((Date) object);
                    cell.setCellValue(time);
                } else if (object instanceof Calendar) {
                    Calendar calendar = (Calendar) object;
                    String time = simpleDateFormat.format(calendar.getTime());
                    cell.setCellValue(time);
                } else if (object instanceof Boolean) {
                    cell.setCellValue((Boolean) object);
                } else {
                    if (object != null) {
                        cell.setCellValue(object.toString());
                    }
                }
            }
            index++;
        }
        response.reset();
        response.setContentType("application/octet-stream");
        response.addHeader("Content-disposition", "attachment;filename=" + new String((sheetName + "." + suffiex).getBytes("UTF-8"), "ISO-8859-1"));
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        workbook.write(response.getOutputStream());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    /**
     * 设置格式
     */
    private static Map<String, CellStyle> createStyles(Workbook wb) {
        Map<String, CellStyle> styles = Maps.newHashMap();

        // 标题样式
        XSSFCellStyle titleStyle = (XSSFCellStyle) wb.createCellStyle();
        titleStyle.setAlignment(HorizontalAlignment.CENTER); // 水平对齐
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直对齐
        titleStyle.setLocked(true); // 样式锁定
        titleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        Font titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleFont.setFontName("微软雅黑");
        titleStyle.setFont(titleFont);
        styles.put("title", titleStyle);

        // 文件头样式
        XSSFCellStyle headerStyle = (XSSFCellStyle) wb.createCellStyle();
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex()); // 前景色
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); // 颜色填充方式
        headerStyle.setWrapText(true);
        headerStyle.setBorderRight(BorderStyle.THIN); // 设置边界
        headerStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        Font headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setFontName("微软雅黑");
        headerStyle.setFont(headerFont);
        styles.put("header", headerStyle);

        Font cellStyleFont = wb.createFont();
        cellStyleFont.setFontHeightInPoints((short) 12);
        cellStyleFont.setColor(IndexedColors.BLUE_GREY.getIndex());
        cellStyleFont.setFontName("微软雅黑");

        // 正文样式A
        XSSFCellStyle cellStyleA = (XSSFCellStyle) wb.createCellStyle();
        cellStyleA.setAlignment(HorizontalAlignment.CENTER); // 居中设置
        cellStyleA.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyleA.setWrapText(true);
        cellStyleA.setBorderRight(BorderStyle.THIN);
        cellStyleA.setRightBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleA.setBorderLeft(BorderStyle.THIN);
        cellStyleA.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleA.setBorderTop(BorderStyle.THIN);
        cellStyleA.setTopBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleA.setBorderBottom(BorderStyle.THIN);
        cellStyleA.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleA.setFont(cellStyleFont);
        styles.put("cellA", cellStyleA);

        // 正文样式B:添加前景色为浅黄色
        XSSFCellStyle cellStyleB = (XSSFCellStyle) wb.createCellStyle();
        cellStyleB.setAlignment(HorizontalAlignment.CENTER);
        cellStyleB.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyleB.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        cellStyleB.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyleB.setWrapText(true);
        cellStyleB.setBorderRight(BorderStyle.THIN);
        cellStyleB.setRightBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleB.setBorderLeft(BorderStyle.THIN);
        cellStyleB.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleB.setBorderTop(BorderStyle.THIN);
        cellStyleB.setTopBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleB.setBorderBottom(BorderStyle.THIN);
        cellStyleB.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        cellStyleB.setFont(cellStyleFont);
        styles.put("cellB", cellStyleB);

        return styles;
    }

    public static void getExcel(Map<String, Object> data, String sheetName, HttpServletResponse response, HttpServletRequest request) throws IOException {
        // 组合数据源
        List<Map<String, Object>> values = new ArrayList<>();

        Set<String> keySet = data.keySet();
        // 当前行的所有值 ("时间",("单元格坐标","值集"))
        Map<String, Map<String, Object>> rowValuesMap = new HashMap<>();

        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("0", ExcelTitleEnum.getTitle("date"));
        List<ExcelBean> beanList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(keySet)) {
            // 未被占用的单元格位置 从1位开始，0位放置时间
            int unusedIndex = 1;
            ExcelBean timeBean = new ExcelBean();
            timeBean.setCallRangeAddress(new CellRangeAddress(0, 0, 0, 0));
            timeBean.setValue(ExcelTitleEnum.getTitle("title"));
            beanList.add(timeBean);

            String title = null;
            for (String key : keySet) {
                Object value = data.get(key);

                // 容器各个指标的值集eg.("cpuUsing",("pod1-constainer1",[[2019-05-23 10:00:00,0.1],[2019-05-23 10:01:00,0.2]]))
                Map<String, List<List<String>>> pvcUsage = (Map<String, List<List<String>>>) value;

                title = ExcelTitleEnum.getTitle(key);
                if (StringUtils.isNotEmpty(title) || StringUtils.containsIgnoreCase(key, "download")
                        || StringUtils.containsIgnoreCase(key, "upload")) {
                    if (StringUtils.isEmpty(title)) {
                        title = key;
                    }
                    // 一级标题
                    ExcelBean bean = new ExcelBean();
                    bean.setCallRangeAddress(new CellRangeAddress(0, 0, unusedIndex,
                            unusedIndex + (pvcUsage.size() == 0 ? 0 : pvcUsage.size() - 1)));
                    bean.setValue(title);
                    unusedIndex = cellValues(rowValuesMap, pvcUsage, titleMap, unusedIndex, values);
                    beanList.add(bean);
                    title = null;
                }

            }
        }

        ExcelUtil.writeExcel(ExcelUtil.OFFICE_EXCEL_XLSX, sheetName, beanList, titleMap, values, response, request);

    }

    /**
     * 组合单元格的值
     *
     * @param rowValuesMap 以行为单位的值集
     * @param pvcUsage
     * @param titleMap
     * @param unusedIndex
     * @param values
     */
    public static Integer cellValues(Map<String, Map<String, Object>> rowValuesMap, Map<String, List<List<String>>> pvcUsage, Map<String, String> titleMap, Integer unusedIndex, List<Map<String, Object>> values) {
        if (!CollectionUtils.isEmpty(pvcUsage)) {
            Integer index = unusedIndex;
            for (String pvc : pvcUsage.keySet()) {
                List<List<String>> pvcUsageValue = (List<List<String>>) pvcUsage.get(pvc);
                titleMap.put(index + "", pvc);
                if (CollectionUtils.isEmpty(pvcUsageValue)) {
                    index++;
                    continue;
                }
                for (List<String> pvcValue : pvcUsageValue) {
                    Map<String, Object> rowValue = rowValuesMap.get(pvcValue.get(0));
                    if (CollectionUtils.isEmpty(rowValue)) {
                        rowValue = new HashMap<>();
                        rowValue.put("0", pvcValue.get(0));
                        rowValuesMap.put(pvcValue.get(0), rowValue);
                        values.add(rowValue);
                    }
                    rowValue.put(index + "", pvcValue.get(1));
                }
                index++;
            }
        }
        unusedIndex = unusedIndex + (pvcUsage.size() == 0 ? 1 : pvcUsage.size());
        return unusedIndex;
    }

    public static List<List<Object>> getListByExcel(InputStream in, String fileName) throws Exception {
        List<List<Object>> list = null;
        //创建Excel工作薄
        Workbook work = getWorkbook(in, fileName);
        if (null == work) {
            throw new Exception("创建Excel工作薄为空！");
        }
        Sheet sheet = null;
        Row row = null;
        Cell cell = null;
        list = new ArrayList<List<Object>>();
        //获取第一张表
        sheet = work.getSheetAt(0);
        //遍历当前sheet中的所有行
        //包涵头部，所以要小于等于最后一列数,这里也可以在初始值加上头部行数，以便跳过头部
        for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++) {
            //读取一行
            row = sheet.getRow(j);
            //去掉空行和表头
            if (row == null || row.getFirstCellNum() == j) {
                continue;
            }
            //遍历所有的列
            List<Object> li = new ArrayList<Object>();
            for (int y = row.getFirstCellNum(); y < row.getLastCellNum(); y++) {
                cell = row.getCell(y);
                if (cell == null) {
                    continue;
                }
                li.add(getCellValue(cell));
            }
            list.add(li);
        }
        return list;
    }

    /**
     * 描述：根据文件后缀，自适应上传文件的版本
     */
    public static Workbook getWorkbook(InputStream inStr, String fileName) throws Exception {
        Workbook wb = null;
        String fileType = getSuffiex(fileName);
        if (OFFICE_EXCEL_XLS.equals(fileType)) {
            wb = new HSSFWorkbook(inStr);  //2003-
        } else if (OFFICE_EXCEL_XLSX.equals(fileType)) {
            // wb = new XSSFWorkbook(inStr);  //2007+
        } else {
            throw new Exception("解析的文件格式有误！");
        }
        return wb;
    }

    /**
     * 描述：对表格中数值进行格式化
     */
    public static Object getCellValue(Cell cell) {
        Object value = null;
        DecimalFormat df = new DecimalFormat("0");  //格式化字符类型的数字
/*        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");  //日期格式化
        DecimalFormat df2 = new DecimalFormat("0.00");  //格式化数字
*/
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getRichStringCellValue().getString();
                break;
            case NUMERIC:
                if ("General".equals(cell.getCellStyle().getDataFormatString())) {
                    value = df.format(cell.getNumericCellValue());
                }/*else if("m/d/yy".equals(cell.getCellStyle().getDataFormatString())){
                    value = sdf.format(cell.getDateCellValue());
                }else{
                    value = df2.format(cell.getNumericCellValue());
                }*/
                break;
            case BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            case BLANK:
                value = "";
                break;
            default:
                break;
        }
        return value;
    }

}

