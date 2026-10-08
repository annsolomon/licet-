package com.college.service;

import com.college.model.AuditEntry;
import com.college.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Audit rows are written by Oracle TRIGGERS; Java only reads them. */
@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public List<AuditEntry> recent(Integer limit) {
        int n = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return repository.findRecent(n);
    }

    public long count() {
        return repository.count();
    }
}
