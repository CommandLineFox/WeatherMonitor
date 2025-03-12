package jobs;

import types.Job;
import types.JobType;
import types.ReadFile;

public class ReadFileJob extends Job {
    private ReadFile readFile;

    public ReadFileJob(String name, ReadFile readFile) {
        super(JobType.READ_FILE, name);

        this.readFile = readFile;
    }

    @Override
    public void execute() {

    }
}
