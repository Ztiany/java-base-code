package l10.v4.foo;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2018/11/24 11:39
 */
public class Foo {

    public static final String COMMAND_EXIT = "00bye00";
    private static final String CACHE_DIR = "cache";

    public static final String COMMAND_GROUP_JOIN = "--m g join";
    public static final String COMMAND_GROUP_LEAVE = "--m g leave";

    public static final String DEFAULT_GROUP_NAME = "IMOOC";

    public static File getCacheDir(String dir) {
        String path = System.getProperty("user.dir") + (File.separator + CACHE_DIR + File.separator + dir);
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("Create path error:" + path);
            }
        }
        return file;
    }

    public static File createRandomTemp(File parent) {
        String string = UUID.randomUUID().toString() + ".tmp";
        File file = new File(parent, string);
        try {
            boolean ignore = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

}