package misc;

@SuppressWarnings("ConstantConditions")
public class Colorer {
    private static final boolean ON = true;

    private Colorer() {
    }

    public static String paint(String message, Format format) {
        if (ON)
            return String.format("\u001B[%dm%s\u001B[m", format.color, message);
        else
            return message;
    }

    /**
     * Example:
     * System.out.println(Colorer.format("%1`Lol%` %4`makes%` %5`looool%`"));
     */
    public static String format(String format, Object... obj) {
        format = format
                .replaceAll("%([1-7])`", ON ? "\u001B[3$1m" : "")
                .replaceAll("%`", ON ? "\u001B[m" : "");
        return String.format(format, obj);
    }

    public enum Format {
        BLACK(30),
        RED(31),
        GREEN(32),
        YELLOW(33),
        BLUE(34),
        MAGENTA(35),
        CYAN(36),
        PLAIN(37);

        private final int color;

        Format(int color) {
            this.color = color;
        }
    }

}
