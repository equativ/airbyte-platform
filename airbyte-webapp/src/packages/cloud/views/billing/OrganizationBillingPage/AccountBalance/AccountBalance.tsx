import dayjs from "dayjs";
import { useMemo } from "react";
import { FormattedDate, FormattedMessage, FormattedNumber, useIntl } from "react-intl";

import { Box } from "components/ui/Box";
import { DataLoadingError } from "components/ui/DataLoadingError";
import { FlexContainer, FlexItem } from "components/ui/Flex";
import { Heading } from "components/ui/Heading";
import { Icon } from "components/ui/Icon";
import { LoadingSkeleton } from "components/ui/LoadingSkeleton";
import { Text } from "components/ui/Text";
import { Tooltip } from "components/ui/Tooltip";

import { useCurrentWorkspace, useGetOrganizationBillingBalance } from "core/api";
import { CreditBlockRead } from "core/api/types/AirbyteClient";
import { useFormatCredits } from "core/utils/numberHelper";

export const AccountBalance = () => {
  const { organizationId } = useCurrentWorkspace();
  const {
    data: balance,
    isLoading: balanceIsLoading,
    isError: balanceError,
  } = useGetOrganizationBillingBalance(organizationId);
  const { formatCredits } = useFormatCredits();

  const hasPositiveCreditBalance = !!balance?.credits?.balance && balance.credits.balance > 0;
  const showCreditBalance = hasPositiveCreditBalance || balance?.planType === "prepaid";

  return (
    <>
      <Heading as="h2" size="sm">
        <FormattedMessage id="settings.organization.billing.accountBalance" />
      </Heading>
      <Box pt="xl">
        {balanceIsLoading && (
          <FlexContainer direction="column" gap="sm">
            <LoadingSkeleton />
            <LoadingSkeleton />
          </FlexContainer>
        )}
        {balance && (
          <FlexContainer justifyContent="space-between" gap="lg" direction="column">
            {showCreditBalance && (
              <FlexItem>
                <Tooltip
                  disabled={balance?.credits?.blocks?.length === 0}
                  control={
                    <FlexContainer alignItems="center" gap="xs">
                      <Text>
                        <FormattedMessage id="settings.organization.billing.remainingCredits" />
                      </Text>
                      <Icon type="infoOutline" size="xs" />
                    </FlexContainer>
                  }
                >
                  {balance?.credits?.blocks?.length && balance.credits.blocks.length > 0 && (
                    <ListOfCreditExpiryDates creditBlocks={balance.credits?.blocks} />
                  )}
                </Tooltip>
                <Text size="lg">
                  <FormattedMessage
                    id="settings.organization.billing.remainingCreditsAmount"
                    values={{ amount: formatCredits(balance.credits?.balance ?? 0) }}
                  />
                </Text>
              </FlexItem>
            )}
            {balance.planType === "in_arrears" && (
              <>
                <FlexItem>
                  <Text size="sm">
                    <FormattedMessage id="settings.organization.billing.upcomingInvoiceAmount" />
                  </Text>
                  <Text size="lg">
                    <FormattedNumber
                      value={
                        isNaN(parseFloat(balance.upcomingInvoice.amount))
                          ? 0
                          : parseFloat(balance.upcomingInvoice.amount)
                      }
                      currency={balance.upcomingInvoice.currency}
                      style="currency"
                      minimumFractionDigits={2}
                      maximumFractionDigits={2}
                    />
                  </Text>
                </FlexItem>
                <FlexItem>
                  <Text size="sm">
                    <FormattedMessage id="settings.organization.billing.invoiceDate" />
                  </Text>
                  <Text size="lg">
                    <FormattedDate value={balance.upcomingInvoice.dueDate} dateStyle="medium" />
                  </Text>
                </FlexItem>
              </>
            )}
          </FlexContainer>
        )}
        {balanceError && (
          <DataLoadingError>
            <FormattedMessage id="settings.organization.billing.accountBalanceError" />
          </DataLoadingError>
        )}
      </Box>
    </>
  );
};

const ListOfCreditExpiryDates = ({ creditBlocks }: { creditBlocks: CreditBlockRead[] }) => {
  const { formatDate } = useIntl();
  const { formatCredits } = useFormatCredits();

  const sortedCreditBlocks = useMemo(
    () => creditBlocks.sort((a, b) => (dayjs(a.expiryDate).isBefore(dayjs(b.expiryDate)) ? -1 : 1)),
    [creditBlocks]
  );

  return (
    <>
      {sortedCreditBlocks.map((creditBlock) => (
        <>
          <FormattedMessage
            id="settings.organization.billing.creditBlockExpiry"
            values={{
              amount: formatCredits(creditBlock.amount),
              expiryDate: formatDate(creditBlock.expiryDate, { dateStyle: "medium" }),
            }}
          />
          <br />
        </>
      ))}
    </>
  );
};