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
package org.springframework.webflow.config;

import java.io.Serializable;

import org.springframework.core.style.ToStringCreator;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;

/**
 * Encapsulates overall flow system configuration defaults. Allows for
 * centralized application of, and if necessary, overridding of system-wide
 * default values.
 * 
 * @author Keith Donald
 */
public class FlowSystemDefaults implements Serializable {

	/**
	 * The default 'alwaysRedirectOnPause' execution attribute value.
	 */
	private Boolean alwaysRedirectOnPause = Boolean.TRUE;

	/**
	 * The default flow execution repository type.
	 */
	private RepositoryType repositoryType = RepositoryType.CONTINUATION;

	/**
	 * Overrides the alwaysRedirectOnPause execution attribute default.
	 * @param alwaysRedirectOnPause the new default value
	 */
	public void setAlwaysRedirectOnPause(Boolean alwaysRedirectOnPause) {
		this.alwaysRedirectOnPause = alwaysRedirectOnPause;
	}

	/**
	 * Overrides the default repository type.
	 * @param repositoryType the new default value
	 */
	public void setRepositoryType(RepositoryType repositoryType) {
		this.repositoryType = repositoryType;
	}

	/**
	 * Applys default execution attributes if necessary. Defaults will only
	 * apply in the case where the user did not configure a value, or explicitly
	 * requested the 'default' value.
	 * @param executionAttributes the user-configured execution attribute map
	 * @return the map with defaults applied as appropriate.
	 */
	public MutableAttributeMap applyExecutionAttributes(MutableAttributeMap executionAttributes) {
		if (executionAttributes == null) {
			executionAttributes = new LocalAttributeMap(1, 1);
		}
		if (!executionAttributes.contains("alwaysRedirectOnPause")) {
			executionAttributes.put("alwaysRedirectOnPause", alwaysRedirectOnPause);
		}
		return executionAttributes;
	}

	/**
	 * Applys the default repository type if requested by the user.
	 * @param selectedType the selected repository type (may be null if no
	 * selection was made)
	 * @return the repository type, with the default applied if necessary
	 */
	public RepositoryType applyIfNecessary(RepositoryType selectedType) {
		if (selectedType == null) {
			return repositoryType;
		}
		else {
			return selectedType;
		}
	}

	public String toString() {
		return new ToStringCreator(this).append("alwaysRedirectOnPause", alwaysRedirectOnPause).append(
				"repositoryType", repositoryType).toString();
	}
}