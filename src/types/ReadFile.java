package types;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ReadFile implements Serializable {
    private String name;
    private String path;
    private long lastModified;

    public ReadFile(String name, String path, long lastModified) {
        this.name = name;
        this.path = path;
        this.lastModified = lastModified;
    }
}
