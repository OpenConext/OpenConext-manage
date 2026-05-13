package manage.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class ReleaseDaysInfoContributor implements InfoContributor {

    private static final String RELEASE_DAYS_KEY = "days_since_release";

    private final BuildProperties buildProperties;

    public ReleaseDaysInfoContributor(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        long currentDays = 0L;

        if (null != buildProperties.getTime()) {
            LocalDate releaseDate = LocalDate.ofInstant(buildProperties.getTime(), ZoneId.systemDefault());
            LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
            currentDays = ChronoUnit.DAYS.between(releaseDate, currentDate);
        }
        builder.withDetail(RELEASE_DAYS_KEY, currentDays);
    }

}
