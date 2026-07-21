package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.request.CreateItemRequest;
import com.borrowly.dto.request.UpdateItemRequest;
import com.borrowly.dto.response.ItemResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.UserDetailsServiceImpl;
import com.borrowly.service.auth.AuthService;
import com.borrowly.service.item.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ItemController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser
    void createItemSuccess() throws Exception {


        CreateItemRequest request =
                new CreateItemRequest(
                        "Drill",
                        "Electric drill",
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        ItemCondition.GOOD,
                        UUID.randomUUID()
                );


        when(itemService.create(any()))
                .thenReturn(mock(ItemResponse.class));


        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());


        verify(itemService)
                .create(any(CreateItemRequest.class));
    }



    @Test
    void createWithoutAuthReturns401() throws Exception {


        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }



    @Test
    @WithMockUser
    void createBlankTitleReturns400() throws Exception {


        CreateItemRequest request =
                new CreateItemRequest(
                        "",
                        "Description",
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        ItemCondition.GOOD,
                        UUID.randomUUID()
                );


        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());


        verify(itemService, never())
                .create(any());
    }



    @Test
    void getItemIsPublic() throws Exception {


        UUID id = UUID.randomUUID();


        when(itemService.getById(id))
                .thenReturn(mock(ItemResponse.class));


        mockMvc.perform(get("/api/items/" + id))
                .andExpect(status().isOk());


        verify(itemService)
                .getById(id);
    }



    @Test
    @WithMockUser
    void updateItemSuccess() throws Exception {


        UUID id = UUID.randomUUID();


        UpdateItemRequest request =
                new UpdateItemRequest(
                        "Updated",
                        "desc",
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        ItemCondition.NEW
                );


        when(itemService.update(eq(id), any()))
                .thenReturn(mock(ItemResponse.class));


        mockMvc.perform(patch("/api/items/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());


        verify(itemService)
                .update(eq(id), any(UpdateItemRequest.class));
    }



    @Test
    void updateWithoutAuthReturns401() throws Exception {


        mockMvc.perform(patch("/api/items/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }



    @Test
    @WithMockUser
    void archiveItemSuccess() throws Exception {


        UUID id = UUID.randomUUID();


        when(itemService.archive(id))
                .thenReturn(mock(ItemResponse.class));


        mockMvc.perform(delete("/api/items/" + id))
                .andExpect(status().isOk());


        verify(itemService)
                .archive(id);
    }



    @Test
    void archiveWithoutAuthReturns401() throws Exception {


        mockMvc.perform(delete("/api/items/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void unarchiveItemSuccess() throws Exception {

        UUID id = UUID.randomUUID();

        when(itemService.unarchive(id))
                .thenReturn(mock(ItemResponse.class));

        mockMvc.perform(patch("/api/items/" + id + "/unarchive"))
                .andExpect(status().isOk());

        verify(itemService)
                .unarchive(id);
    }


    @Test
    void unarchiveWithoutAuthReturns401() throws Exception {

        mockMvc.perform(patch("/api/items/" + UUID.randomUUID() + "/unarchive"))
                .andExpect(status().isUnauthorized());
    }
}