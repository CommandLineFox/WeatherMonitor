package jobs;

import types.Job;
import types.JobType;

public class ScanJob extends Job {
    private float minimum;
    private float maximum;
    private char startLetter;
    private String outputFileName;

    public ScanJob(String name, float minimum, float maximum, char startLetter, String outputFileName) {
        super(JobType.SCAN, name);

        this.minimum = minimum;
        this.maximum = maximum;
        this.startLetter = startLetter;
        this.outputFileName = outputFileName;
    }

    @Override
    public void execute() {

    }
}
