package com.plainphone.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class TimeBlockRules {

    private TimeBlockRules() {}

    static boolean isActiveNow(TimeBlock block, Calendar now) {
        if (!block.enabled) return false;
        int minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);

        if (block.endMinute > block.startMinute) {
            return block.runsOnDay(dayOfWeek) && minuteOfDay >= block.startMinute && minuteOfDay < block.endMinute;
        } else {

            if (minuteOfDay >= block.startMinute) {
                return block.runsOnDay(dayOfWeek);
            } else if (minuteOfDay < block.endMinute) {
                int yesterday = dayOfWeek == Calendar.SUNDAY ? Calendar.SATURDAY : dayOfWeek - 1;
                return block.runsOnDay(yesterday);
            }
            return false;
        }
    }

    static long activeWindowEndMillis(TimeBlock block, Calendar now) {
        Calendar end = (Calendar) now.clone();
        int minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        boolean wraps = block.endMinute <= block.startMinute;
        boolean inPostMidnightTail = wraps && minuteOfDay < block.endMinute;
        if (wraps && !inPostMidnightTail) {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }
        end.set(Calendar.HOUR_OF_DAY, (block.endMinute / 60) % 24);
        end.set(Calendar.MINUTE, block.endMinute % 60);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        return end.getTimeInMillis();
    }

    static long blockEndMillis(Context context, TimeBlock block) {
        long adhocUntil = Config.getAdhocUntil(context);
        if (block.id.equals(Config.getAdhocBlockId(context)) && adhocUntil > System.currentTimeMillis()) {
            return adhocUntil;
        }
        return activeWindowEndMillis(block, Calendar.getInstance());
    }

    static String formatEndTime(Context context, TimeBlock block) {
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(blockEndMillis(context, block));
        return TimeBlock.formatTime(end.get(Calendar.HOUR_OF_DAY) * 60 + end.get(Calendar.MINUTE));
    }

    static List<TimeBlock> getActiveBlocks(Context context) {
        List<TimeBlock> all = Config.getTimeBlocks(context);
        List<TimeBlock> active = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        long nowMillis = System.currentTimeMillis();

        for (TimeBlock block : all) {
            if (isActiveNow(block, now) && Config.getOverrideUntil(context, block.id) < nowMillis) {
                active.add(block);
            }
        }

        String adhocId = Config.getAdhocBlockId(context);
        if (adhocId != null && Config.getAdhocUntil(context) > nowMillis
                && TimeBlock.findById(active, adhocId) == null) {
            TimeBlock adhocBlock = TimeBlock.findById(all, adhocId);
            if (adhocBlock != null) {
                active.add(adhocBlock);
            }
        }

        return active;
    }

    static TimeBlock getBlockingBlock(Context context, String packageName) {
        for (TimeBlock block : getActiveBlocks(context)) {

            if (block.mode == TimeBlock.Mode.ALLOW_ONLY && block.packages.isEmpty()) continue;

            boolean blocked = block.mode == TimeBlock.Mode.BLACKOUT
                    ? block.packages.contains(packageName)
                    : !block.packages.contains(packageName);
            if (blocked) return block;
        }
        return null;
    }
}

