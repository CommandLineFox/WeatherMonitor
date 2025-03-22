package jobs;

import types.Job;
import types.JobType;

public class ScanJob extends Job {
    private final double minimum;
    private final double maximum;
    private final char startLetter;
    private final String outputFileName;

    public ScanJob(String name, double minimum, double maximum, char startLetter, String outputFileName) {
        super(JobType.SCAN, name);

        this.minimum = minimum;
        this.maximum = maximum;
        this.startLetter = startLetter;
        this.outputFileName = outputFileName;
    }

    @Override
    public void execute() {
        System.out.println("Scanning for words between " + minimum + " and " + maximum + " starting with " + startLetter + " and writing to " + outputFileName);
    }
}
