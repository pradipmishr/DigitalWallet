package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    public UserServiceImpl(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        User user = UserMapper.toUserEntity(userDto);
        userRepository.save(user);
        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return UserMapper.toUserDto(users);
    }
}
