package jobs;

import memory.Memory;
import types.Job;
import types.JobType;
import types.ParsedData;

import java.util.Map;

public class MapJob extends Job {
    public MapJob(String name) {
        super(JobType.MAP, name);
    }

    @Override
    public void execute() {
        Memory memory = Memory.getInstance();

        synchronized (memory.getData()) {
            if (memory.getData().isEmpty()) {
                System.out.println("Mapa još uvek nije dostupna.");
                return;
            }
        }

        StringBuilder output = new StringBuilder();
        int count = 0;

        synchronized (memory.getData()) {
            for (Map.Entry<Character, ParsedData> entry : memory.getData().entrySet()) {
                char letter = entry.getKey();
                ParsedData parsedData = entry.getValue();
                output.append(letter).append(": ").append(parsedData.getAppearanceCount()).append(" - ").append(parsedData.getValueSum()).append(" | ");

                count++;

                if (count % 2 == 0) {
                    output.append("\n");
                }
            }

            if (count % 2 != 0) {
                output.append("\n");
            }
        }

        System.out.println(output);
    }
}
