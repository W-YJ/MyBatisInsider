/**
 * Copyright 2009-2018 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.reflection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * This class represents a cached set of class definition information that
 * allows for easy mapping between property names and getter/setter methods.
 *
 * @author Clinton Begin
 */

//Reflector是Mybatis中反射模块的基础，每个Reflector对象都对应一个类，在Reflector中缓存了反射操作需要使用的类的元信息
public class Reflector {

    private final Class<?> type;//对应的Class类型
    //可读属性的名称集合 - 可读属性就是存在相应的getter方法的属性
    private final String[] readablePropertyNames;
    //可写属性的名称集合 - 可写属性就是存在相应的setter方法的属性
    private final String[] writeablePropertyNames;
    //属性相应的setter方法，key是属性名称，value是Invoker对象（对setter方法对应Method对象的封装）
    private final Map<String, Invoker> setMethods = new HashMap<>();
    //属性相应的getter方法集合
    private final Map<String, Invoker> getMethods = new HashMap<>();
    //属性相应的setter方法的参数值类型，key是属性名称，value是setter方法的参数类型
    private final Map<String, Class<?>> setTypes = new HashMap<>();
    //属性相应的getter方法的返回值类型
    private final Map<String, Class<?>> getTypes = new HashMap<>();
    //记录了默认构造方法
    private Constructor<?> defaultConstructor;
    //记录了所有属性名称的集合
    private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

    public Reflector(Class<?> clazz) {
        type = clazz;
        //查找clazz的默认构造方法 —— 通过反射遍历所有构造方法
        addDefaultConstructor(clazz);
        //处理clazz中的getter方法，填充getMethods和getTypes集合
        addGetMethods(clazz);
        //处理setter方法并填充相关字段
        addSetMethods(clazz);
        //处理没有getter/setter方法的字段
        addFields(clazz);
        //根据getMethods/setMethods集合，初始化可读/写属性的名称集合
        readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
        writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
        //初始化caseInsensitivePropertyMap集合，其中记录了所有大写格式的属性名称
        for (String propName : readablePropertyNames) {
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
        for (String propName : writeablePropertyNames) {
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
    }

    private void addDefaultConstructor(Class<?> clazz) {
        Constructor<?>[] consts = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : consts) {
            if (constructor.getParameterTypes().length == 0) {
                if (canControlMemberAccessible()) {
                    try {
                        constructor.setAccessible(true);
                    } catch (Exception e) {
                        //Ignored. This is only a final precaution, nothing we can do.
                    }
                }
                if (constructor.isAccessible()) {
                    this.defaultConstructor = constructor;
                }
            }
        }
    }

    private void addGetMethods(Class<?> cls) {
        //conflictingGetters集合的key为属性名称，value是相应getter方法的集合，
        //因为子类可能覆盖父类的getter方法，所以同一属性名称可能会存在多个getter方法
        Map<String, List<Method>> conflictingGetters = new HashMap<>();

        //步骤1：获取指定类以及父类和接口中定义的方法
        Method[] methods = getClassMethods(cls);
        //步骤2：按照JavaBean规范查找getter方法，并记录到conflictingGetters集合中
        for (Method method : methods) {
            if (method.getParameterTypes().length > 0) {
                continue;
            }
            String name = method.getName();
            //JavaBean中getter方法的方法名长度大于3且必须以"get"开头
            if ((name.startsWith("get") && name.length() > 3)
                    || (name.startsWith("is") && name.length() > 2)) {
                //方法的参数列表为空
                //按照JavaBean的规范，获取对应的属性名称
                name = PropertyNamer.methodToProperty(name);
                //将属性名与getter方法的对应关系记录到conflictingGetters集合中
                addMethodConflict(conflictingGetters, name, method);
            }
        }
        //步骤3：对conflictingGetters集合进行处理
        resolveGetterConflicts(conflictingGetters);
    }

    private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
        for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {//遍历conflictingGetters集合
            Method winner = null;
            String propName = entry.getKey();

            for (Method candidate : entry.getValue()) {
                /* 同一属性名称存在多个getter方法，则需要比较这些getter方法的返回值，选择getter方法迭代过程中的临时变量，
                 * 用于记录迭代到目前为止最适合作为getter方法的Method */
                if (winner == null) {
                    winner = candidate;
                    continue;
                }
                //记录返回值类型
                Class<?> winnerType = winner.getReturnType();
                Class<?> candidateType = candidate.getReturnType();//获取方法返回值
                if (candidateType.equals(winnerType)) {
                    //返回值相同，这种情况应该在之前就被过滤掉，如果出现，则抛出异常
                    if (!boolean.class.equals(candidateType)) {
                        throw new ReflectionException(
                                "Illegal overloaded getter method with ambiguous type for property "
                                        + propName + " in class " + winner.getDeclaringClass()
                                        + ". This breaks the JavaBeans specification and can cause unpredictable results.");
                    } else if (candidate.getName().startsWith("is")) {
                        winner = candidate;
                    }
                } else if (candidateType.isAssignableFrom(winnerType)) {
                    // 当前方法的返回值是当前最适合的方法的返回值的子类，什么都不做，当前最适合的方法依然不变
                    // OK getter type is descendant
                } else if (winnerType.isAssignableFrom(candidateType)) {
                    // 当前方法的返回值是当前最适合方法的返回值的子类，更新临时变量getter，当前的getter方法成为最合适的getter方法
                    winner = candidate;
                } else {
                    // 返回值相同，二义性，抛出异常
                    throw new ReflectionException(
                            "Illegal overloaded getter method with ambiguous type for property "
                                    + propName + " in class " + winner.getDeclaringClass()
                                    + ". This breaks the JavaBeans specification and can cause unpredictable results.");
                }
            }
            addGetMethod(propName, winner);
        }
    }

