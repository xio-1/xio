package org.xio.one.reactive.flow.annotations;

import org.xio.one.reactive.flow.XIOService;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableXIO {
  Class<?> app() default XIOService.class;
}
