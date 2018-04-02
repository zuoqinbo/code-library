

import org.apache.commons.lang.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class EnumUtil {

    private static final Map<String, Map<Object, Object>> enumHolder = new HashMap<String, Map<Object, Object>>();

    public static Map<Object, Object> getEnumKeyValueMapping(String enumClassName) throws Exception {
        if (enumHolder.containsKey(enumClassName)) {
            return enumHolder.get(enumClassName);
        }

        final Class<?> clazz = Class.forName(enumClassName);
        final Method valuesMethod = clazz.getMethod("values");
        final Object[] values = (Object[]) valuesMethod.invoke(null);
        final Map<Object, Object> result = new TreeMap<Object, Object>();
        for (final Object object : values) {
            result.put(MethodUtils.invokeMethod(object, "getKey", null),
                    MethodUtils.invokeMethod(object, "getValue", null));
        }
        enumHolder.put(enumClassName, result);
        return result;

    }

    public static Object getEnumValue(String enumClassName, Object key) throws Exception {
        if (!enumHolder.containsKey(enumClassName)) {
            getEnumKeyValueMapping(enumClassName);
        }
        final Map<Object, Object> enumValues = enumHolder.get(enumClassName);
        return enumValues.get(key);
    }

}