package com.example.springbootstaging.entity;

import java.util.List;

import lombok.Data;
import org.springframework.stereotype.Component;

import com.example.springbootstaging.model.User;

@Data
@Component
public class UserResponse {
    private int code;
    private String message;
    private List<User> data;
}
