package jobs;

import types.Job;
import types.JobType;

public class StartJob extends Job {
    private boolean loadJobs;

    public StartJob(String name, boolean loadJobs) {
        super(JobType.START, name);

        this.loadJobs = loadJobs;
    }

    @Override
    public void execute() {

    }
}
