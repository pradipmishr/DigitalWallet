package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }

    @PostMapping("/add")
    public ResponseWrapper<?> createUser(@RequestBody UserDto userDto){
        return new ResponseWrapper<>(userService.createUser(userDto),"User created", HttpStatus.CREATED.value(), true);
    }

    @GetMapping()
    public ResponseWrapper<?> getAllUsers(){
        return new ResponseWrapper<>(userService.getAllUsers(),"All Users",HttpStatus.OK.value(),true);
    }
}
