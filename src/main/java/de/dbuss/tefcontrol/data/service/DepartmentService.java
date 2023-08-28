package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.Department;
import de.dbuss.tefcontrol.data.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    public Optional<Department> get(Long id) {
        return repository.findById(id);
    }

    public Department update(Department entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Department> findAll() {
        return repository.findAll();
    }

    public Optional<Department> findById(long id) {
        return repository.findById(id);
    }

    public List<Department> getRootDepartments() {
        List<Department> departmentList = repository.findAll();
        return departmentList
                .stream()
                .filter(department -> department.getParent_id() == null)
                .collect(Collectors.toList());
    }

    public List<Department> getChildDepartments(Department parent) {
        List<Department> departmentList = repository.findAll();
        return departmentList
                .stream()
                .filter(department -> Objects.equals(department.getParent_id(), parent.getId()))
                .collect(Collectors.toList());
    }
}
