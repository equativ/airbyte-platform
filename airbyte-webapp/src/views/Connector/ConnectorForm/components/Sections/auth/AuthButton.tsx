import classnames from "classnames";
import React, { useImperativeHandle, useRef, useState } from "react";
import { FormattedMessage } from "react-intl";

import { Button } from "components/ui/Button";
import { FlexContainer } from "components/ui/Flex";
import { Text } from "components/ui/Text";

import { ConnectorIds } from "area/connector/utils";
import { ConnectorDefinitionSpecificationRead, ConnectorSpecification } from "core/domain/connector";

import styles from "./AuthButton.module.scss";
import { GoogleAuthButton } from "./GoogleAuthButton";
import QuickBooksAuthButton from "./QuickBooksAuthButton";
import { useFormOauthAdapter, useFormOauthAdapterBuilder } from "./useOauthFlowAdapter";
import { useConnectorForm } from "../../../connectorFormContext";
import { useAuthentication } from "../../../useAuthentication";

function isGoogleConnector(connectorDefinitionId: string): boolean {
  return (
    [
      ConnectorIds.Sources.GoogleAds,
      ConnectorIds.Sources.GoogleAnalyticsUniversalAnalytics,
      ConnectorIds.Sources.GoogleDirectory,
      ConnectorIds.Sources.GoogleSearchConsole,
      ConnectorIds.Sources.GoogleSheets,
      ConnectorIds.Sources.GoogleWorkspaceAdminReports,
      ConnectorIds.Sources.YouTubeAnalytics,
      ConnectorIds.Destinations.GoogleSheets,
      // TODO: revert me
      ConnectorIds.Sources.YouTubeAnalyticsBusiness,
      //
    ] as string[]
  ).includes(connectorDefinitionId);
}

function getButtonComponent(connectorDefinitionId: string) {
  if (isGoogleConnector(connectorDefinitionId)) {
    return GoogleAuthButton;
  }
  if (connectorDefinitionId === ConnectorIds.Sources.QuickBooks) {
    return QuickBooksAuthButton;
  }
  return Button;
}

function getAuthenticateMessageId(connectorDefinitionId: string): string {
  if (isGoogleConnector(connectorDefinitionId)) {
    return "connectorForm.signInWithGoogle";
  }
  return "connectorForm.authenticate";
}

export const AuthButton: React.FC<{
  selectedConnectorDefinitionSpecification: ConnectorDefinitionSpecificationRead;
}> = ({ selectedConnectorDefinitionSpecification }) => {
  const { selectedConnectorDefinition } = useConnectorForm();

  const { hiddenAuthFieldErrors } = useAuthentication();
  const authRequiredError = Object.values(hiddenAuthFieldErrors).includes("required");

  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const { loading, done, run } = useFormOauthAdapter(
    selectedConnectorDefinitionSpecification,
    selectedConnectorDefinition
  );

  if (!selectedConnectorDefinition) {
    console.error("Entered non-auth flow while no supported connector is selected");
    return null;
  }

  const definitionId = ConnectorSpecification.id(selectedConnectorDefinitionSpecification);
  const Component = getButtonComponent(definitionId);

  const messageStyle = classnames(styles.message, {
    [styles.error]: authRequiredError,
    [styles.success]: !authRequiredError,
  });
  const buttonLabel = done ? (
    <FormattedMessage id="connectorForm.reauthenticate" />
  ) : (
    <FormattedMessage
      id={getAuthenticateMessageId(definitionId)}
      values={{ connector: selectedConnectorDefinition.name }}
    />
  );
  return (
    <FlexContainer alignItems="center">
      <Component isLoading={loading} type="button" data-testid="oauth-button" onClick={run}>
        {buttonLabel}
      </Component>
      {authRequiredError && (
        <Text as="div" size="lg" className={messageStyle}>
          <FormattedMessage id="connectorForm.authenticate.required" />
        </Text>
      )}
    </FlexContainer>
  );
};

export const AuthButtonBuilder = React.forwardRef<
  HTMLDivElement | null,
  {
    builderProjectId: string;
    onComplete: (authPayload: Record<string, unknown>) => void;
    onClick?: () => void;
    disabled?: boolean;
  }
>(({ builderProjectId, onComplete, onClick, disabled }, ref) => {
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  const { loading, run } = useFormOauthAdapterBuilder(builderProjectId, onComplete);
  const [isAccented, setIsAccented] = useState(false);

  const flexRef = useRef<HTMLDivElement>(null);

  useImperativeHandle(
    ref,
    () =>
      new Proxy(flexRef.current!, {
        get(target, prop, receiver) {
          if (prop === "scrollIntoView") {
            const fn: HTMLElement["scrollIntoView"] = (...args) => {
              target.scrollIntoView(...args);
              setIsAccented(true);
            };
            return fn;
          }
          return Reflect.get(target, prop, receiver);
        },
      })
  );

  return (
    <FlexContainer alignItems="center" ref={flexRef}>
      <div className={isAccented ? styles.accented__container : undefined}>
        <Button
          disabled={disabled}
          isLoading={loading}
          type="button"
          data-testid="oauth-button"
          onClick={() => {
            setIsAccented(false);
            (onClick ?? run)();
          }}
          className={isAccented ? styles.accented__button : undefined}
        >
          <FormattedMessage id="connectorBuilder.authentication.oauthButton.label" />
        </Button>
      </div>
    </FlexContainer>
  );
});
AuthButtonBuilder.displayName = "AuthButtonBuilder";
