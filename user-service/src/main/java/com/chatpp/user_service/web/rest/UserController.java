package com.chatpp.user_service.web.rest;

import com.chatpp.user_service.service.UserService;
import com.chatpp.user_service.web.data.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserDto userDto){
        try {
            userService.saveUser(userDto);
            // Return JSON response
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new java.util.HashMap<String, Object>() {{
                        put("success", true);
                        put("message", "User registered successfully");
                    }});
        } catch (Exception e) {
            // Handle duplicate user or other errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new java.util.HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Registration failed: " + e.getMessage());
                    }});
        }
    }

    @GetMapping
    public List<UserDto> getUsers(){
        return userService.getUsers();
    }

    @PostMapping("/login")
    public ResponseEntity<Boolean> login(@RequestBody UserDto userDto){
       boolean isValid = userService.validateUser(userDto.getUserName(), userDto.getPassword());
       if(isValid){
           return ResponseEntity.ok(true);
       }else{
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
       }
    }

    @GetMapping("/{userName}")
    public ResponseEntity<UserDto> getUserByUserName(@PathVariable("userName") String userName){
        UserDto user = userService.getUserByUserName(userName);
        if (user != null) {
            return ResponseEntity.ok(user); // 200 OK with user object
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404 Not Found
        }
    }

}
