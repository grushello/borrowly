package com.borrowly.controller;

import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.service.item.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class UserItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;



    @Test
    @WithMockUser
    void getCurrentUserItemsSuccess() throws Exception {


        Page<ItemSummaryResponse> response =
                new PageImpl<>(List.of());


        when(itemService.getCurrentUserItems(any(Pageable.class)))
                .thenReturn(response);


        mockMvc.perform(get("/api/users/me/items"))
                .andExpect(status().isOk());


        verify(itemService)
                .getCurrentUserItems(any(Pageable.class));
    }



    @Test
    void getCurrentUserItemsWithoutAuthReturns401()
            throws Exception {


        mockMvc.perform(get("/api/users/me/items"))
                .andExpect(status().isUnauthorized());
    }



    @Test
    @WithMockUser
    void paginationWorks() throws Exception {


        when(itemService.getCurrentUserItems(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));


        mockMvc.perform(get("/api/users/me/items")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());


        verify(itemService)
                .getCurrentUserItems(any(Pageable.class));
    }



    @Test
    @WithMockUser
    void sizeOver50Returns400() throws Exception {


        mockMvc.perform(get("/api/users/me/items")
                        .param("size", "100"))
                .andExpect(status().isBadRequest());
    }
}