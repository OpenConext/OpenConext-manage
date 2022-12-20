const QueryParameter = {

    //shameless refactor of https://gist.githubusercontent.com/pduey/2764606/raw/e8b9d6099f1e4161f7dd9f81d71c2c7a1fecbd5b/querystring.js

    searchToHash: function (windowLocationSearch) {
        const h = {};
        if (windowLocationSearch === undefined || windowLocationSearch.length < 1) {
            return h;
        }
        const q = windowLocationSearch.slice(1).split("&");
        for (let i = 0; i < q.length; i++) {
            const keyVal = q[i].split("=");
            // replace '+' (alt space) char explicitly since decode does not
            const hkey = decodeURIComponent(keyVal[0]).replace(/\+/g, " ");
            const hval = decodeURIComponent(keyVal[1]).replace(/\+/g, " ");
            if (h[hkey] === undefined) {
                h[hkey] = [];
            }
            h[hkey].push(hval);
        }
        return h;
    },


    hashToSearch: function (newSearchHash) {
        let search = "?";
        for (const key in newSearchHash) {
            if (newSearchHash.hasOwnProperty(key)) {
                for (let i = 0; i < newSearchHash[key].length; i++) {
                    search += search === "?" ? "" : "&";
                    search += encodeURIComponent(key) + "=" + encodeURIComponent(newSearchHash[key][i]);
                }
            }
        }
        return search;
    }

};

export function replaceQueryParameter(windowLocationSearch, name, value) {
    const newSearchHash = QueryParameter.searchToHash(windowLocationSearch);
    delete newSearchHash[name];
    newSearchHash[decodeURIComponent(name)] = [decodeURIComponent(value)];
    return QueryParameter.hashToSearch(newSearchHash);
}

export function getParameterByName(name, windowLocationSearch) {
    const replacedName = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    const regex = new RegExp("[\\?&]" + replacedName + "=([^&#]*)"),
        results = regex.exec(windowLocationSearch);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}