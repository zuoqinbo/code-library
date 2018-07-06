package spring;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * User: zhaohuiyu
 * Date: 7/5/13
 * Time: 7:57 PM
 */
public class AnnotationBean implements BeanPostProcessor, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationBean.class);

    private ApplicationContext applicationContext;

    private final Set<String> registerJobs = new HashSet<String>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        parseMethods(bean, bean.getClass().getDeclaredMethods());
        return bean;
    }

    private void parseMethods(Object bean, Method[] methods) {
        for (Method method : methods) {
            QSchedule annotation = AnnotationUtils.findAnnotation(method, QSchedule.class);
            if (annotation == null) continue;

            String jobName = annotation.value();
            if (Strings.isNullOrEmpty(jobName)) {
                jobName = bean.getClass().getCanonicalName();
            }
            if (registerJobs.contains(jobName)) {
                throw new IllegalStateException("已经注册了名为" + jobName + "的调度任务");
            }
            registerJobs.add(jobName);
            TaskBean taskBean = new TaskBean();
            attachExecutor(taskBean, annotation);
            taskBean.setApplicationContext(applicationContext);
            taskBean.setTargetMethod(method);
            taskBean.setRef(bean);
            taskBean.setJobName(jobName);
            taskBean.schedule();
        }
    }

    private void attachExecutor(TaskBean taskBean, QSchedule annotation) {
        try {
            if (!Strings.isNullOrEmpty(annotation.executor())) {
                ThreadPoolExecutor executor = applicationContext.getBean(annotation.executor(), ThreadPoolExecutor.class);
                if (executor != null) taskBean.setExecutor(executor);
            }
        } catch (Exception e) {
            logger.warn("Can not found {} of type ThreadPoolExecutor", annotation.executor());
        }
    }
}
