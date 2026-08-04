package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.RegisterRequest;
import com.project.digitalwallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;



    @GetMapping()
    public ResponseWrapper<?> getAllUsers(){
        return new ResponseWrapper<>(userService.getAllUsers(),"All Users",HttpStatus.OK.value(),true);
    }
}
