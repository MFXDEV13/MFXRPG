package id.mfx.rpg.util;

public final class ProgressBar {

    private ProgressBar() {
    }

    public static String render(long value, long maximum, int width) {
        if (maximum <= 0L) {
            return "<green>" + "█".repeat(width) + "</green> <white>100%</white>";
        }

        double ratio = Math.max(0.0D, Math.min(1.0D, (double) value / maximum));
        int filled = (int) Math.round(ratio * width);
        String color = ratio < 0.35D ? "red" : ratio < 0.75D ? "yellow" : "green";
        int percent = (int) Math.round(ratio * 100.0D);
        return "<" + color + ">" + "█".repeat(filled) + "</" + color + "><dark_gray>" + "░".repeat(width - filled) + "</dark_gray> <white>" + percent + "%</white>";
    }
}