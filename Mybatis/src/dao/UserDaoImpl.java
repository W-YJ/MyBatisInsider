package dao;

import dao.UserDao;
import domain.User;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;

public class UserDaoImpl implements UserDao {
    
    private SqlSessionFactory sqlSessionFactory;
    
    //将SqlSessionFactory注入
    public UserDaoImpl(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public User findUserById(int id) throws Exception {
        //首先创建SqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //根据id查询用户信息
        User user = sqlSession.selectOne("test.findUserById",id);
        sqlSession.close();
        return user;
    }

    @Override
    public List<User> findUserById(String username) throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        List<User> users = sqlSession.selectList("test.findUserById",username);
        sqlSession.close();
        return users;
    }
}
