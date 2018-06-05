package test;

import domain.*;
import dao.UserDao;
import dao.UserDaoImpl;
import mapper.OrdersMapperCustom;
import mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MapperTest {
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
    public void findById() throws Exception {
        UserDao userDao = new UserDaoImpl(sqlSessionFactory);
        User user = userDao.findUserById(1);
        System.out.println(user);
    }

    @Test
    public void findUserById() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        User user = userMapper.findUserById(2);
        sqlSession.close();
        System.out.println(user);
    }

    @Test
    public void findUserByName() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        List<User> users = userMapper.findUserByName("wyj");
        sqlSession.close();
        System.out.println(users);
    }

    @Test
    public void insertUser() throws Exception {
        User user = new User();
        user.setUsername("coding");
        user.setSex("male");

        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        userMapper.insertUser(user);
        sqlSession.commit();
        sqlSession.close();
    }


    @Test
    public void findUserByListResultMap() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

        UserQueryVo userQueryVo = new UserQueryVo();
        UserCustom userCustom = new UserCustom();
        userCustom.setUsername("wyj");
        userQueryVo.setUserCustom(userCustom);
        List<User> users = userMapper.findUserListResultMap(userQueryVo);

        sqlSession.close();
        System.out.println(users);
    }

    @Test
    public void findUserByList() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();

        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

        UserQueryVo userQueryVo = new UserQueryVo();

        //定义一个id集合
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(2);
        userQueryVo.setIds(ids);

        UserCustom userCustom = new UserCustom();
        userCustom.setUsername("wyj");
        userCustom.setSex("1");
        userQueryVo.setUserCustom(userCustom);

        List<User> users = userMapper.findUserList(userQueryVo);

        sqlSession.close();

        System.out.println(users);

    }

    @Test
    public void findUserCount() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();

        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

        UserQueryVo userQueryVo = new UserQueryVo();

        //定义一个id集合
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(2);
        userQueryVo.setIds(ids);

        UserCustom userCustom = new UserCustom();
        userCustom.setUsername("wyj");
        userQueryVo.setUserCustom(userCustom);
        int count = userMapper.findUserCount(userQueryVo);
        sqlSession.close();
        System.out.println(count);
    }

    @Test
    public void testFindOrderUserList() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        OrdersMapperCustom ordersMapperCustom = sqlSession.getMapper(OrdersMapperCustom.class);

        List<OrderCustom> list = ordersMapperCustom.findOrderUserList();
        sqlSession.close();
        System.out.println(list);
    }

    @Test
    public void testFindOrderUserListResultMap() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        OrdersMapperCustom ordersMapperCustom = sqlSession.getMapper(OrdersMapperCustom.class);
        List<Orders> list = ordersMapperCustom.findOrderUserListResultMap();
        sqlSession.close();
        System.out.println(list);
    }

    @Test
    public void testFindOrderAndOrderDetails() throws Exception {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        OrdersMapperCustom ordersMapperCustom = sqlSession.getMapper(OrdersMapperCustom.class);
        List<Orders> list = ordersMapperCustom.findOrderAndOrderDetails();
        sqlSession.close();
        System.out.println(list);
    }

    @Test
    public void testFindUserListLazyLoading() throws Exception{
        SqlSession sqlSession = sqlSessionFactory.openSession();
        OrdersMapperCustom ordersMapperCustom = sqlSession.getMapper(OrdersMapperCustom.class);
        List<Orders> list=ordersMapperCustom.findOrderUserListLazyLoading();

        //这里执行延迟加载
        User user = list.get(0).getUser();
        System.out.println(user);
   }
}
