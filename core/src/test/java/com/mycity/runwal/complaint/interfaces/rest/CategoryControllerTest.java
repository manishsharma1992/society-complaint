package com.mycity.runwal.complaint.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.complaint.application.query.CategoryView;
import com.mycity.runwal.complaint.application.query.ListCategoriesQuery;
import com.mycity.runwal.complaint.application.service.ComplaintCategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintCategoryService categoryService;

    @Test
    void listsCategories() throws Exception {
        given(categoryService.active(new ListCategoriesQuery(1))).willReturn(List.of(new CategoryView(3, "Lift", 4)));

        mockMvc.perform(get("/complaint/societies/1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lift"))
                .andExpect(jsonPath("$[0].slaHours").value(4));
    }
}
