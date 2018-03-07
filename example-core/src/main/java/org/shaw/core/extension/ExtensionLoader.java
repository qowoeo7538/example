package org.shaw.core.extension;

import org.shaw.common.Constants;
import org.shaw.util.ClassUtils;
import org.shaw.util.ConcurrentHashSet;
import org.shaw.util.Holder;
import org.shaw.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

public class ExtensionLoader<T> {

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    /** 构建过的对象, 将保存到该对象中进行缓存 */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /** 默认 SPI 目录 */
    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    /** 自定义 SPI 目录 */
    private static final String DIRECTORY = "META-INF/example/";

    /** 自定义 SPI 目录 */
    private static final String INTERNAL_DIRECTORY = DIRECTORY + "internal/";

    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    private final Map<String, Activate> cachedActivates = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    private final ExtensionFactory objectFactory;

    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();

    /** 维护一个 Object 对象 */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<>();

    /** 维护一个 Map<String, Class> 对象 */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /** SPI 接口 */
    private final Class<?> type;

    /** SPI 实现类 */
    private volatile Class<?> cachedAdaptiveClass = null;

    /**
     * 为保证错误及时获取，通过 volatile 每次获取主内存的值
     */
    private volatile Throwable createAdaptiveInstanceError;

    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

    private Set<Class<?>> cachedWrapperClasses;

    private String cachedDefaultName;

    /**
     * ExtensionLoader<type> 基于 ExtensionLoader<ExtensionFactory> 构建,
     * 所以会先构建 ExtensionLoader<ExtensionFactory> 对象
     *
     * @param type
     */
    private ExtensionLoader(Class<?> type) {
        this.type = type;
        // 创建 SPI 的工厂类,忽略掉本身就是工厂的 ExtensionFactory 类
        objectFactory = (type == ExtensionFactory.class ? null :
                ExtensionLoader
                        // 获取 ExtensionFactory 的 ExtensionLoader 对象
                        .getExtensionLoader(ExtensionFactory.class)
                        .getAdaptiveExtension());
    }

    /**
     * 设计模式: 单例双重检查
     */
    public T getAdaptiveExtension() {
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            if (createAdaptiveInstanceError == null) {
                synchronized (cachedAdaptiveInstance) {
                    instance = cachedAdaptiveInstance.get();
                    if (instance == null) {
                        try {
                            instance = createAdaptiveExtension();
                            cachedAdaptiveInstance.set(instance);
                        } catch (Throwable t) {
                            createAdaptiveInstanceError = t;
                            throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                        }
                    }
                }
            } else {
                throw new IllegalStateException("fail to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
            }
        }
        return (T) instance;
    }

