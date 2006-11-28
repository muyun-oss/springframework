/*
 * Copyright 2005-2006 the original author or authors.
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
package org.springframework.ldap.samples.person.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.ldap.samples.person.domain.Group;
import org.springframework.ldap.samples.person.service.GroupService;
import org.springframework.util.Assert;
import org.springframework.webflow.action.FormAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action implementation that handles edit of a group. This action has the
 * capability to add and remove members of the current Group form object.
 * 
 * @author Ulrik Sandberg
 */
public class EditGroupFormAction extends FormAction {

    private GroupService groupService;

    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    /*
     * @see org.springframework.webflow.action.FormAction#initAction()
     */
    protected void initAction() {
        super.initAction();
        Assert.notNull(groupService, "A GroupService object is required");
    }

    /*
     * @see org.springframework.webflow.action.FormAction#createFormObject(org.springframework.webflow.execution.RequestContext)
     */
    protected Object createFormObject(RequestContext context) throws Exception {
        String name = context.getFlowScope().getRequiredString("name");
        Group group = groupService.findByPrimaryKey(name);
        return group;
    }

    /**
     * Removes the members in the <code>selectedMembers</code> property from
     * the form object <code>members</code> property.
     * 
     * @param context
     *            RequestContext with parameters and attributes.
     * @return The <code>success</code> event if members were removed
     *         successfully
     * @throws Exception
     *             if an unexpected error occurs
     */
    public Event remove(RequestContext context) throws Exception {
        Group group = (Group) getFormObject(context);
        Collection members = group.getMembers();
        String[] selectedMembers = (String[]) context.getRequestParameters()
                .getArray("selectedMembers", String.class);
        List membersToRemove = Arrays.asList(selectedMembers);
        Collection adjustedMembers = CollectionUtils.subtract(members,
                membersToRemove);
        group.setMembers(adjustedMembers);
        return success();
    }
}
