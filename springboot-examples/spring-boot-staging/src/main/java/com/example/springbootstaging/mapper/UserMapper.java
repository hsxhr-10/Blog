package com.example.springbootstaging.mapper;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.springbootstaging.model.User;

@Repository
public interface UserMapper {
    List<User> getALL();

    User getOne(Long id);

    void insert(User user);

    void update(User user);

    void delete(Long id);
}
