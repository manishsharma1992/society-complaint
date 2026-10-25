package com.mycity.runwal.complaint.application.service;

import static com.mycity.runwal.complaint.application.service.TestActors.SOCIETY;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycity.runwal.complaint.application.query.CategoryView;
import com.mycity.runwal.complaint.application.query.ListCategoriesQuery;
import com.mycity.runwal.complaint.domain.model.actor.Actor;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategory;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryId;
import com.mycity.runwal.shared.id.UserId;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ComplaintCategoryServiceTest {

    private final TestActors.Switchable caller = new TestActors.Switchable(TestActors.resident(10));
    private final ComplaintCategoryService service = new ComplaintCategoryService(RaiseComplaintServiceTest.categories(
            new ComplaintCategory(new ComplaintCategoryId(1), SOCIETY, "Plumbing", Duration.ofHours(24), true),
            new ComplaintCategory(new ComplaintCategoryId(3), SOCIETY, "Lift", Duration.ofHours(4), true),
            new ComplaintCategory(new ComplaintCategoryId(10), SOCIETY, "Old", Duration.ofHours(4), false)), caller);

    @Test
    void membersSeeActiveCategoriesWithTheirResponseTime() {
        assertThat(service.active(new ListCategoriesQuery(1)))
                .containsExactly(new CategoryView(1, "Plumbing", 24), new CategoryView(3, "Lift", 4));
    }

    @Test
    void outsidersSeeNone() {
        caller.become(new Actor(new UserId(100), false, Set.of()));
        assertThat(service.active(new ListCategoriesQuery(1))).isEmpty();
    }
}
