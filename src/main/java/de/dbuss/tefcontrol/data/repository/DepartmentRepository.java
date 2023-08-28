package de.dbuss.tefcontrol.data.repository;

import de.dbuss.tefcontrol.data.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

}
