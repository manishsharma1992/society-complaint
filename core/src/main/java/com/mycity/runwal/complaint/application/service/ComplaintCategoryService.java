package com.mycity.runwal.complaint.application.service;

import com.mycity.runwal.complaint.application.port.CurrentActor;
import com.mycity.runwal.complaint.application.query.CategoryView;
import com.mycity.runwal.complaint.application.query.ListCategoriesQuery;
import com.mycity.runwal.complaint.domain.model.category.ComplaintCategoryRepository;
import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.id.SocietyId;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/** The categories offered when raising a complaint; only members of the society see them. */
@ApplicationService
public class ComplaintCategoryService {

    private final ComplaintCategoryRepository categories;
    private final CurrentActor currentActor;

    public ComplaintCategoryService(ComplaintCategoryRepository categories, CurrentActor currentActor) {
        this.categories = categories;
        this.currentActor = currentActor;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> active(ListCategoriesQuery query) {
        SocietyId societyId = new SocietyId(query.societyId());
        if (!currentActor.actor().belongsTo(societyId)) {
            return List.of();
        }
        return categories.findActiveBySociety(societyId).stream().map(CategoryView::of).toList();
    }
}
