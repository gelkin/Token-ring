package token.ring;

import sender.UniqueValue;

import java.io.Serializable;

public class Priority implements Comparable<Priority>, Serializable {
    private final int progress;
    private final UniqueValue unique;

    public Priority(int progress, UniqueValue unique) {
        this.progress = progress;
        this.unique = unique;
    }

    @Override
    public int compareTo(Priority o) {
        if (progress != o.progress)
            return Integer.compare(progress, o.progress);

        return unique.compareTo(o.unique);
    }

    @Override
    public String toString() {
        return String.format("Priority {%d %s}", progress, unique);
    }
}
