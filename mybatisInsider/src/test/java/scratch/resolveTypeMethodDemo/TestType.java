package scratch.resolveTypeMethodDemo;

import org.apache.ibatis.reflection.TypeParameterResolver;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TestType {
    SubA<Long> sa = new SubA<>();

    public static void main(String[] args) throws NoSuchFieldException {
        Field f = A.class.getDeclaredField("map");
        System.out.println(f.getGenericType());//java.util.Map<K, V>
        System.out.println(f.getGenericType() instanceof ParameterizedType);//true

        //解析SubA<Long>(ParameterizedType类型中的map字段)
        //ParameterizedTypeImpl是ParameterizedType的接口实现
        Type type = TypeParameterResolver.resolveFieldType(f, ParameterizedTypeImpl.make(SubA.class, new Type[]{Long.class}, TestType.class));
        //也可使用以下方法生成上述ParameterizedType对象，并调用相关方法解析
        //TypeParameterResolver.resolveFieldType(f, TestType.class.getDeclaredField("sa").getGenericType());
        System.out.println(type.getClass());//class org.apache.ibatis.reflection.TypeParameterResolver$ParameterizedTypeImpl

        ParameterizedType p = (ParameterizedType) type;
        System.out.println(p.getRawType());//interface java.util.Map
        System.out.println(p.getOwnerType());//null
        for (Type t : p.getActualTypeArguments()) {
            System.out.println(t);
        }
        //class java.lang.Long
        //class java.lang.Long

    }
}
