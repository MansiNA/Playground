package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.ProjectSql;
import de.dbuss.tefcontrol.data.repository.ProjectSqlRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectSqlService {
    private final ProjectSqlRepository repository;

    public ProjectSqlService(ProjectSqlRepository repository) {
        this.repository = repository;
    }

    public List<ProjectSql> findAll() {
        return repository.findAll();
    }
    public Optional<ProjectSql> findByName(String jobName) {
        return repository.findByName(jobName);
    }
    public Optional<ProjectSql> findById(long id) {
        return repository.findById(id);
    }
    public ProjectSql save(ProjectSql projectSql) {
        return repository.save(projectSql);
    }
    public void delete (ProjectSql projectSql) {
        repository.deleteById(projectSql.getId());
    }

}
