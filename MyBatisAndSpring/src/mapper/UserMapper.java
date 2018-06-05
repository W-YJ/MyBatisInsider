package mapper;

import po.UserQueryVo;
import po.User;

import java.util.List;

public interface UserMapper {
    public User findUserById(int id) throws Exception;

    //根据用户姓名
    public List<User> findUserByName(String username) throws Exception;

    public void insertUser(User user);

    public void updateUser(User user);

    //自定义查询条件来查询用户的信息
    public List<User> findUserList(UserQueryVo userQueryVo) throws Exception;

    //自定义查询条件来查询用户的信息
    public int findUserCount(UserQueryVo userQueryVo) throws Exception;

    //查询用户使用resultMap进行映射
    public List<User> findUserListResultMap(UserQueryVo userQueryVo) throws Exception;
}
