package com.mycity.runwal.complaint.interfaces.rest;

import com.mycity.runwal.complaint.application.query.CategoryView;
import com.mycity.runwal.complaint.application.query.ListCategoriesQuery;
import com.mycity.runwal.complaint.application.service.ComplaintCategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The categories residents choose from when raising a complaint. */
@RestController
class CategoryController {

    private final ComplaintCategoryService categoryService;

    CategoryController(ComplaintCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/societies/{societyId}/categories")
    List<CategoryResponse> categories(@PathVariable long societyId) {
        return categoryService.active(new ListCategoriesQuery(societyId)).stream().map(CategoryResponse::from).toList();
    }

    record CategoryResponse(long id, String name, long slaHours) {

        static CategoryResponse from(CategoryView view) {
            return new CategoryResponse(view.id(), view.name(), view.slaHours());
        }
    }
}
