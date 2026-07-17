package com.borrowly.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

// Our ids are assigned in Java at construction, so Spring Data's default "is the id null?"
// test reports every entity as already existing. That makes save() merge instead of persist:
// a wasted SELECT before every insert, and a managed copy returned while the object you
// passed in stays detached. Tracking newness explicitly restores persist().
@MappedSuperclass
public abstract class BaseEntity implements Persistable<UUID> {

    @Transient
    private boolean persisted;

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        this.persisted = true;
    }
}