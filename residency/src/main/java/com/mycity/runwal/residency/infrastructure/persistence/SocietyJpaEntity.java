package com.mycity.runwal.residency.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(schema = "residency", name = "society")
class SocietyJpaEntity {

    @Id
    Long id;

    @Column(nullable = false, length = 150)
    String name;

    Double latitude;

    Double longitude;

    /** One row per add-on available to the society. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "residency", name = "society_add_on", joinColumns = @JoinColumn(name = "society_id"))
    Set<AddOnRow> addOns = new HashSet<>();

    /** Towers where complaints are off while they are on for the rest of the society. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "residency", name = "society_complaints_off_tower", joinColumns = @JoinColumn(name = "society_id"))
    @Column(name = "tower_id", nullable = false)
    Set<Long> complaintsOffTowers = new HashSet<>();

    protected SocietyJpaEntity() {
    }

    @Embeddable
    static class AddOnRow {

        @Column(name = "add_on", nullable = false, length = 20)
        String addOn;

        @Column(nullable = false)
        boolean enabled;

        protected AddOnRow() {
        }

        AddOnRow(String addOn, boolean enabled) {
            this.addOn = addOn;
            this.enabled = enabled;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof AddOnRow other && addOn.equals(other.addOn) && enabled == other.enabled;
        }

        @Override
        public int hashCode() {
            return Objects.hash(addOn, enabled);
        }
    }
}
