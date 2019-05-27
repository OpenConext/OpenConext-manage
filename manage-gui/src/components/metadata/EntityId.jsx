import React from "react";
import I18n from "i18n-js";

import { search, validation } from "../../api";
import InlineEditable from "./InlineEditable";
import { isEmpty } from "../../utils/Utils";

export default class EntityId extends React.PureComponent {
  state = {
    errorMessage: ""
  };

  setErrorMessage(errorType) {
    let errorMessage;

    switch (errorType) {
      case "notPresent":
        errorMessage = I18n.t("metadata.required", {
          name: "Entity ID"
        });
        break;
      case "notUnique":
        errorMessage = I18n.t("metadata.entityIdAlreadyExists", {
          entityid: this.props.value
        });
        break;
      case "notFormatted":
        errorMessage = I18n.t("metaDataFields.error", {
          format: this.props.entityIdFormat
        });
        break;
      default:
        errorMessage = "";
    }

    this.setState({ errorMessage });
    return false;
  }

  validPresence(entityId) {
    return !isEmpty(entityId) || this.setErrorMessage("notPresent");
  }

  async validUniqueness(entityid) {
    const { originalEntityId, type } = this.props;

    if (originalEntityId === entityid) {
      return true;
    }

    const isUnique = (await search({ entityid }, type)).length === 0;

    return isUnique || this.setErrorMessage("notUnique");
  }

  async validFormat(entityId) {
    const { entityIdFormat } = this.props;

    if (!entityIdFormat) {
      return true;
    }

    const valid = await validation(entityIdFormat, entityId);
    return valid || this.setErrorMessage("notFormatted");
  }

  async validateEntityId(entityId) {
    const valid =
      this.validPresence(entityId) &&
      (await this.validFormat(entityId)) &&
      (await this.validUniqueness(entityId));

    if (!valid) this.props.onError(true);
  }

  componentDidMount() {
    this.validateEntityId(this.props.value);
  }

  render() {
    const { value, hasError, ...rest } = this.props;

    return (
      <span>
        <InlineEditable
          {...rest}
          value={value}
          onBlur={e => this.validateEntityId(e.target.value)}
        />
        {hasError && <span className="error">{this.state.errorMessage}</span>}
      </span>
    );
  }
}
