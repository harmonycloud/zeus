package com.middleware.zeus.util.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dengyulong
 * @date 2019/06/26
 */
public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 写出文件到本地
     *
     * @param fileDir  文件路径，如/home/resource/shell
     * @param fileName 文件名称，如test.txt
     * @param input    输入流
     * @throws IOException
     */
    public static String writeToLocal(String fileDir, String fileName, InputStream input) throws IOException {
        // 如果文件夹不存在，则进行创建
        File dir = new File(fileDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("file dir create fail.");
        }
        String filePath = fileDir + "/" + fileName;
        int index;
        byte[] bytes = new byte[1024];
        FileOutputStream downloadFile = new FileOutputStream(filePath);
        while ((index = input.read(bytes)) != -1) {
            downloadFile.write(bytes, 0, index);
            downloadFile.flush();
        }
        downloadFile.close();
        input.close();

        return filePath;
    }

    /**
     * 写出文件到本地
     *
     * @param fileDir  文件路径，如/home/resource/shell
     * @param fileName 文件名称，如test.txt
     * @param content  文件内容
     * @throws IOException
     */
    public static void writeToLocal(String fileDir, String fileName, String content) throws IOException {
        // 如果文件夹不存在，则进行创建
        // 如果文件夹不存在，则进行创建
        File dir = new File(fileDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("file dir create fail.");
        }
        FileWriter writer;
        String filePath = fileDir + "/" + fileName;
        writer = new FileWriter(filePath);
        writer.write(content);
        writer.flush();
        writer.close();
    }


    /**
     * 写出文件到本地
     *
     * @param file     文件输入流，类型为byte[]
     * @param fileDir  储存的文件路径
     * @param fileName 储存的文件名称
     * @throws IOException
     */
    public static void writeToLocal(String fileDir, String fileName, byte[] file) throws IOException {
        // 如果文件夹不存在，则进行创建
        File dir = new File(fileDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("file dir create fail.");
        }
        FileOutputStream fileOutputStream = new FileOutputStream(fileDir + File.separator + fileName);
        fileOutputStream.write(file);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    /**
     * 读取文件内容
     * 
     * @param fileName  文件绝对路径
     * @throws IOException
     */
    public static void fileReader(String fileName) throws IOException {
        FileReader fileReader = new FileReader(fileName);

        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = bufferedReader.readLine();
        List<String> lines = new ArrayList<>();
        while (line != null) {
            line = line.trim();
            if (StringUtils.isBlank(line)) {
                line = bufferedReader.readLine();
                continue;
            }
            if (!line.startsWith("'")) {
                line = bufferedReader.readLine();
                continue;
            }
            line = line.substring(1, line.indexOf(":") - 1);
            lines.add(line);
            line = bufferedReader.readLine();
        }

        bufferedReader.close();
        fileReader.close();
    }

    public static ArrayList<File> getImageUpdateFiles(Object obj) throws Exception {
        File directory = null;
        if (obj instanceof File) {
            directory = (File)obj;
        } else {
            directory = new File(obj.toString());
        }
        ArrayList<File> files = new ArrayList<File>();
        if (directory.isFile()) {
            if (contains(directory, "image: k8s-deploy")) {
                files.add(directory);
            }
            return files;
        } else if (directory.isDirectory()) {
            if (directory.getName().contains(".git") || directory.getName().contains(".idea")) {
                return files;
            }
            File[] fileArr = directory.listFiles();
            for (int i = 0; i < fileArr.length; i++) {
                File fileOne = fileArr[i];
                files.addAll(getImageUpdateFiles(fileOne));
            }
        }
        return files;
    }

    public static boolean contains(File file, String str) throws Exception {
        FileReader fileReader = new FileReader(file);

        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = bufferedReader.readLine();
        while (line != null) {
            if (line.contains(str)) {
                return true;
            }
            line = bufferedReader.readLine();
        }

        bufferedReader.close();
        fileReader.close();
        return false;
    }

    /**
     * 读取文件
     * 
     * @param filePath 文件绝对路径
     * @return
     * @throws IOException
     */
    public static String readFile(String filePath) throws IOException {
        List<String> strList = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String s;
        while ((s = br.readLine()) != null) {
            strList.add(s);
        }
        return String.join("\n", strList);
    }

    /**
     * 获取文件夹下所有文件名
     * 
     * @param path         文件路径
     * @param listFileName 存放所有文件名称
     */
    public static void getAllFileName(String path, List<String> listFileName) {
        File file = new File(path);
        File[] files = file.listFiles();
        String[] names = file.list();
        if (names != null) {
            String[] completeNames = new String[names.length];
            for (int i = 0; i < names.length; i++) {
                completeNames[i] = path + names[i];
            }
            listFileName.addAll(Arrays.asList(completeNames));
        }
        for (File a : files) {
            // 如果文件夹下有子文件夹，获取子文件夹下的所有文件全路径
            if (a.isDirectory()) {
                getAllFileName(a.getAbsolutePath() + "/", listFileName);
            }
        }
    }

    /**
     * 创建FileItem
     * 
     * @param filePath 文件路径
     * @return
     * @throws IOException
     */
    public static FileItem createFileItem(String filePath) throws IOException {
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        String textFieldName = "textField";
        int num = filePath.lastIndexOf(".");
        FileItem item = factory.createItem(textFieldName, "text/plain", true, "MyFileName");
        File newFile = new File(filePath);
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        FileInputStream fis = new FileInputStream(newFile);
        OutputStream os = item.getOutputStream();
        while ((bytesRead = fis.read(buffer, 0, 8192)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.close();
        fis.close();
        return item;
    }

    /**
     * 递归删除
     *
     * @param filePaths
     */
    public static void deleteFile(String... filePaths) {
        if (filePaths == null || filePaths.length == 0) {
            return;
        }
        File[] files = new File[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            files[i] = new File(filePaths[i]);
        }
        deleteFile(files);
    }
    
    public static void deleteFile(File... files) {
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
            try {
                // 先删除子文件/子目录
                if (file.isDirectory()) {
                    deleteFile(file.listFiles());
                }
                // 删除当前文件/目录
                file.delete();
            } catch (Exception e) {
                LOGGER.error("删除文件失败:{}", file.getAbsolutePath());
            }
        }
    }

    public static Boolean isExists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 获取目录下文件列表
     * @param filePath 目录路径
     * @param recursion 是否递归
     * @return
     */
    public static List<File> getFileList(String filePath, boolean recursion) {
        List<File> fileList = new ArrayList<>();
        File directory = new File(filePath);

        if (!directory.isDirectory()) {
            LOGGER.error("文件路径{}无效", filePath);
            return fileList;
        }

        File[] files = directory.listFiles();
        // 如果该目录下没有文件或者读取失败，返回空列表
        if (files == null || files.length == 0) {
            return fileList;
        }

        for (File file : files) {
            if (file.isFile()) { // 判断是否是文件，如果是则添加到文件列表中
                fileList.add(file);
            } else if (file.isDirectory() && recursion) { // 如果是目录，则递归执行该方法
                fileList.addAll(getFileList(file.getAbsolutePath(), recursion));
            }
        }

        return fileList;
    }

}
