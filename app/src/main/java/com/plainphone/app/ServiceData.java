package com.plainphone.app;

import java.util.ArrayList;
import java.util.List;

/** Plain holders a {@link ServicePanel} renders: a list of titled sections of rows. */
final class ServiceData {

    private ServiceData() {}

    static final class Row {
        final String title;
        final String subtitle;
        final Boolean ok;    // null = no status marker, TRUE = ✓, FALSE = ✗
        final String url;    // tap target, or null

        Row(String title, String subtitle, Boolean ok, String url) {
            this.title = title;
            this.subtitle = subtitle;
            this.ok = ok;
            this.url = url;
        }
    }

    static final class Section {
        final String header;
        final List<Row> rows = new ArrayList<>();

        Section(String header) { this.header = header; }

        Section add(String title, String subtitle, Boolean ok, String url) {
            rows.add(new Row(title, subtitle, ok, url));
            return this;
        }

        Section add(String title, String subtitle) {
            return add(title, subtitle, null, null);
        }
    }
}
