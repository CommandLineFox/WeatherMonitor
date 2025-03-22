package types;

import lombok.Getter;

@Getter
public abstract class Job {
    private final JobType type;
    private final String name;
    private JobStatus jobStatus;

    public Job(JobType type, String name) {
        this.type = type;
        this.name = name;
        this.jobStatus = JobStatus.PENDING;
    }

    protected void setjobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public abstract void execute();
}