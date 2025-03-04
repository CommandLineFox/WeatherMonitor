package types;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReadFile {
    private String name;
    private String path;
    private long lastModified;

    public ReadFile(String name, String path, long lastModified) {
        this.name = name;
        this.path = path;
        this.lastModified = lastModified;
    }
}
