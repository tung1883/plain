package com.plainphone.app;

import java.util.List;

/** A Vercel status window: recent deployments with state + age. */
final class VercelPanel extends ServicePanel {

    VercelPanel(String accountId) { super(accountId); }

    @Override public String kind() { return "vercel"; }
    @Override String accountKind() { return DevAccount.VERCEL; }
    @Override String serviceName() { return "Vercel"; }
    @Override long pollMs() { return 60_000; }

    @Override
    List<ServiceData.Section> load(DevAccount account, String token) throws Exception {
        return Vercel.sections(ctx, account, token);
    }
}
