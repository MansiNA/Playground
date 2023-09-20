package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.KnowledgeBase;
import de.dbuss.tefcontrol.data.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository repository;

    public KnowledgeBaseService(KnowledgeBaseRepository repository) {
        this.repository = repository;
    }

    public Optional<KnowledgeBase> get(Long id) {
        return repository.findById(id);
    }

    public KnowledgeBase update(KnowledgeBase entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<KnowledgeBase> findById(long id) {
        return repository.findById(id);
    }
}
