package memory;

import lombok.Getter;
import types.ParsedData;

import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Memory {
    private static volatile Memory instance = null;

    public static Memory getInstance() {
        if (instance == null) {
            synchronized (Memory.class) {
                if (instance == null) {
                    instance = new Memory();
                }
            }
        }
        return instance;
    }

    private final ConcurrentHashMap<Character, ParsedData> data = new ConcurrentHashMap<>();
}