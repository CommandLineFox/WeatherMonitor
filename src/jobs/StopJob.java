package jobs;

import memory.Memory;
import types.Job;
import types.JobStatus;
import types.JobType;

public class StopJob extends Job {
    private boolean saveJob;

    public StopJob(String name, boolean saveJob) {
        super(JobType.STOP, name);

        this.saveJob = saveJob;
    }

    @Override
    public void execute() {
        Memory memory = Memory.getInstance();
        memory.getJobQueue().forEach((job -> {
            if (job.getJobStatus() == JobStatus.PENDING) {
                //TODO
            }
        }));

        memory.getJobDispatcherThread().interrupt();
        memory.getDirectoryMonitorThread().interrupt();
        memory.getCliThread().interrupt();
        System.out.println("Stopped.");
    }
}