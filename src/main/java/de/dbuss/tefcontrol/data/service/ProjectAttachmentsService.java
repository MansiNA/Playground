package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.repository.ProjectAttachmentsRepository;
import de.dbuss.tefcontrol.data.repository.ProjectsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectAttachmentsService {
    private final ProjectAttachmentsRepository repository;

    public ProjectAttachmentsService(ProjectAttachmentsRepository repository) {
        this.repository = repository;
    }

    public Optional<ProjectAttachments> get(Long id) {
        return repository.findById(id);
    }

    public ProjectAttachments update(ProjectAttachments entity) { return repository.save(entity);}
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<ProjectAttachments> findAll() {
        return repository.findAll();
    }

    public Optional<ProjectAttachments> findById(long id) {
        return repository.findById(id);
    }

}