    //完成对getMethods集合和getTypes集合的填充
    private void addGetMethod(String name, Method method) {
        //检测属性名是否合法
        if (isValidPropertyName(name)) {
            //将属性名以及对应的MethodInvoker对象添加到getMethods集合中
            getMethods.put(name, new MethodInvoker(method));
            //获取返回值的Type
            Type returnType = TypeParameterResolver.resolveReturnType(method, type);
            //将属性名称及其getter方法的返回值类型添加到getTypes集合中保存
            getTypes.put(name, typeToClass(returnType));
        }
    }

    private void addSetMethods(Class<?> cls) {
        Map<String, List<Method>> conflictingSetters = new HashMap<>();
        Method[] methods = getClassMethods(cls);
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3) {
                if (method.getParameterTypes().length == 1) {
                    name = PropertyNamer.methodToProperty(name);
                    addMethodConflict(conflictingSetters, name, method);
                }
            }
        }
        resolveSetterConflicts(conflictingSetters);
    }

    private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
        List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
        list.add(method);
    }

    private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
        for (String propName : conflictingSetters.keySet()) {
            List<Method> setters = conflictingSetters.get(propName);
            Class<?> getterType = getTypes.get(propName);
            Method match = null;
            ReflectionException exception = null;
            for (Method setter : setters) {
                Class<?> paramType = setter.getParameterTypes()[0];
                if (paramType.equals(getterType)) {
                    // should be the best match
                    match = setter;
                    break;
                }
                if (exception == null) {
                    try {
                        match = pickBetterSetter(match, setter, propName);
                    } catch (ReflectionException e) {
                        // there could still be the 'best match'
                        match = null;
                        exception = e;
                    }
                }
            }
            if (match == null) {
                throw exception;
            } else {
                addSetMethod(propName, match);
            }
        }
    }

    private Method pickBetterSetter(Method setter1, Method setter2, String property) {
        if (setter1 == null) {
            return setter2;
        }
        Class<?> paramType1 = setter1.getParameterTypes()[0];
        Class<?> paramType2 = setter2.getParameterTypes()[0];
        if (paramType1.isAssignableFrom(paramType2)) {
            return setter2;
        } else if (paramType2.isAssignableFrom(paramType1)) {
            return setter1;
        }
        throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '"
                + setter2.getDeclaringClass() + "' with types '" + paramType1.getName() + "' and '"
                + paramType2.getName() + "'.");
    }

    private void addSetMethod(String name, Method method) {
        if (isValidPropertyName(name)) {
            setMethods.put(name, new MethodInvoker(method));
            Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
            setTypes.put(name, typeToClass(paramTypes[0]));
        }
    }

    private Class<?> typeToClass(Type src) {
        Class<?> result = null;
        if (src instanceof Class) {
            result = (Class<?>) src;
        } else if (src instanceof ParameterizedType) {
            result = (Class<?>) ((ParameterizedType) src).getRawType();
        } else if (src instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) src).getGenericComponentType();
            if (componentType instanceof Class) {
                result = Array.newInstance((Class<?>) componentType, 0).getClass();
            } else {
                Class<?> componentClass = typeToClass(componentType);
                result = Array.newInstance((Class<?>) componentClass, 0).getClass();
            }
        }
        if (result == null) {
            result = Object.class;
        }
        return result;
    }

    //处理类中定义的所有字段
    private void addFields(Class<?> clazz) {
        //获取clazz中定义的全部字段
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (canControlMemberAccessible()) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    //Ignored. This is only a final precaution, nothing we can do.
                }
            }
            if (field.isAccessible()) {
                //当setMethods集合不包括同名属性时，将其记录到setMethods集合和setTypes集合
                if (!setMethods.containsKey(field.getName())) {
                    //issue #379 - removed the check for final because JDK 1.5 allows
                    //modification of final fields through reflection (JSR-133). (JGB)
                    //pr #16 - final static can only be set by the classloader
                    int modifiers = field.getModifiers();
                    //过滤掉final和static修饰的字段
                    if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
                        //addSetField()方法的主要功能是填充setMethods集合和setTypes集合，与addGetMethod()方法类似
                        addSetField(field);
                    }
                }
                //当getMethods集合中不包括同名属性时，将其记录到getMethods集合和getTypes集合
                if (!getMethods.containsKey(field.getName())) {
                    //addGetField()方法的主要功能是填充getMethods集合和getTypes集合
                    addGetField(field);
                }
            }
        }
        if (clazz.getSuperclass() != null) {
            //处理父类中定义的字段
            addFields(clazz.getSuperclass());
        }
    }

    private void addSetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            setMethods.put(field.getName(), new SetFieldInvoker(field));
            Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
            setTypes.put(field.getName(), typeToClass(fieldType));
        }
    }

    private void addGetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            getMethods.put(field.getName(), new GetFieldInvoker(field));
            Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
            getTypes.put(field.getName(), typeToClass(fieldType));
        }
    }

    private boolean isValidPropertyName(String name) {
        return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
    }

    /*
     * This method returns an array containing all methods
     * declared in this class and any superclass.
     * We use this method, instead of the simpler Class.getMethods(),
     * because we want to look for private methods as well.
     *
     * @param cls The class
     * @return An array containing all methods in this class
     */
    private Method[] getClassMethods(Class<?> cls) {
        //用于记录指定类中定义的的全部方法的唯一签名以及对应的Method方法
        Map<String, Method> uniqueMethods = new HashMap<>();
        Class<?> currentClass = cls;
        while (currentClass != null && currentClass != Object.class) {
            //记录currentClass这个类中定义的全部方法
            addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

            //we also need to look for interface methods - because the class may be abstract
            //记录接口中定义的方法
            Class<?>[] interfaces = currentClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                addUniqueMethods(uniqueMethods, anInterface.getMethods());
            }

            //获取父类，继续while循环
            currentClass = currentClass.getSuperclass();
        }

        Collection<Method> methods = uniqueMethods.values();
        //转换成Methods数组返回
        return methods.toArray(new Method[methods.size()]);
    }

    private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
        for (Method currentMethod : methods) {
            if (!currentMethod.isBridge()) {
                String signature = getSignature(currentMethod);
                //Check to see if the method is already known, if it is known, then an extended class must have overridden a method
                //检测是否在子类中已经添加过该方法，如果在子类中已经添加过，则表示子类覆盖了该方法，无须在向集合中添加该方法
                if (!uniqueMethods.containsKey(signature)) {
                    if (canControlMemberAccessible()) {
                        try {
                            currentMethod.setAccessible(true);
                        } catch (Exception e) {
                            //Ignored. This is only a final precaution, nothing we can do.
                        }
                    }
                    //记录该签名和方法的对应关系
                    uniqueMethods.put(signature, currentMethod);
                }
            }
        }
    }

    //通过一下方法返回的签名格式：返回值类型#方法名称：参数类型列表
    //例如，以下方法的唯一签名是：java.lang.String#getSignature:java.lang.reflect.Method
    //该方法返回的方法签名可作为全局唯一标识
    private String getSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        Class<?> returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(returnType.getName()).append('#');
        }
        sb.append(method.getName());
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i == 0) {
                sb.append(':');
            } else {
                sb.append(',');
            }
            sb.append(parameters[i].getName());
        }
        return sb.toString();
    }

    /**
     * Checks whether can control member accessible.
     *
     * @return If can control member accessible, it return {@literal true}
     * @since 3.5.0
     */
    public static boolean canControlMemberAccessible() {
        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (null != securityManager) {
                securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
            }
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }

    /*
     * Gets the name of the class the instance provides information for
     *
     * @return The class name
     */
    public Class<?> getType() {
        return type;
    }

    public Constructor<?> getDefaultConstructor() {
        if (defaultConstructor != null) {
            return defaultConstructor;
        } else {
            throw new ReflectionException("There is no default constructor for " + type);
        }
    }

    public boolean hasDefaultConstructor() {
        return defaultConstructor != null;
    }

    public Invoker getSetInvoker(String propertyName) {
        Invoker method = setMethods.get(propertyName);
        if (method == null) {
            throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    public Invoker getGetInvoker(String propertyName) {
        Invoker method = getMethods.get(propertyName);
        if (method == null) {
            throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    /*
     * Gets the type for a property setter
     *
     * @param propertyName - the name of the property
     * @return The Class of the propery setter
     */
    public Class<?> getSetterType(String propertyName) {
        Class<?> clazz = setTypes.get(propertyName);
        if (clazz == null) {
            throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    /*
     * Gets the type for a property getter
     *
     * @param propertyName - the name of the property
     * @return The Class of the propery getter
     */
    public Class<?> getGetterType(String propertyName) {
        Class<?> clazz = getTypes.get(propertyName);
        if (clazz == null) {
            throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    /*
     * Gets an array of the readable properties for an object
     *
     * @return The array
     */
    public String[] getGetablePropertyNames() {
        return readablePropertyNames;
    }

    /*
     * Gets an array of the writeable properties for an object
     *
     * @return The array
     */
    public String[] getSetablePropertyNames() {
        return writeablePropertyNames;
    }

    /*
     * Check to see if a class has a writeable property by name
     *
     * @param propertyName - the name of the property to check
     * @return True if the object has a writeable property by the name
     */
    public boolean hasSetter(String propertyName) {
        return setMethods.keySet().contains(propertyName);
    }

    /*
     * Check to see if a class has a readable property by name
     *
     * @param propertyName - the name of the property to check
     * @return True if the object has a readable property by the name
     */
    public boolean hasGetter(String propertyName) {
        return getMethods.keySet().contains(propertyName);
    }

    public String findPropertyName(String name) {
        return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
    }
}
