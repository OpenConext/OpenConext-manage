import React from "react";
import I18n from "i18n-js";

import { search, validation } from "../../api";
import InlineEditable from "./InlineEditable";
import { isEmpty } from "../../utils/Utils";

export default class EntityId extends React.PureComponent {
  state = {
    hasError: false,
    errorMessage: ""
  };

  notUnique() {
    this.setState({
      hasError: true,
      errorMessage: I18n.t("metadata.entityIdAlreadyExists", {
        entityid: this.props.value
      })
    });
  }

  notFormatted() {
    this.setState({
      hasError: true,
      errorMessage: I18n.t("metaDataFields.error", {
        format: this.props.entityIdFormat
      })
    });
  }

  validPresence = entityId => !isEmpty(entityId);

  validUniqueness(entityId) {
    const isNewValue = this.props.originalEntityId !== entityId;
    if (!isNewValue) return true;

    return search({ entityid: entityId }, this.props.type).then(json => {
      const isUnique = isNewValue && json.length === 0;

      return isUnique ? true : this.notUnique() & false;
    });
  }

  validFormat(entityId) {
    const { entityIdFormat } = this.props;

    if (!entityIdFormat) {
      return true;
    }

    return validation(entityIdFormat, entityId).then(valid => valid || this.notFormatted());
  }

  async validateEntityId(entityId) {
    this.setState({
      hasError: false,
      errorMessage: ""
    });

    const valid =
      this.validPresence(entityId) &&
      (await this.validFormat(entityId)) &&
      (await this.validUniqueness(entityId));

    if (!valid) this.props.onError(true);
  }

  render() {
    const { hasError, errorMessage } = this.state;
    const { entityIdFormat, value, ...rest } = this.props;

    return (
      <span>
        <InlineEditable
          {...rest}
          value={value}
          onBlur={e => this.validateEntityId(e.target.value)}
        />
        {hasError && <p className="error">{errorMessage}</p>}
      </span>
    );
  }
}
