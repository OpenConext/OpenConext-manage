import {I18n as I18nRemote} from "i18n-js";

import en from "./en";
import nl from "./nl";

const I18n = new I18nRemote({
    en: en,
    nl: nl,
}, {locale: "en"});

I18n.missingTranslation.register("report-error", (i18n, scope) => {
    return `Missing translation <${scope}>`;
});
I18n.missingBehavior = "report-error";

export default I18n;
