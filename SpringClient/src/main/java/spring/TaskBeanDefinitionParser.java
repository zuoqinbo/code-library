package spring;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * User: zhaohuiyu
 * Date: 7/11/13
 * Time: 10:49 AM
 */
public class TaskBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    @Override
    protected Class<?> getBeanClass(Element element) {
        return TaskBean.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String ref = element.getAttribute("ref");
        if (!StringUtils.hasText(ref)) {
            parserContext.getReaderContext().error("Property 'ref' required", element);
        }

        String clazz = element.getAttribute("class");
        if (StringUtils.hasText(clazz)) {
            builder.addPropertyValue("clazz", clazz);
        }

        String executor = element.getAttribute("executor");
        if (StringUtils.hasText(executor)) {
            builder.addPropertyReference("executor", executor);
        }

        builder.addPropertyReference("ref", ref);
        builder.addPropertyValue("method", element.getAttribute("method"));

        String jobName = Preconditions.checkNotNull(element.getAttribute("id"), "id会作为任务名称，不能为空");
        builder.addPropertyValue("jobName", jobName);
        builder.setInitMethodName("schedule");
    }
}
