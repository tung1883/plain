package com.plainphone.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Runs a {@link TextExtractor} on one file and gathers its passages, within limits. */
final class ExtractRunner {

    private ExtractRunner() {}

    static final class Passage {
        final String locator;
        final String body;

        Passage(String locator, String body) {
            this.locator = locator;
            this.body = body;
        }
    }

    /** What came out: passages and a title, or why there are none ({@link #status}). */
    static final class Result {
        final List<Passage> passages;
        final String title;
        final String status;       // ok | empty | scanned | locked | failed | toobig
        final String error;

        Result(List<Passage> passages, String title, String status, String error) {
            this.passages = passages;
            this.title = title;
            this.status = status;
            this.error = error;
        }
    }

    /** At most this many characters are kept from one file. */
    static final int MAX_CHARS = 2 * 1024 * 1024;

    static Result run(TextExtractor extractor, File file, int maxChars, BooleanSupplier cancelled) {
        final List<Passage> passages = new ArrayList<>();
        final String[] title = {null};
        final int[] total = {0};
        final boolean[] full = {false};
        final String[] partialNote = {null};

        TextExtractor.Sink sink = new TextExtractor.Sink() {
            @Override
            public void chunk(String locator, String text) {
                for (String piece : Chunker.split(text)) {
                    if (full[0]) return;
                    passages.add(new Passage(locator == null ? "" : locator, piece));
                    total[0] += piece.length();
                    if (total[0] >= maxChars) full[0] = true;
                }
            }

            @Override
            public void title(String t) {
                if (title[0] == null && t != null && !t.trim().isEmpty()) title[0] = t.trim();
            }

            @Override
            public void partial(String note) {
                partialNote[0] = note;
            }

            @Override
            public boolean stop() {
                return full[0] || cancelled.getAsBoolean();
            }
        };

        try {
            extractor.extract(file, sink);
        } catch (TextExtractor.Unreadable e) {
            return new Result(new ArrayList<Passage>(), null, e.status, e.getMessage());
        } catch (IOException | RuntimeException e) {
            return new Result(new ArrayList<Passage>(), null, "failed", String.valueOf(e.getMessage()));
        } catch (OutOfMemoryError e) {
            return new Result(new ArrayList<Passage>(), null, "failed", "out of memory");
        }
        if (passages.isEmpty()) return new Result(passages, title[0], "empty", "no text");
        return new Result(passages, title[0], "ok", partialNote[0]);
    }
}
