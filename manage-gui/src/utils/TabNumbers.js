export const getConnectedEntities = (whiteListing, allowedAll , allowedEntities= [], entityId, state) => {
  return whiteListing
    .filter(idp => idp.data.allowedall || idp.data.allowedEntities.some(entity => entity.name === entityId))
    .filter(idp => idp.data.state === state)
    .filter(idp => allowedAll || allowedEntities.some(entity => entity.name === idp.data.entityid));
}