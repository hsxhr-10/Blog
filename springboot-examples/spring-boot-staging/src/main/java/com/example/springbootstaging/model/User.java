package com.example.springbootstaging.model;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String userName;
    private Integer age;
    private String sex;

    public User(String userName, Integer age, String sex) {
        this.userName = userName;
        this.age = age;
        this.sex = sex;
    }

    public User(Long id, String userName, Integer age, String sex) {
        this.id = id;
        this.userName = userName;
        this.age = age;
        this.sex = sex;
    }

    @Override
    public String toString() {
        return String.format("[id: %s, userName: %s, age: %s, sex: %s]", id, userName, age, sex);
    }
}
