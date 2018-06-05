package dao;

import org.mybatis.spring.support.SqlSessionDaoSupport;
import po.User;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

public class UserDaoImpl extends SqlSessionDaoSupport implements UserDao {

    @Override
    public User findUserById(int id) throws Exception {
        //首先创建SqlSession
        SqlSession sqlSession = this.getSqlSession();

        //根据id查询用户信息
        User user = sqlSession.selectOne("test.findUserById",id);
        return user;
    }

    @Override
    public List<User> findUserById(String username) throws Exception {
        SqlSession sqlSession = this.getSqlSession();
        List<User> users = sqlSession.selectList("test.findUserById",username);
        sqlSession.close();
        return users;
    }
}
