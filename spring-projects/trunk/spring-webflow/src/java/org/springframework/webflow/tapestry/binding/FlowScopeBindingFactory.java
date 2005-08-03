package org.springframework.webflow.tapestry.binding;

import org.apache.hivemind.Location;
import org.apache.tapestry.IBinding;
import org.apache.tapestry.IComponent;
import org.apache.tapestry.binding.AbstractBindingFactory;

public class FlowScopeBindingFactory extends AbstractBindingFactory {
	public IBinding createBinding(IComponent root, String bindingDescription,
			String path, Location location) {
		return new FlowScopeBinding(root.getPage(), path, bindingDescription,
				getValueConverter(), location);
	}
}
