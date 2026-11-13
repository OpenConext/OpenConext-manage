// Removes all contact information ("contacts:N:surName|givenName|emailAddress|telephoneNumber|
// isSirtfiSecurityContact|contactType") from data.metaDataFields in every collection,
// including the *_revision collections. Copy/paste into a mongo shell (mongosh, MongoDB 4.2+).
// First select the metadata database, e.g.: use metadata
// Set dryRun to true to only count the affected documents.
(function () {
    const dryRun = false;
    const prefix = "contacts:";
    // Matches the schema patterns ^contacts:([0-3]{1}):<field>$
    const contactKey = {$eq: [{$indexOfCP: ["$$f.k", prefix]}, 0]};
    const hasContacts = {
        $expr: {
            $gt: [{
                $size: {
                    $filter: {
                        input: {$objectToArray: {$ifNull: ["$data.metaDataFields", {}]}},
                        as: "f",
                        cond: contactKey
                    }
                }
            }, 0]
        }
    };
    const pipeline = [{
        $set: {
            "data.metaDataFields": {
                $arrayToObject: {
                    $filter: {
                        input: {$objectToArray: "$data.metaDataFields"},
                        as: "f",
                        cond: {$ne: [{$indexOfCP: ["$$f.k", prefix]}, 0]}
                    }
                }
            }
        }
    }];
    db.getCollectionNames().forEach(function (name) {
        if (name.startsWith("system.")) {
            return;
        }
        const coll = db.getCollection(name);
        const count = coll.countDocuments({"data.metaDataFields": {$type: "object"}, ...hasContacts});
        if (count === 0) {
            return;
        }
        if (dryRun) {
            print(name + ": " + count + " documents with contacts (dry run)");
        } else {
            const res = coll.updateMany({"data.metaDataFields": {$type: "object"}, ...hasContacts}, pipeline);
            print(name + ": " + res.modifiedCount + " of " + count + " documents updated");
        }
    });
})();
