package spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * User: zhaohuiyu
 * Date: 7/5/13
 * Time: 5:37 PM
 */
public class QScheduleNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("config", new QScheduleBeanDefinitionParser());
        registerBeanDefinitionParser("task", new TaskBeanDefinitionParser());
    }
}
