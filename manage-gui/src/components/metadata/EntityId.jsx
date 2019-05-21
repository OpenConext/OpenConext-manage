import React from "react";
import I18n from "i18n-js";

import {search, validation} from "../../api";
import InlineEditable from "./InlineEditable";
import {isEmpty} from "../../utils/Utils";

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

  async validUniqueness(entityId) {
    const isNewValue = this.props.originalEntityId !== entityId;
    if (!isNewValue) return true;

    const json = await search({entityid: entityId}, this.props.type);
    const isUnique = isNewValue && json.length === 0;

    return isUnique || this.notUnique();
  }

  async validFormat(entityId) {
    const {entityIdFormat} = this.props;

    if (!entityIdFormat) {
      return true;
    }
    const valid = await validation(entityIdFormat, entityId);
    return valid || this.notFormatted();
  }

  validateEntityId(entityId) {
    this.setState({
      hasError: false,
      errorMessage: ""
    });

    const valid =
      this.validPresence(entityId) &&
      (this.validFormat(entityId)) &&
      (this.validUniqueness(entityId));

    if (!valid) this.props.onError(true);
  }

  render() {
    const {hasError, errorMessage} = this.state;
    const {entityIdFormat, value, ...rest} = this.props;

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
