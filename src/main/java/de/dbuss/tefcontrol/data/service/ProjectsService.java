package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.repository.ProjectsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectsService {

    private final ProjectsRepository repository;
    private List<Projects> projectsList;

    public ProjectsService(ProjectsRepository repository) {
        this.repository = repository;
        this.projectsList = repository.findAll();
    }

    public Optional<Projects> get(Long id) {
        return repository.findById(id);
    }

    public Projects update(Projects entity) { return repository.save(entity);}

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Projects> findAll() {
        return repository.findAll();
    }

    public Projects findByName(String jobName) {
        return repository.findByName(jobName).get();
    }


    public Optional<Projects> findById(long id) {
        return repository.findById(id);
    }

    public List<ProjectAttachments> getProjectAttachments(Projects projects) {
        log.info("Executing getProjectAttachments() in projectsService");
        return projects.getListOfAttachments();
    }

    public List<Projects> getRootProjects() {
        log.info("Executing getRootProjects() in projectsService");
        return projectsList
                .stream()
                .filter(projects -> projects.getParent_id() == null)
                .collect(Collectors.toList());
    }

    public List<Projects> getChildProjects(Projects parent) {
        log.info("Executing getChildProjects() in projectsService");
        return projectsList
                .stream()
                .filter(projects -> Objects.equals(projects.getParent_id(), parent.getId()))
                .collect(Collectors.toList());
    }

}
