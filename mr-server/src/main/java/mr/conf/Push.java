package mr.conf;

import lombok.AllArgsConstructor;

public class Push {

    public final String url;
    public final String name;

    public Push(String url, String name) {
        this.url = url.replaceFirst("://(.*)@", "://");
        this.name = name;
    }
}
