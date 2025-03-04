package types;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ParsedData {
    private char name;
    private int appearanceCount;
    private float valueSum;

    public ParsedData(char name, int appearanceCount, float valueSum) {
        this.name = name;
        this.appearanceCount = appearanceCount;
        this.valueSum = valueSum;
    }
}
