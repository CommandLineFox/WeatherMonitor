package jobs;

import lombok.Getter;
import utility.Memory;
import types.*;

import java.io.*;

@Getter
public class ReadFileJob extends Job implements Serializable {
    private final ReadFile readFile;

    public ReadFileJob(String name, ReadFile readFile) {
        super(JobType.READ_FILE, name);
        this.readFile = readFile;
    }

    @Override
    public void execute() {
        setJobStatus(JobStatus.RUNNING);
        Memory memory = Memory.getInstance();
        memory.getJobHistory().put(this.getName(), this);

        File file = new File(readFile.getPath());
        if (!file.exists()) {
            System.err.println("File not found: " + readFile.getPath());
            return;
        }

        boolean success = true;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            boolean skipHeader = file.getName().endsWith(".csv");
            if (skipHeader) {
                reader.readLine();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (!memory.getRunning().get()) {
                    System.out.println("Shutting down ReadFileJob due to system shutdown.");
                    success = false;
                    break;
                }

                processLine(line, memory);
            }
        } catch (IOException e) {
            success = false;
            System.err.println("Error reading file: " + readFile.getPath());
        }

        setJobStatus(JobStatus.COMPLETED);
        if (success) {
            System.out.println("Finished processing file: " + readFile.getName());
        }
    }

    /**
     * Procesovanje linija u fajlu
     *
     * @param line   Line to process
     * @param memory Memory instance
     */
    private void processLine(String line, Memory memory) {
        if (!memory.getRunning().get()) {
            return;
        }

        String[] parts = line.split("[;,]");
        if (parts.length < 2) {
            return;
        }

        String stationName = parts[0].trim();
        double temperature;
        try {
            temperature = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            return;
        }

        char firstLetter = Character.toUpperCase(stationName.charAt(0));

        synchronized (memory.getData()) {
            memory.getData().compute(firstLetter, (key, parsedData) -> {
                if (parsedData == null) {
                    parsedData = new ParsedData(0, 0);
                }

                parsedData.incrementAppearanceCount();
                parsedData.addValue(temperature);
                return parsedData;
            });
        }
    }
}