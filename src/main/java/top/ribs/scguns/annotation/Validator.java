package top.ribs.scguns.annotation;

import java.io.InvalidObjectException;
import java.lang.reflect.Field;

public class Validator {
   public Validator() {
      super();
   }

   public static <T> boolean isValidObject(T t) throws IllegalAccessException, InvalidObjectException {
      Field[] fields = t.getClass().getDeclaredFields();

      for (Field field : fields) {
         if (field.getDeclaredAnnotation(Optional.class) == null) {
            field.setAccessible(true);
            if (field.get(t) == null) {
               throw new InvalidObjectException("Missing required property: " + field.getName());
            }

            if (!field.getType().isPrimitive()
               && field.getType() != String.class
               && !field.getType().isEnum()
               && field.getDeclaredAnnotation(Ignored.class) == null) {
               return isValidObject(field.get(t));
            }
         }
      }

      return true;
   }
}
