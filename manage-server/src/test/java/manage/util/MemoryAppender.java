package manage.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class MemoryAppender extends ListAppender<ILoggingEvent> {

    public boolean contains(String string, Level level) {
        return this.list.stream()
                .anyMatch(event -> event.getFormattedMessage().toLowerCase().contains(string.toLowerCase())
                        && event.getLevel().equals(level));
    }
}