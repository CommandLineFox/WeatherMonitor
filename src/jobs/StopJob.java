package jobs;

import types.Job;
import types.JobType;

public class StopJob extends Job {
    private boolean saveJob;

    public StopJob(String name, boolean saveJob) {
        super(JobType.STOP, name);

        this.saveJob = saveJob;
    }

    @Override
    public void execute() {
    }
}