package manage.model;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum DashboardConnectOption {

    CWI("connect_with_interaction"),                    // Connect With Interaction
    CWOIWE("connect_without_interaction_with_email"),     // Connect WithOut Interaction With Email
    CWOIWOE("connect_without_interaction_without_email");  // Connect WithOut Interaction WithOutEmail


    private final String type;

    DashboardConnectOption(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean connectWithoutInteraction() {
        return this.type.equals(DashboardConnectOption.CWOIWE.getType()) || this.type.equals(DashboardConnectOption.CWOIWOE.getType());
    }

    public static DashboardConnectOption fromType(String type) {
        String sanitizedType = type.replaceAll(Pattern.quote("-"), "_");
        return Stream.of(DashboardConnectOption.values())
                .filter(option -> option.getType().equals(sanitizedType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid EntityType " + type));
    }
}
