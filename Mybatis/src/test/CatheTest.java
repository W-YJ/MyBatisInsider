package test;

import domain.User;
import mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class CatheTest {
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void init() throws IOException {
        //配置文件
        String resource = "SqlMapConfig.xml";

        //加载配置文件到输入流中
        InputStream inputStream = Resources.getResourceAsStream(resource);
        //创建会话工厂
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Test
    public void testCache1() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

        //第一次查询用户id为1的用户
        User user1 = userMapper.findUserById(1);
        System.out.println(user1);

        //加入更新数据的操作
        user1.setUsername("sss");
        userMapper.updateUser(user1);
        sqlSession.close();


        //第二次查询用户id为1的用户
        User user2 = userMapper.findUserById(1);
        System.out.println(user2);

        sqlSession.close();
    }

    /*二级缓存的刷新操作有两种：
    * 1.如果sqlSession操作commit操作，对二级缓存进行刷新（全局清空）
    * 2.设置statement的flushCache是否刷新缓存，默认值是true*/
    @Test
    public void testCache2() throws Exception {
        SqlSession sqlSession1 = sqlSessionFactory.openSession();
        SqlSession sqlSession2 = sqlSessionFactory.openSession();
        SqlSession sqlSession3 = sqlSessionFactory.openSession();
        UserMapper userMapper1 = sqlSession1.getMapper(UserMapper.class);
        UserMapper userMapper2 = sqlSession1.getMapper(UserMapper.class);
        UserMapper userMapper3 = sqlSession1.getMapper(UserMapper.class);

        //第一查询用户id为1的用户
        User user = userMapper1.findUserById(1);
        System.out.println(user);
        sqlSession1.close();

        //中间修改用户要清空缓存，目的防止查询出脏数据
        user.setUsername("测试用户2");
        userMapper3.updateUser(user);
        sqlSession3.commit();
        sqlSession3.close();

        //第二次查询用户id为1的用户
        User user2 = userMapper2.findUserById(1);
        System.out.println(user2);
        sqlSession2.close();
    }

}
