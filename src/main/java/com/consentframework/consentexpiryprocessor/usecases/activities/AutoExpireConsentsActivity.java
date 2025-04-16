package com.consentframework.consentexpiryprocessor.usecases.activities;

import com.consentframework.consentexpiryprocessor.domain.entities.ActiveConsentWithExpiryTime;
import com.consentframework.consentexpiryprocessor.domain.repositories.ConsentRepository;
import com.consentframework.shared.api.domain.pagination.ListPage;
import com.consentframework.shared.api.infrastructure.mappers.DynamoDbConsentExpiryTimeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * This activity queries for active consents past their expiry time, and updates their status to expired.
 */
public class AutoExpireConsentsActivity {
    private static final Logger logger = LogManager.getLogger(AutoExpireConsentsActivity.class);

    private final ConsentRepository consentRepository;

    /**
     * Initialize the activity.
     *
     * @param consentRepository The consent repository.
     */
    public AutoExpireConsentsActivity(final ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    /**
     * Execute the activity.
     *
     * Makes paginated API calls to retrieve active consents with non-null expiry times,
     * in ascending order of expiry time (oldest to newest), and updates the status of
     * each consent to expired if its expiryTime is in the past.
     *
     * Assumption: the consents returned by the repository are sorted in ascending order
     * of expiry time (oldest to newest).
     */
    public void execute() {
        ListPage<ActiveConsentWithExpiryTime> currentPageConsents = consentRepository.getActiveConsentsWithExpiryTimes(Optional.empty());

        while (currentPageConsents != null && currentPageConsents.resultsOnPage() != null) {
            logger.info("Processing page of {} active consents with expiry times.", currentPageConsents.resultsOnPage().size());

            for (final ActiveConsentWithExpiryTime consent : currentPageConsents.resultsOnPage()) {
                final boolean isPastExpiryTime = isPastExpiryTime(consent);
                if (!isPastExpiryTime) {
                    logger.info("Remaining consent expiry times are in the future, ending auto-expire consent activity.");
                    return;
                }
                expireConsent(consent);
            }

            currentPageConsents = getNextPage(currentPageConsents);
        }

        logger.info("No more consents to auto-expire, ending auto-expire consent activity.");
    }

    /**
     * Update the consent to expired.
     *
     * @param consent The consent to update.
     */
    private void expireConsent(final ActiveConsentWithExpiryTime consent) {
        final String consentPartitionKey = consent.id();
        logger.info("Consent with partition key {} has expiryTimeId {} with expiryTime in the past, updating status to EXPIRED.",
            consentPartitionKey, consent.expiryTimeId());
        final String nextConsentVersion = String.valueOf(consent.consentVersion() + 1);
        consentRepository.expireConsent(consentPartitionKey, nextConsentVersion);
    }

    /**
     * Checks if a consent's expiryTime is in the past.
     *
     * @param consent The consent to check.
     * @return True if the consent's expiryTime is in the past, false otherwise.
     */
    private boolean isPastExpiryTime(final ActiveConsentWithExpiryTime consent) {
        final OffsetDateTime currentTime = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
        final OffsetDateTime expiryTime = DynamoDbConsentExpiryTimeConverter.toOffsetDateTimeFromExpiryTimeId(consent.expiryTimeId());
        return expiryTime.isBefore(currentTime);
    }

    /**
     * Retrieves the next page of consents.
     *
     * @param currentPageConsents The current page of consents.
     * @return The next page of consents, or null if there are no more pages.
     */
    private ListPage<ActiveConsentWithExpiryTime> getNextPage(final ListPage<ActiveConsentWithExpiryTime> currentPageConsents) {
        final Optional<String> nextPageToken = currentPageConsents.nextPageToken();
        if (nextPageToken.isPresent()) {
            logger.info("Retrieving next page of consents.");
            return consentRepository.getActiveConsentsWithExpiryTimes(nextPageToken);
        }
        return null;
    }
}
