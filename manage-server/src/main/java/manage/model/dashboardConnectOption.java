package manage.model;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum dashboardConnectOption {

    CWI("connect_with_interaction"),                    // Connect With Interaction
    CWOIWE("connect_without_interaction_with_email"),     // Connect WithOut Interaction With Email
    CWOIWOE("connect_without_interaction_without_email");  // Connect WithOut Interaction WithOutEmail


    private final String type;

    dashboardConnectOption(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean connectWithoutInteraction() {
        return this.type.equals(dashboardConnectOption.CWOIWE.getType()) || this.type.equals(dashboardConnectOption.CWOIWOE.getType());
    }

    public static dashboardConnectOption fromType(String type) {
        String sanitizedType = type.replaceAll(Pattern.quote("-"), "_");
        return Stream.of(dashboardConnectOption.values())
                .filter(entityType -> entityType.getType().equals(sanitizedType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid EntityType " + type));
    }
}
