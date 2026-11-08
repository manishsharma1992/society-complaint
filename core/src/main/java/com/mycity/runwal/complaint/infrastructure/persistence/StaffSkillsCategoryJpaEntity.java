package com.mycity.runwal.complaint.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/** One category a staff member handles; a staff member's skills are all their rows. */
@Entity
@Table(schema = "complaint", name = "staff_skills_category")
class StaffSkillsCategoryJpaEntity {

    @EmbeddedId
    Key id;

    protected StaffSkillsCategoryJpaEntity() {
    }

    StaffSkillsCategoryJpaEntity(long societyId, long staffId, long categoryId) {
        this.id = new Key(societyId, staffId, categoryId);
    }

    @Embeddable
    static class Key implements Serializable {

        @Column(name = "society_id")
        long societyId;

        @Column(name = "staff_id")
        long staffId;

        @Column(name = "category_id")
        long categoryId;

        protected Key() {
        }

        Key(long societyId, long staffId, long categoryId) {
            this.societyId = societyId;
            this.staffId = staffId;
            this.categoryId = categoryId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.societyId == societyId && k.staffId == staffId && k.categoryId == categoryId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(societyId, staffId, categoryId);
        }
    }
}
