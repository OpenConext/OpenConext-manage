const dryRun = true;   // set to false to actually write

db.saml20_idp.find({ "data.allowedEntities.1": { $exists: true } }).forEach(idp => {
    const seen = new Set();
    const deduped = idp.data.allowedEntities.filter(e => {
        if (seen.has(e.name)) return false;
        seen.add(e.name);
        return true;
    });
    const removed = idp.data.allowedEntities.length - deduped.length;
    if (removed === 0) return;

    print(`${idp.data.entityid}: removing ${removed} duplicate(s)`);
    if (!dryRun) {
        db.saml20_idp.updateOne(
            { _id: idp._id },
            { $set: { "data.allowedEntities": deduped } }
        );
    }
});
