package com.plainphone.app;

import java.util.List;

/** A Supabase status window: project health, database, tables, buckets, edge functions. */
final class SupabasePanel extends ServicePanel {

    SupabasePanel(String accountId) { super(accountId); }

    @Override public String kind() { return "supabase"; }
    @Override String accountKind() { return DevAccount.SUPABASE; }
    @Override String serviceName() { return "Supabase"; }
    @Override long pollMs() { return 120_000; }

    @Override
    List<ServiceData.Section> load(DevAccount account, String token) throws Exception {
        return Supabase.sections(ctx, account, token);
    }
}
