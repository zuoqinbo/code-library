package spring;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * User: zhaohuiyu
 * Date: 7/5/13
 * Time: 6:36 PM
 */
public class TaskBean implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(TaskBean.class);

    private String method;
    private Object ref;
    private String jobName;
    private ApplicationContext applicationContext;
    private Scheduler scheduler;
    private Method targetMethod;
    private String clazz;
    private ThreadPoolExecutor executor;

    public TaskBean() {
        super();
    }

    public void schedule() {
        Preconditions.checkNotNull(ref);
        Preconditions.checkNotNull(jobName);
        Preconditions.checkNotNull(applicationContext);

        final Method scheduledMethod = Preconditions.checkNotNull(getTargetMethod(), "在类 " + ref.getClass().getCanonicalName() + " 中未找到 " + this.method + " 的方法");
        Preconditions.checkArgument(validateParameter(scheduledMethod), "方法必须无参或有一个类型为" + Parameter.class.getCanonicalName() + "的参数");

        scheduler = BeanFactoryUtils.beanOfType(applicationContext, Scheduler.class);
        Preconditions.checkNotNull(scheduler, "请配置<qschedule:config address=\"zkAddress\" />");

        Worker worker = new Worker() {
            @Override
            public void doWork(Parameter parameter) {
                try {
                    ReflectionUtils.makeAccessible(scheduledMethod);
                    if (scheduledMethod.getParameterTypes().length == 0) {
                        scheduledMethod.invoke(ref);
                    } else {
                        scheduledMethod.invoke(ref, parameter);
                    }
                } catch (IllegalAccessException e) {
                    logger.warn("It's impossible", e);
                } catch (InvocationTargetException e) {
                    throw new WrapperException(e.getTargetException());
                }
            }
        };
        if (executor == null) {
            scheduler.schedule(jobName, worker);
        } else {
            scheduler.schedule(jobName, worker, executor);
        }
    }

    private boolean validateParameter(Method scheduledMethod) {
        if (scheduledMethod.getParameterTypes().length == 0) return Boolean.TRUE;
        if (scheduledMethod.getParameterTypes().length == 1) {
            Class<?> parameterType = scheduledMethod.getParameterTypes()[0];
            return parameterType.isAssignableFrom(Parameter.class);
        }
        return Boolean.FALSE;
    }

    private Method getTargetMethod() {
        if (targetMethod != null) return targetMethod;
        if (StringUtils.hasText(clazz)) {
            return resolveMethod(resolveClass(clazz));
        }
        return resolveMethod(ref.getClass());
    }

    private Class resolveClass(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            logger.error("Can not found this class: {}", clazz);
            throw new RuntimeException("Can not found this class " + clazz);
        }
    }

    private Method resolveMethod(Class clazz) {
        try {
            return clazz.getDeclaredMethod(method);
        } catch (NoSuchMethodException e) {
            logger.warn("Can not found method " + method + " in " + clazz.getCanonicalName());
        }
        try {
            return clazz.getMethod(method);
        } catch (NoSuchMethodException e) {
            logger.warn("Can not found method " + method + " in " + clazz.getCanonicalName());
        }
        try {
            return clazz.getDeclaredMethod(method, Parameter.class);
        } catch (NoSuchMethodException e) {
            logger.warn("Can not found method " + method + "(Parameter) in " + clazz.getCanonicalName());
        }
        try {
            return clazz.getMethod(method, Parameter.class);
        } catch (NoSuchMethodException e) {
            logger.warn("Can not found method " + method + "(Parameter) in " + clazz.getCanonicalName());
        }
        return null;
    }


    public void setRef(Object ref) {
        this.ref = ref;
    }

    /**
     * call by spring handler
     *
     * @param method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * set schedule bean class
     *
     * @param clazz
     */
    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    /**
     * set tasked method, for annotation
     *
     * @param method
     */
    public void setTargetMethod(Method method) {
        this.targetMethod = method;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
