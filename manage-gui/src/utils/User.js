export function isSystemUser(currentUser) {
    return currentUser.authorities.some(authority => authority.authority === "ROLE_SYSTEM");
}
