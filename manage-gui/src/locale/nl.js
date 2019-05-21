// Interpolation works as follows:
//
// Make a key with the translation and enclose the variable with {{}}
// ie "Hello {{name}}" Do not add any spaces around the variable name.
// Provide the values as: I18n.t("key", {name: "John Doe"})
import I18n from "i18n-js";

I18n.translations.nl = {
  code: "EN",
  name: "English",
  select_locale: "Select English",

  header: {
    title: "Manage",
    links: {
      help_html: "<a href=\"https://wiki.surfnet.nl/pages/viewpage.action?pageId=35422637\" target=\"_blank\">Help</a>",
      logout: "Logout",
      exit: "Exit"
    },
    role: "Role"
  },

  navigation: {
    search: "Search"
  },

  error_dialog: {
    title: "Unexpected error",
    body: "This is embarrassing; an unexpected error has occurred. It has been logged and reported. Please try again. Still doesn't work? Please click 'Help'.",
    ok: "Close"
  },

  confirmation_dialog: {
    title: "Please confirm",
    confirm: "Confirm",
    cancel: "Cancel",
    leavePage: "Do you really want to leave this page?",
    leavePageSub: "Changes that you made will not be saved.",
    stay: "Stay",
    leave: "Leave"
  },


};

export default I18n.translations.nl;
