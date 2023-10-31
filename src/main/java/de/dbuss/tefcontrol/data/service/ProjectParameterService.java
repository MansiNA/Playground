package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.ProjectSql;
import de.dbuss.tefcontrol.data.repository.CLTV_HW_MeasuresRepository;
import de.dbuss.tefcontrol.data.repository.ProjectParameterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectParameterService {

    private final ProjectParameterRepository projectParameterRepository;

    public ProjectParameterService(ProjectParameterRepository projectParameterRepository) {
        this.projectParameterRepository = projectParameterRepository;
    }

    public List<ProjectParameter> findAll() {
        return projectParameterRepository.findAll();
    }

    public List<ProjectParameter> findByNamespace(String namespace) {
        return projectParameterRepository.findByNamespace(namespace);
    }

    public ProjectParameter update(ProjectParameter entity) { return projectParameterRepository.save(entity);}
}
