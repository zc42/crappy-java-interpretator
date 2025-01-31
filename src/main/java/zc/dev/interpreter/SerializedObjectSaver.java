package zc.dev.interpreter;

import lombok.SneakyThrows;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static zc.dev.interpreter.Utils.prnt;

public class SerializedObjectSaver {

    private static <T> void _save(Path filePath, T o) throws IOException {
        String fileName = filePath.toAbsolutePath().toString();
        FileOutputStream file = null;
        ObjectOutputStream out = null;
        try {
            file = new FileOutputStream(fileName);
            out = new ObjectOutputStream(file);
            out.writeObject(o);
        } finally {
            if (out != null) {
                out.close();
            }
            if (file != null) {
                file.close();
            }
        }
    }

    @SneakyThrows
    public static <T> void save(String filePath, T object) {
        Path path = getFilePath(filePath);
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            _save(path, object);
        } catch (IOException e) {
            prnt(e.getMessage());
            throw e;
        }
    }

    public static Path getFilePath(String path) {
        return Path.of("", path);
    }
}
