/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.config.java.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation containing the pointcut for a Spring advice. This can either
 * contain an AspectJ expression or a value.
 * 
 * @author Rod Johnson
 */

// TODO pluggable languages? Probably better to standardize on AspectJ
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SpringAdvice {

	/**
	 * AspectJ pointcut expression value
	 * @return
	 */
	String value() default "";

	/**
	 * If this is true, the value will be discarded
	 * @return
	 */
	boolean matchAll() default false;

	// String lang() default "AJ";

}
