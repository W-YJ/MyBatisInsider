package dao;

import domain.User;

import java.util.List;

//原始的dao开发方式
public interface UserDao {
    //根据id查询用户信息
    public User findUserById(int id) throws Exception;

    //根据name查询用户信息
    public List<User> findUserById(String username) throws Exception;

}