    /**
     * 尝试从 {@link #EXTENSION_LOADERS} 获取 {@code ExtensionLoader}，
     * 如果为 {@code null}, 再构造该 Class 的 {@code ExtensionLoader} 放入 {@link #EXTENSION_LOADERS}
     *
     * @param type 类类型对象
     * @see #withExtensionAnnotation(Class)
     */
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type 不能为 null!");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException(type + "不是一个接口类!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException(type + "不是一个扩展接口, 没有 " + SPI.class.getSimpleName() + " 注解!");
        }
        // 尝试获取该 type 的 ExtensionLoader 对象
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            // 构建 ExtensionLoader(type) 对象
            // 如果 key 存在则不做修改
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    public T getExtension(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Extension name == null");
        }
        if (Constants.TRUE.equals(name)) {
            return getDefaultExtension();
        }
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    public T getDefaultExtension() {
        getExtensionClasses();
        if (null == cachedDefaultName || cachedDefaultName.length() == 0 || Constants.TRUE.equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<>(clazzes.keySet()));
    }

    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            injectExtension(instance);
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);
        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }

    /**
     * 判断该方法是否包含 SPI 注解
     *
     * @param type 类类型对象
     * @param <T>
     * @return 如果包含 SPI 注解则返回 {@code true}
     */
    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    private T createAdaptiveExtension() {
        try {
            /**
             * 注意：通过 {@link Class#newInstance()} 创建实例，会绕过编译时的异常检查。如果不希望如此，
             *      建议通过反射构造函数来创建实例 {@link java.lang.reflect.Constructor#newInstance(Object...)}
             */
            return injectExtension((T) getAdaptiveExtensionClass().getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("不能创建 adaptive extension: " + type + ", 原因: " + e.getMessage(), e);
        }
    }

    /**
     * @see #getExtensionClasses()
     */
    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    private Class<?> createAdaptiveExtensionClass() {
        String code = createAdaptiveExtensionClassCode();
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        org.shaw.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.shaw.compiler.Compiler.class)
                .getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }

    private String createAdaptiveExtensionClassCode() {
        StringBuilder codeBuidler = new StringBuilder();
        Method[] methods = type.getMethods();
        boolean hasAdaptiveAnnotation = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Adaptive.class)) {
                hasAdaptiveAnnotation = true;
                break;
            }
        }

        if (!hasAdaptiveAnnotation) {
            throw new IllegalStateException("No adaptive method on extension " + type.getName()
                    + ", refuse to create the adaptive class!");
        }

        codeBuidler.append("package " + type.getPackage().getName() + ";");
        codeBuidler.append("\nimport " + ExtensionLoader.class.getName() + ";");
        codeBuidler.append("\npublic class " + type.getSimpleName() + "$Adaptive" + " implements "
                + type.getCanonicalName() + " {");

        for (Method method : methods) {
            // 获取返回类型
            Class<?> rt = method.getReturnType();
            // 获取参数类型
            Class<?>[] pts = method.getParameterTypes();
            // 获取方法异常类型
            Class<?>[] ets = method.getExceptionTypes();

            Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
            StringBuilder code = new StringBuilder(512);
            if (adaptiveAnnotation == null) {
                code.append("throw new UnsupportedOperationException(\"method ")
                        .append(method.toString()).append(" of interface ")
                        .append(type.getName()).append(" is not adaptive method!\");");
            } else {
                String[] value = adaptiveAnnotation.value();
                if (value.length == 0) {
                    StringBuilder sb = new StringBuilder(128);
                    /**
                     * 类名转换, 如 ExtensionLoader 转成 Extension.loader
                     */
                    char[] charArray = type.getSimpleName().toCharArray();
                    for (int i = 0; i < charArray.length; i++) {
                        if (Character.isUpperCase(charArray[i])) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(Character.toLowerCase(charArray[i]));
                        } else {
                            sb.append(charArray[i]);
                        }
                    }
                    value = new String[]{sb.toString()};
                }
                String getNameCode = null;
                code.append("\nString extName = ").append(getNameCode).append(";");
                String s = String.format("\nif(extName == null) " +
                                "throw new IllegalStateException(\"Fail to get extension(%s) name from url(\" + url.toString() + \") use keys(%s)\");",
                        type.getName(), Arrays.toString(value));
                code.append(s);

                s = String.format("\n%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);",
                        type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
                code.append(s);

                if (!rt.equals(void.class)) {
                    code.append("\nreturn ");
                }

                s = String.format("extension.%s(", method.getName());
                code.append(s);
                for (int i = 0; i < pts.length; i++) {
                    if (i != 0) {
                        code.append(", ");
                    }
                    code.append("arg").append(i);
                }
                code.append(");");
            }

            codeBuidler.append("\npublic " + rt.getCanonicalName() + " " + method.getName() + "(");
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    codeBuidler.append(", ");
                }
                codeBuidler.append(pts[i].getCanonicalName());
                codeBuidler.append(" ");
                codeBuidler.append("arg" + i);
            }
            codeBuidler.append(")");
            if (ets.length > 0) {
                codeBuidler.append(" throws ");
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        codeBuidler.append(", ");
                    }
                    codeBuidler.append(ets[i].getCanonicalName());
                }
            }
            codeBuidler.append(" {");
            codeBuidler.append(code.toString());
            codeBuidler.append("\n}");
        }
        codeBuidler.append("\n}");
        return codeBuidler.toString();
    }

    /**
     * 设计模式: 双重检查保证单例
     *
     * @return
     * @see #loadExtensionClasses()
     */
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private Map<String, Class<?>> loadExtensionClasses() {
        // 获取 SPI 接口的注解信息
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if (value != null && (value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) {
                    cachedDefaultName = names[0];
                }
            }
        }
        Map<String, Class<?>> extensionClasses = new HashMap<>(16);
        loadFile(extensionClasses, INTERNAL_DIRECTORY);
        loadFile(extensionClasses, DIRECTORY);
        loadFile(extensionClasses, SERVICES_DIRECTORY);
        return extensionClasses;
    }

    /**
     * 加载 class 文件
     *
     * @param extensionClasses
     * @param dir              servers 文件目录
     */
    private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
        // 获取 SPI 接口描述文件
        String fileName = dir + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
            // 加载 SPI 接口描述文件
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    try {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(url.openStream(), "utf-8"))) {
                            String line;
                            // 获取文件每行信息
                            while ((line = reader.readLine()) != null) {
                                // 截取 "#" 之前的信息
                                final int ci = line.indexOf('#');
                                if (ci >= 0) {
                                    line = line.substring(0, ci);
                                }
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            // 实现类别名
                                            name = line.substring(0, i).trim();
                                            // 实现类全名
                                            line = line.substring(i + 1).trim();
                                        }
                                        if (line.length() > 0) {
                                            // 加载 SPI 实现类
                                            Class<?> clazz = Class.forName(line, true, classLoader);
                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("加载的类 " + clazz.getName()
                                                        + " 不是 " + type + " 的子类或实现类");
                                            }
                                            if (clazz.isAnnotationPresent(Adaptive.class)) {
                                                if (cachedAdaptiveClass == null) {
                                                    cachedAdaptiveClass = clazz;
                                                } else if (!cachedAdaptiveClass.equals(clazz)) {
                                                    throw new IllegalStateException("More than 1 adaptive class found: "
                                                            + cachedAdaptiveClass.getClass().getName()
                                                            + ", " + clazz.getClass().getName());
                                                }
                                            } else {
                                                try {
                                                    // 获取指定参数的构造函数
                                                    clazz.getConstructor(type);
                                                    Set<Class<?>> wrappers = cachedWrapperClasses;
                                                    if (wrappers == null) {
                                                        cachedWrapperClasses = new ConcurrentHashSet<>();
                                                        wrappers = cachedWrapperClasses;
                                                    }
                                                } catch (NoSuchMethodException e) {
                                                    clazz.getConstructor();
                                                    if (name == null || name.length() == 0) {
                                                        if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                                                && clazz.getSimpleName().endsWith(type.getSimpleName())) {
                                                            name = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
                                                        } else {
                                                            throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
                                                        }
                                                    }
                                                    String[] names = NAME_SEPARATOR.split(name);
                                                    if (names != null && names.length > 0) {
                                                        Activate activate = clazz.getAnnotation(Activate.class);
                                                        if (activate != null) {
                                                            cachedActivates.put(names[0], activate);
                                                        }
                                                        for (String n : names) {
                                                            if (!cachedNames.containsKey(clazz)) {
                                                                cachedNames.put(clazz, n);
                                                            }
                                                            Class<?> c = extensionClasses.get(n);
                                                            if (c == null) {
                                                                extensionClasses.put(n, clazz);
                                                            } else if (c != clazz) {
                                                                throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Throwable t) {
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            }
                        }
                    } catch (Throwable t) {
                        // todo 日志
                    }
                }
            }
        } catch (Throwable t) {
            // todo 日志
        }
    }

    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                // 遍历 instance 所有的 public 方法
                for (Method method : instance.getClass().getMethods()) {
                    /**
                     * 条件：
                     * 1) 方法名以 set 开头
                     * 2) 方法只有一个参数
                     * 3) 方法的修饰符为 public
                     */
                    if (method.getName().startsWith("set")
                            && method.getParameterTypes().length == 1
                            && Modifier.isPublic(method.getModifiers())) {
                        Class<?> pt = method.getParameterTypes()[0];
                        try {
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
                            Object object = objectFactory.getExtension(pt, property);
                            if (object != null) {
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            //TODO 日志
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            //TODO 日志
            e.printStackTrace();
        }
        return instance;
    }
}
