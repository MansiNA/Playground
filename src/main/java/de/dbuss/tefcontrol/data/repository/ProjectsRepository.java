package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.AgentJobs;
import de.dbuss.tefcontrol.data.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectsRepository extends JpaRepository<Projects, Long> {
    Optional<Projects> findByName(String name);
}
