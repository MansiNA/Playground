package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.AgentJobs;
import de.dbuss.tefcontrol.data.repository.AgentJobsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentJobsService {
    private final AgentJobsRepository repository;

    public AgentJobsService(AgentJobsRepository repository) {
        this.repository = repository;
    }

    public List<AgentJobs> findAll() {
        return repository.findAll();
    }

    public List<AgentJobs> findbyJobName(String jobNames) {

        List<AgentJobs> angentJobs = new ArrayList<>();

        if (jobNames != null) {
            String[] teile = jobNames.split(";");
            for (String teil : teile) {
                angentJobs.add(repository.search(teil));
            }
        }

        return angentJobs;
    }

}
