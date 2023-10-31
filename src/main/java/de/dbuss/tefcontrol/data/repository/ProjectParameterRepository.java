package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectParameterRepository extends JpaRepository<ProjectParameter, Integer> {
    List<ProjectParameter> findByNamespace(String namespace);
}
