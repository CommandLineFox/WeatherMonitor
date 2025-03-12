package types;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ParsedData {
    private int appearanceCount;
    private float valueSum;

    public ParsedData(int appearanceCount, float valueSum) {
        this.appearanceCount = appearanceCount;
        this.valueSum = valueSum;
    }
}
