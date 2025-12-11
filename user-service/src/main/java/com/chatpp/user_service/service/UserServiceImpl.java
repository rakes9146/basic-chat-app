package com.chatpp.user_service.service;

import com.chatpp.user_service.entity.User;
import com.chatpp.user_service.repository.UserRepository;
import com.chatpp.user_service.web.data.UserDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements  UserService{

    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDto getUserByUserName(String userName) {
        Optional<User> user = userRepository.findByUserName(userName);
        UserDto userDto = null;
        if(user.isPresent()){
            userDto =  new UserDto();
            BeanUtils.copyProperties(user.get(), userDto);
            userDto.setUserId(user.get().getId());  // Manually map id to userId
            log.info("Retrieved user: {} with ID: {}", userName, userDto.getUserId());
        }
        return userDto;
    }

    @Override
    public List<UserDto> getUsers() {
        List<User> dbUserList = userRepository.findAll();
        List<UserDto> dtoUserList = new ArrayList<>();

        dtoUserList = dbUserList.stream().map(us ->{
                   UserDto userDto = new UserDto();
                    BeanUtils.copyProperties(us, userDto);
                    userDto.setUserId(us.getId());  // Manually map id to userId
                    log.info("Mapped user: {} {} with ID: {}", us.getFirstName(), us.getLastName(), userDto.getUserId());
                    return userDto;
                }).collect(Collectors.toUnmodifiableList());

        log.info("Retrieved {} users from database", dtoUserList.size());
        return dtoUserList;
    }

    @Override
    public UserDto saveUser(UserDto userDto) {

        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        userRepository.save(user);
        return userDto;
    }

    @Override
    public Boolean validateUser(String userName, String password) {

        log.info("Validating user: {}", userName);
        UserDto userDto = getUserByUserName(userName);
        if(userDto != null){
            log.info("User found: {}", userName);
            String dbPassword = userDto.getPassword();
            if(dbPassword.equals(password)){
                log.info("Password matches for user: {}", userName);
                return true;
            } else {
                log.warn("Password mismatch for user: {}", userName);
            }
        } else {
            log.warn("User not found: {}", userName);
        }
        return false;
    }
}
