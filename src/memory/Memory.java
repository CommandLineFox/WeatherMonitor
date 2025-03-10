package memory;

import lombok.Getter;
import types.ParsedData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class Memory {
    private final ConcurrentHashMap<Character, AtomicReference<ParsedData>> data = new ConcurrentHashMap<>();
}
