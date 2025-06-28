package dev.alsalman.ragservice;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;

@Push(transport = Transport.WEBSOCKET_XHR)
public class AppShell implements AppShellConfigurator {
}