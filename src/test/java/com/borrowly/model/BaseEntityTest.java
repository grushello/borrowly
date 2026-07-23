package com.borrowly.model;

import com.borrowly.model.user.User;
import com.borrowly.repository.user.UserRepository;
import com.borrowly.support.AbstractPostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

// Guards the Persistable contract. Without it, Spring Data decides newness by "is the id
// null?", which is always false for our assigned ids: save() would merge, hand back a copy,
// and leave the object you passed in detached.
@DataJpaTest
class BaseEntityTest extends AbstractPostgresTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("save() persists rather than merges, so it returns the instance it was given")
    void saveReturnsTheSameInstance() {
        User user = User.register("Ada", "Lovelace", "ada@example.com", "hash");
        assertThat(user.isNew()).isTrue();

        User saved = userRepository.save(user);

        assertThat(saved).isSameAs(user);
        assertThat(entityManager.contains(user))
                .as("the instance passed to save() must itself be managed, not a detached copy")
                .isTrue();

        // @PostPersist runs after the INSERT, so the flag only flips once we flush.
        entityManager.flush();
        assertThat(saved.isNew()).isFalse();
    }

    @Test
    @DisplayName("a loaded entity is not new, so saving it updates instead of inserting")
    void loadedEntityIsNotNew() {
        long usersBefore = userRepository.count();

        User user = userRepository.saveAndFlush(
                User.register("Grace", "Hopper", "grace@example.com", "hash"));
        entityManager.clear();

        User loaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(loaded.isNew())
                .as("@PostLoad must clear the flag, or save() would try to insert a duplicate")
                .isFalse();

        loaded.setPhone("+370 600 00000");
        userRepository.save(loaded);
        entityManager.flush();

        assertThat(userRepository.count())
                .as("the second save must update the existing row, not insert a duplicate")
                .isEqualTo(usersBefore + 1);
    }
}
