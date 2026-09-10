package com.plainphone.app;

import java.util.List;

/** A GitHub status window: open PRs, review requests, assigned issues, latest Actions run. */
final class GithubPanel extends ServicePanel {

    GithubPanel(String accountId) { super(accountId); }

    @Override public String kind() { return "github"; }
    @Override String accountKind() { return DevAccount.GITHUB; }
    @Override String serviceName() { return "GitHub"; }
    @Override long pollMs() { return 90_000; }

    @Override
    List<ServiceData.Section> load(DevAccount account, String token) throws Exception {
        return Github.sections(ctx, account, token);
    }
}
