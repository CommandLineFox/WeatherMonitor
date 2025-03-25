package jobs;

import types.JobStatus;
import utility.Memory;
import types.Job;
import types.JobType;
import types.ParsedData;

import java.io.Serializable;
import java.util.Map;

public class MapJob extends Job implements Serializable {
    public MapJob(String name) {
        super(JobType.MAP, name);
    }

    @Override
    public void execute() {
        setJobStatus(JobStatus.RUNNING);
        Memory memory = Memory.getInstance();
        memory.getJobHistory().put(this.getName(), this);

        synchronized (memory.getData()) {
            if (memory.getData().isEmpty()) {
                System.out.println("Map doesn't exist yet.");
                return;
            }

            StringBuilder output = new StringBuilder();
            int count = 0;

            for (Map.Entry<Character, ParsedData> entry : memory.getData().entrySet()) {
                char letter = entry.getKey();
                ParsedData parsedData = entry.getValue();

                output.append(letter).append(": ").append(parsedData.getAppearanceCount()).append(" - ").append(parsedData.getValueSum()).append(" | ");

                if (++count % 2 == 0) {
                    output.append("\n");
                }
            }

            if (count % 2 != 0) {
                output.append("\n");
            }

            System.out.println(output);
        }
    }
}