package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectsRepository extends JpaRepository<Projects, Long> {
}
