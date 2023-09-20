package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.AgentJobs;
import de.dbuss.tefcontrol.data.entity.ProjectConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentJobsRepository extends JpaRepository<AgentJobs, Long> {

    Optional<AgentJobs> findByName(String name);
}
