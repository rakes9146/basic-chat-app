package com.chatpp.user_service.web.rest;

import com.chatpp.user_service.service.UserService;
import com.chatpp.user_service.web.data.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserDto sampleUser;

    @BeforeEach
    void setup(){

        sampleUser = new UserDto();
        sampleUser.setFirstName("Ravi");
        sampleUser.setLastName("Kumar");
        sampleUser.setEmail("ravi@example.com");
        sampleUser.setUserName("ravi123");
        sampleUser.setPassword("pass123");
    }

    @Test
    void testCreateUser()throws Exception{
        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(sampleUser)))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string("User Created"));
    }

    @Test
    void testGetUserByUserName()throws  Exception{
        Mockito.when(userService.getUserByUserName("ravi123")).thenReturn(sampleUser);

        mockMvc.perform(get("/user/ravi123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ravi@example.com"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        Mockito.when(userService.validateUser("ravi123", "pass123")).thenReturn(true);

        mockMvc.perform(get("/user/login")
                        .param("userName", "ravi123")
                        .param("password", "pass123"))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) content().string("true"));
    }

    @Test
    void testGetUsers() throws Exception {
        List<UserDto> users = List.of(sampleUser);
        Mockito.when(userService.getUsers()).thenReturn(users);

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("ravi123"));
    }

}