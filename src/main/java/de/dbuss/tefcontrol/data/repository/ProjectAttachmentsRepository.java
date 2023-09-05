package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectAttachmentsRepository extends JpaRepository<ProjectAttachments, Long> {
}
