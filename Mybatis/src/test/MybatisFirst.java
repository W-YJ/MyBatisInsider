package test;


import domain.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;


import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class MybatisFirst {
    private SqlSessionFactory sqlSessionFactory;

    //创建工厂
    @Before
    public void init() throws IOException {
        //配置文件
        String resource = "sqlMapConfig.xml";

        //加载配置文件到输入流中
        InputStream inputStream = Resources.getResourceAsStream(resource);
        //创建会话工厂
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }

    //根据id查找用户
    @Test
    public void testFindUserById() {
        //通过sqlSessionFactory创建sqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();

        User user = null;

        //通过sqlSession操作数据库
        //第一个参数：statement的位置，等于namespace+statement的id
        //第二个参数；传给占位符的参数
        try {
            user = sqlSession.selectOne("test.findUserById", 1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭sqlSession
            sqlSession.close();
        }
        System.out.print(user);
    }

    //测试根据name查询用户（得到多条记录）
    @Test
    public void testFindUserByName() {
        // 通过sqlSessionFactory创建sqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();

        // 通过sqlSession操作数据库
        // 第一个参数：statement的位置，等于namespace+statement的ID
        // 第二个参数：传入的参数
        List<User> list = null;
        try {
            list = sqlSession.selectList("test.findUserByName", "ls");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭sqlSession
            sqlSession.close();
        }
        System.out.println(list.get(1).getId());
    }

    @Test
    public void testInsertUser() {
        //通过sqlSessionFactory创建sqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();

        User user = new User();
        user.setUsername("wyj");
        user.setAddress("jiangsu");
        user.setBirthday(new Date());
        user.setSex("1");
        //通过sqlSession操作数据库
        //第一个参数：statement的位置，等于namespace+statement的id
        //第二个参数：传给占位符的参数
        try {
            sqlSession.insert("test.insertUser", user);

            //需要提交事物
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭sqlSession
            sqlSession.close();
        }
    }

    @Test
    public void testDeleteUser(){
        //通过sqlSessionFactory创建sqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();

        try {
            sqlSession.delete("test.deleteUser",3);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //关闭sqlSession
            sqlSession.close();
        }
    }

    @Test
    public void testUpdateUser(){
        //通过sqlSessionFactory创建sqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();

        User user = new User();
        user.setSex("1");
        user.setUsername("wyj");
        user.setBirthday(new Date());
        user.setAddress("nanjin");
        user.setId(4);
        try{
            sqlSession.update("test.updateUser",user);
            //需要提交事物
            sqlSession.commit();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //关闭sqlSession
            sqlSession.close();
        }
        System.out.println("用户ID="+user.getId());
    }
}