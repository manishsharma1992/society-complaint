package com.mycity.runwal.identity.interfaces.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycity.runwal.identity.application.query.UserNameView;
import com.mycity.runwal.identity.application.query.UserNamesQuery;
import com.mycity.runwal.identity.application.query.UsersWithRoleQuery;
import com.mycity.runwal.identity.application.service.UserDirectoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserDirectoryController.class)
class UserDirectoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDirectoryService directory;

    @Test
    void namesForIds() throws Exception {
        given(directory.names(new UserNamesQuery(1, List.of(4L, 3L))))
                .willReturn(List.of(new UserNameView(4, "Meera"), new UserNameView(3, "Suresh")));

        mockMvc.perform(get("/identity/societies/1/users").param("ids", "4,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Meera"))
                .andExpect(jsonPath("$[0].phone").doesNotExist());
    }

    @Test
    void staffList() throws Exception {
        given(directory.withRole(new UsersWithRoleQuery(1, "STAFF"))).willReturn(List.of(new UserNameView(3, "Suresh")));

        mockMvc.perform(get("/identity/societies/1/users").param("role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }

    @Test
    void needsExactlyOneOfIdsOrRole() throws Exception {
        mockMvc.perform(get("/identity/societies/1/users")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/identity/societies/1/users").param("ids", "1").param("role", "STAFF"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(directory);
    }
}
