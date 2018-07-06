package spring;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * User: zhaohuiyu
 * Date: 7/5/13
 * Time: 6:23 PM
 */
public class QScheduleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    private static final Logger logger = LoggerFactory.getLogger(QScheduleBeanDefinitionParser.class);

    private static final String QSCHEDULE_ID = "xxx_QSCHEDULE_ID";

    private static final String QSCHEDULE_ANNOTATION = "QSCHEDULE_ANNOTATION";

    private volatile boolean init = false;

    @Override
    protected Class<?> getBeanClass(Element element) {
        return SchedulerProvider.class;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        if (init) {
            throw new RuntimeException("一个应用中只允许配置一个<qschedule:config>");
        }
        builder.setInitMethodName("init");
        builder.setDestroyMethodName("destroy");
        String address = element.getAttribute("address");
        if (!Strings.isNullOrEmpty(address)) {
            builder.addConstructorArgValue(address);
            logger.warn("使用最新版本的qschedule客户端无需额外配置了zk地址了");
        }

        String port = element.getAttribute("port");
        if (!Strings.isNullOrEmpty(port)) {
            builder.addPropertyValue("port", port);
        }
        builder.addPropertyValue("root", element.getAttribute("root"));
        if (!parserContext.getRegistry().containsBeanDefinition(QSCHEDULE_ANNOTATION)) {
            RootBeanDefinition annotation = new RootBeanDefinition(AnnotationBean.class);
            parserContext.getRegistry().registerBeanDefinition(QSCHEDULE_ANNOTATION, annotation);
        }

        init = true;
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return QSCHEDULE_ID;
    }
}
