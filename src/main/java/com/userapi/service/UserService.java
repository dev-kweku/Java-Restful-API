package com.userapi.service;

import com.userapi.model.User;
import com.userapi.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo){
        this.repo=repo;
    }

    public record PageResult(List<User> users, long total, int page, int size){}
    public PageResult getAll(int page,int size,String country)throws SQLException{
        if (size > 100) size=100;
        if(page < 0) page=0;
        long total=repo.count(country);
        List<User> users=repo.findAll(page,size,country);

        return new PageResult(users,total,page,size);
    }

    public Optional<User> getById(long id) throws SQLException{
        return repo.findById(id);
    }

    public User create(User user) throws SQLException{
        validate(user);
        return repo.create(user);
    }

    public Optional<User> update(long id,User user) throws SQLException{
        validate(user);
        return repo.update(id,user);
    }

    public boolean delete(long id) throws SQLException{
        return repo.delete(id);
    }

    private void validate(User u){
        if(u.getFirstName() == null || u.getFirstName().isBlank())
            throw new IllegalArgumentException("first_name is required");
        if(u.getEmail()==null || u.getEmail().contains("@"))
            throw new IllegalArgumentException("valid email is required");
        if(u.getAge()<0 || u.getAge()>150)
            throw new IllegalArgumentException("age must be between 0 - 150");
    }
}
