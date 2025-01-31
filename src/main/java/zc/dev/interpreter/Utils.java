package zc.dev.interpreter;

import java.io.File;

public class Utils {
    public static void prnt(Object o) {
        System.out.println(o.toString());
    }

    public static String getCode() {
        String filePath = getFilePath();
        TextFileReader reader = TextFileReader.of(filePath);
        return String.join("\n", reader.readAll());
    }

    private static String getFilePath() {
        File currentDir = new File(".");
        String dir0 = currentDir.getAbsolutePath();
        boolean isWindows = dir0.contains(":\\\\");
        String dir1 = isWindows
                ?"\\src\\main\\java\\zc\\dev\\interpreter\\test_code\\"
                :"/src/main/java/zc/dev/interpreter/test_code/";
        String fileName = "TestCode.java";
        return dir0 + dir1 + fileName;
    }
}
