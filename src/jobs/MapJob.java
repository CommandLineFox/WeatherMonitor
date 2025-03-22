package jobs;

import types.Job;
import types.JobType;

public class MapJob extends Job {
    public MapJob(String name) {
        super(JobType.MAP, name);
    }

    @Override
    public void execute() {
        System.out.println("Map job " + getName() + " executed.");
    }
}
