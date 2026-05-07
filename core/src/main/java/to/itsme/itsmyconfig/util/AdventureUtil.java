package to.itsme.itsmyconfig.util;

import net.kyori.adventure.text.Component;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class AdventureUtil {

    private static final Class<?> COMPONENT_CLASS;
    private static final Object GSON_SERIALIZER_INSTANCE;
    private static final Method SERIALIZE_METHOD;
    private static final Method DESERIALIZE_METHOD;

    static {
        Class<?> componentClass = null;
        Object gsonSerializer = null;
        Method serialize = null;
        Method deserialize = null;

        try {
            componentClass = Class.forName("net{}kyori{}adventure{}text{}Component".replace("{}", "."));
            Class<?> gsonSerializerClass = Class.forName("net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer".replace("{}", "."));
            Method gsonMethod = gsonSerializerClass.getMethod("gson");
            gsonSerializer = gsonMethod.invoke(null);
            serialize = gsonSerializerClass.getMethod("serialize", componentClass);
            deserialize = gsonSerializer.getClass().getMethod("deserialize", String.class);
            serialize.setAccessible(true);
            deserialize.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        COMPONENT_CLASS = componentClass;
        GSON_SERIALIZER_INSTANCE = gsonSerializer;
        SERIALIZE_METHOD = serialize;
        DESERIALIZE_METHOD = deserialize;
    }

    private AdventureUtil() {}

    public static Class<?> getComponentClass() {
        return COMPONENT_CLASS;
    }

    public static String serialize(Object componentObject) {
        if (GSON_SERIALIZER_INSTANCE == null || SERIALIZE_METHOD == null) return null;
        try {
            return (String) SERIALIZE_METHOD.invoke(GSON_SERIALIZER_INSTANCE, componentObject);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object deserialize(String json) {
        if (GSON_SERIALIZER_INSTANCE == null || DESERIALIZE_METHOD == null) return null;
        try {
            return DESERIALIZE_METHOD.invoke(GSON_SERIALIZER_INSTANCE, json);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Component toComponent(Object componentObject) {
        if (componentObject == null) return null;
        String json = serialize(componentObject);
        if (json == null) return null;
        try {
            return Utilities.GSON_SERIALIZER.deserialize(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object fromComponent(Component component) {
        if (component == null) return null;
        try {
            String json = Utilities.GSON_SERIALIZER.serialize(component);
            return deserialize(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
