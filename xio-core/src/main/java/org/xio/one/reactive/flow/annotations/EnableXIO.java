package org.xio.one.reactive.flow.annotations;

import org.xio.one.reactive.flow.XIOService;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableXIO {
  Class<?> app() default XIOService.class;
}
