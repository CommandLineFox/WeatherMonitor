package jobs;

import memory.Memory;
import types.Job;
import types.JobType;
import types.ParsedData;
import types.ReadFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ReadFileJob extends Job {
    private final ReadFile readFile;

    public ReadFileJob(String name, ReadFile readFile) {
        super(JobType.READ_FILE, name);

        this.readFile = readFile;
    }

    @Override
    public void execute() {
        Memory memory = Memory.getInstance();

        if (!memory.markFileAsProcessing(readFile.getName())) {
            return;
        }

        try {
            System.out.println("Processing file: " + readFile.getPath());

            File file = new File(readFile.getPath());
            if (!file.exists()) {
                System.err.println("File not found: " + readFile.getPath());
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                boolean skipHeader = file.getName().endsWith(".csv");
                if (skipHeader) reader.readLine();

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) continue;

                    String stationName = parts[0];
                    double temperature;
                    try {
                        temperature = Double.parseDouble(parts[1]);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    char firstLetter = Character.toUpperCase(stationName.charAt(0));
                    memory.getData().compute(firstLetter, (key, parsedData) -> {
                        if (parsedData == null) parsedData = new ParsedData(0, 0);
                        parsedData.incrementStationCount();
                        parsedData.addTemperature(temperature);
                        return parsedData;
                    });
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + readFile.getPath());
            }

            System.out.println("Finished processing file: " + readFile.getName());

        } finally {
            memory.unmarkFileAsProcessing(readFile.getName());
        }
    }
}
