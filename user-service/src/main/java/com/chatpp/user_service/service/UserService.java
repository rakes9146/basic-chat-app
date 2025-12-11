package com.chatpp.user_service.service;

import com.chatpp.user_service.web.data.UserDto;

import java.util.List;

public interface UserService {

        UserDto getUserByUserName(String userId);

        List<UserDto> getUsers();

        UserDto saveUser(UserDto userDto);

        Boolean validateUser(String userName, String password);
}
