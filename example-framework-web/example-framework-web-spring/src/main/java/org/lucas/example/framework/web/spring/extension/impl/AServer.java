package org.lucas.example.framework.web.spring.extension.impl;

import org.lucas.example.framework.web.spring.extension.IServer;
import org.springframework.stereotype.Component;

@Component
public class AServer implements IServer {

    public static final Class<AServer> UUID = AServer.class;

    @Override
    public Class<AServer> getUUID() {
        return UUID;
    }

    public String callTradeServiceWork(String url) {
        System.out.println("BForceCloseServer.callTradeServiceWork");
        return null;
    }
}
