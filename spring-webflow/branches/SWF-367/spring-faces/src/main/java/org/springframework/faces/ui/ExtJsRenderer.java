package org.springframework.faces.ui;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.springframework.faces.ui.resource.FlowResourceHelper;

public class ExtJsRenderer extends Renderer {

	private static final String EXT_CSS = "/ext/resources/css/ext-all.css";

	private static final String EXT_SCRIPT = "/ext/ext.js";

	private static final String SPRING_FACES_SCRIPT = "/spring-faces/SpringFaces.js";

	private FlowResourceHelper resourceHelper = new FlowResourceHelper();

	public void encodeBegin(FacesContext context, UIComponent component) throws IOException {

		ExtJsComponent extJsComponent = (ExtJsComponent) component;

		if (extJsComponent.getIncludeExtStyles().equals(Boolean.TRUE)) {
			resourceHelper.renderStyleLink(context, EXT_CSS);
		}

		if (extJsComponent.getIncludeExtScript().equals(Boolean.TRUE)) {
			resourceHelper.renderScriptLink(context, EXT_SCRIPT);
		}

		resourceHelper.renderScriptLink(context, SPRING_FACES_SCRIPT);
	}
}
