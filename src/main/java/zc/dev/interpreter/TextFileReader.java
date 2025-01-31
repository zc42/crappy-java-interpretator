package zc.dev.interpreter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static zc.dev.interpreter.SerializedObjectSaver.getFilePath;

@RequiredArgsConstructor(staticName = "from")
public class TextFileReader {
    private final Path filePath;

    public static TextFileReader of(String filePath) {
        Path path = getFilePath(filePath);
        return TextFileReader.from(path);
    }

    @SneakyThrows
    synchronized public List<String> readAll() {
        Path path = filePath.toAbsolutePath();
        return Files.lines(path, Charset.defaultCharset())
                .collect(Collectors.toList());
    }

}
