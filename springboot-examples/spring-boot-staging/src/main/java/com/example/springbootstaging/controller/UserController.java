package com.example.springbootstaging.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.springbootstaging.enums.UserEnum;
import com.example.springbootstaging.entity.UserResponse;
import com.example.springbootstaging.model.User;
import com.example.springbootstaging.mapper.UserMapper;

@RestController
@RequestMapping("/api/staging/user")
public class UserController {
    @Autowired
    private UserResponse UserResponse;

    @Autowired
    private UserMapper userMapper;

    @GetMapping(value = "/getUser/{id}")
    public UserResponse getUser(@PathVariable(value = "id", required = true) Long id) {
        User user = userMapper.getOne(id);

        UserResponse.setCode(0);
        UserResponse.setMessage("success");
        List<User> data = new ArrayList<>(1);
        data.add(user);
        UserResponse.setData(data);
        return UserResponse;
    }

    @GetMapping(value = "/getUsers")
    public UserResponse getUser() {
        List<User> users = userMapper.getALL();

        UserResponse.setCode(0);
        UserResponse.setMessage("success");
        UserResponse.setData(users);
        return UserResponse;
    }

    @PostMapping("/insertUser")
    public UserResponse insertUser(@RequestParam(value = "userName", required = true) String userName,
                                   @RequestParam(value = "age", required = true) Integer age,
                                   @RequestParam(value = "sex", required = true, defaultValue = "female") String sex) {
        User user = new User(userName, age, sex);
        userMapper.insert(user);

        UserResponse.setCode(0);
        UserResponse.setMessage("success");
        List<User> data = new ArrayList<>(0);
        UserResponse.setData(data);
        return UserResponse;
    }

    @PostMapping("/updateUser")
    public UserResponse updateUser(@RequestParam(value = "id", required = true) Long id,
                                   @RequestParam(value = "userName", required = true) String userName,
                                   @RequestParam(value = "age", required = true) Integer age,
                                   @RequestParam(value = "sex", required = true, defaultValue = "female") String sex) {
        User user = new User(id, userName, age, sex);
        userMapper.update(user);

        UserResponse.setCode(0);
        UserResponse.setMessage("success");
        List<User> data = new ArrayList<>(0);
        UserResponse.setData(data);
        return UserResponse;
    }

    @GetMapping("/deleteUser/{id}")
    public UserResponse deleteUser(@PathVariable(value = "id", required = true) Long id) {
        userMapper.delete(id);

        UserResponse.setCode(0);
        UserResponse.setMessage("success");
        List<User> data = new ArrayList<>(0);
        UserResponse.setData(data);
        return UserResponse;
    }
}
