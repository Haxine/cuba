/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.haulmont.cuba.web.widgets.client.tabsheet;

import com.haulmont.cuba.web.widgets.CubaMainTabSheet;
import com.haulmont.cuba.web.widgets.client.action.RemoteAction;
import com.haulmont.cuba.web.widgets.client.action.StaticActionOwner;
import com.haulmont.cuba.web.widgets.client.addons.dragdroplayouts.ui.VDragDropUtil;
import com.haulmont.cuba.web.widgets.client.addons.dragdroplayouts.ui.tabsheet.DDTabsheetConnector;
import com.haulmont.cuba.web.widgets.client.addons.dragdroplayouts.ui.tabsheet.VDDTabsheetDropHandler;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.UIDL;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.Action;
import com.vaadin.shared.ui.Connect;

@Connect(CubaMainTabSheet.class)
public class CubaMainTabSheetConnector extends DDTabsheetConnector {

    protected CubaMainTabSheetServerRpc rpc = RpcProxy.create(CubaMainTabSheetServerRpc.class, this);

    protected int lastContextMenuX = -1;
    protected int lastContextMenuY = -1;

    public CubaMainTabSheetConnector() {
        //noinspection Convert2Lambda
        registerRpc(CubaMainTabSheetClientRpc.class, new CubaMainTabSheetClientRpc() {
            @Override
            public void showTabContextMenu(final int tabIndex, ClientAction[] actions) {
                StaticActionOwner actionOwner = new StaticActionOwner(getConnection(), getConnectorId());

                Action[] contextMenuActions = new Action[actions.length];

                for (int i = 0; i < contextMenuActions.length; i++) {
                    contextMenuActions[i] = new RemoteAction(actions[i], actionOwner) {
                        @Override
                        public void execute() {
                            rpc.performAction(tabIndex, actionId);

                            getConnection().getContextMenu().hide();
                        }
                    };
                }

                actionOwner.setActions(contextMenuActions);

                if (lastContextMenuX >= 0 && lastContextMenuY >= 0) {
                    getConnection().getContextMenu().showAt(actionOwner, lastContextMenuX, lastContextMenuY);

                    lastContextMenuX = -1;
                    lastContextMenuY = -1;
                }
            }
        });
    }

    @Override
    public CubaMainTabSheetWidget getWidget() {
        return (CubaMainTabSheetWidget) super.getWidget();
    }

    @Override
    protected void init() {
        super.init();

        getWidget().tabContextMenuHandler = (tabIndex, event) -> {
            lastContextMenuX = WidgetUtil.getTouchOrMouseClientX(event.getNativeEvent());
            lastContextMenuY = WidgetUtil.getTouchOrMouseClientY(event.getNativeEvent());

            if (getState().hasActionsHandlers) {
                rpc.onTabContextMenu(tabIndex);

                event.stopPropagation();
                event.preventDefault();
            }
        };
    }

    @Override
    public CubaMainTabSheetState getState() {
        return (CubaMainTabSheetState) super.getState();
    }

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        VDragDropUtil.updateDropHandlerFromUIDL(uidl, this, new VDDTabsheetDropHandler(this));
        if (getState().ddHtmlEnable) {
            enableDDHtml5();
        }
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);

        getWidget().assignAdditionalCellStyles();

        if (stateChangeEvent.hasPropertyChanged("ddHtmlEnable")) {
            if (getState().ddHtmlEnable) {
                enableDDHtml5();
            } else {
                disableDDHtml5();
            }
        }
    }
}