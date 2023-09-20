package de.dbuss.tefcontrol.data.service;


import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.repository.CLTV_HW_MeasuresRepository;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Service
public class CLTV_HW_MeasureService {
    private final CLTV_HW_MeasuresRepository repository;

    public CLTV_HW_MeasureService(CLTV_HW_MeasuresRepository repository) {
        this.repository = repository;
    }

    public Optional<CLTV_HW_Measures> get(Long id) {
        return repository.findById(id);
    }

    public CLTV_HW_Measures update(CLTV_HW_Measures entity) { return repository.save(entity);}
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<CLTV_HW_Measures> findAllProducts(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {
            return repository.findAll();
        } else {
            return repository.search(stringFilter);
        }
    }
}
