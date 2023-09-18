package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.ProjectAttachments;
import de.dbuss.tefcontrol.data.entity.Projects;
import de.dbuss.tefcontrol.data.repository.ProjectsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProjectsService {

    private final ProjectsRepository repository;

    public ProjectsService(ProjectsRepository repository) {
        this.repository = repository;
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

    public Projects search(String jobName) {
        return repository.search(jobName);
    }


    public Optional<Projects> findById(long id) {
        return repository.findById(id);
    }

    public List<ProjectAttachments> getProjectAttachments(Projects projects) {
        return projects.getProjectAttachments();
    }

        public List<Projects> getRootProjects() {
        List<Projects> projectsList = repository.findAll();
        return projectsList
                .stream()
                .filter(projects -> projects.getParent_id() == null)
                .collect(Collectors.toList());
    }

        public List<Projects> getChildProjects(Projects parent) {
        List<Projects> projectsList = repository.findAll();
        return projectsList
                .stream()
                .filter(projects -> Objects.equals(projects.getParent_id(), parent.getId()))
                .collect(Collectors.toList());
    }

}
