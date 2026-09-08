package com.fireway.backend.shared.annotation;
import java.lang.annotation.*;
@Documented @Retention(RetentionPolicy.SOURCE) @Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface Owner { String value(); }
